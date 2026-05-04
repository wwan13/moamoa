import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import ts from "typescript";
import { fileURLToPath } from "node:url";

const DEFAULT_OUTPUT_DIR = path.resolve(process.cwd(), "build/symbolify/core-web");
const INDEX_VERSION = 1;

export function normalizeName(name) {
  return name.toLowerCase();
}

export function parseArgs(argv) {
  const [command, ...rest] = argv;
  const options = {};

  for (let index = 0; index < rest.length; index += 2) {
    const key = rest[index];
    const value = rest[index + 1];

    if (!key?.startsWith("--")) {
      throw new Error(`Unexpected argument: ${key ?? ""}`);
    }
    if (value == null) {
      throw new Error(`Missing value for ${key}`);
    }

    options[key] = value;
  }

  return { command, options };
}

export function indexProject({
  rootDir = path.resolve(process.cwd(), "src"),
  outputDir = DEFAULT_OUTPUT_DIR,
  moduleHint = "core-web",
  sourceSet = "main",
} = {}) {
  const files = collectSourceFiles(rootDir);
  const symbols = files.flatMap((filePath) => extractSymbolsFromFile(filePath, moduleHint, sourceSet));
  const sortedSymbols = [...symbols].sort(compareSymbols);

  fs.mkdirSync(path.join(outputDir, "by-name"), { recursive: true });
  fs.mkdirSync(path.join(outputDir, "by-file"), { recursive: true });

  const manifest = {
    version: INDEX_VERSION,
    generatedAt: new Date().toISOString(),
    repoRoot: rootDir,
    language: "react-ts",
    moduleHint,
    sourceSet,
    symbolCount: sortedSymbols.length,
    fileCount: new Set(sortedSymbols.map((symbol) => symbol.filePath)).size,
  };

  fs.writeFileSync(path.join(outputDir, "manifest.json"), `${JSON.stringify(manifest, null, 2)}\n`);
  fs.writeFileSync(
    path.join(outputDir, "symbols.jsonl"),
    `${sortedSymbols.map((symbol) => JSON.stringify(symbol)).join("\n")}${sortedSymbols.length ? "\n" : ""}`,
  );

  for (const [normalizedName, matchingSymbols] of Object.entries(groupBy(sortedSymbols, (symbol) => symbol.normalizedName))) {
    fs.writeFileSync(path.join(outputDir, "by-name", `${normalizedName}.json`), `${JSON.stringify(matchingSymbols, null, 2)}\n`);
  }

  for (const [encodedPath, matchingSymbols] of Object.entries(groupBy(sortedSymbols, (symbol) => encodeFileKey(symbol.filePath)))) {
    fs.writeFileSync(path.join(outputDir, "by-file", `${encodedPath}.json`), `${JSON.stringify(matchingSymbols, null, 2)}\n`);
  }

  return sortedSymbols;
}

export function findSymbols({ outputDir = DEFAULT_OUTPUT_DIR, name }) {
  if (!name) {
    throw new Error("Provide --name");
  }

  const byNameFile = path.join(outputDir, "by-name", `${normalizeName(name)}.json`);
  if (!fs.existsSync(byNameFile)) {
    return [];
  }

  return JSON.parse(fs.readFileSync(byNameFile, "utf8"));
}

export function showSymbol({ outputDir = DEFAULT_OUTPUT_DIR, id }) {
  if (!id) {
    throw new Error("Provide --id");
  }

  const symbolsPath = path.join(outputDir, "symbols.jsonl");
  if (!fs.existsSync(symbolsPath)) {
    throw new Error(`Index file does not exist: ${symbolsPath}`);
  }

  const symbol = fs
    .readFileSync(symbolsPath, "utf8")
    .split("\n")
    .filter(Boolean)
    .map((line) => JSON.parse(line))
    .find((entry) => entry.id === id);

  if (!symbol) {
    throw new Error(`No symbol found for id=${id}`);
  }

  return {
    symbol,
    snippet: readSnippet(symbol.filePath, symbol.lineStart, symbol.lineEnd),
  };
}

export function extractSymbolsFromFile(filePath, moduleHint, sourceSet) {
  const sourceText = fs.readFileSync(filePath, "utf8");
  const sourceFile = ts.createSourceFile(
    filePath,
    sourceText,
    ts.ScriptTarget.Latest,
    true,
    filePath.endsWith(".tsx") ? ts.ScriptKind.TSX : ts.ScriptKind.TS,
  );
  const symbols = [];
  const containerStack = [];

  visit(sourceFile);
  return symbols.sort(compareSymbols);

  function visit(node) {
    const createdSymbols = createSymbol(node, sourceFile, filePath, moduleHint, sourceSet, containerStack.at(-1));
    const symbolList = Array.isArray(createdSymbols) ? createdSymbols : createdSymbols ? [createdSymbols] : [];
    if (symbolList.length) {
      symbols.push(...symbolList);
      for (const symbol of symbolList.filter((entry) => isContainerKind(entry.kind))) {
        containerStack.push({ name: symbol.name, kind: symbol.kind, end: node.end });
      }
    }

    ts.forEachChild(node, visit);

    if (symbolList.length) {
      for (const symbol of symbolList.filter((entry) => isContainerKind(entry.kind))) {
        containerStack.pop();
      }
    }
  }
}

function createSymbol(node, sourceFile, filePath, moduleHint, sourceSet, container) {
  if (ts.isFunctionDeclaration(node) && node.name) {
    return buildSymbolRecord({
      node,
      name: node.name.text,
      kind: classifyFunctionLike(node.name.text, node, sourceFile),
      sourceFile,
      filePath,
      moduleHint,
      sourceSet,
      container,
      signature: buildFunctionSignature(node, sourceFile),
      exported: hasExportModifier(node),
      visibility: null,
      tags: buildPathTags(filePath),
    });
  }

  if (ts.isVariableStatement(node)) {
    const exported = hasExportModifier(node);
    return node.declarationList.declarations
      .map((declaration) => {
        if (!ts.isIdentifier(declaration.name)) {
          return null;
        }
        const name = declaration.name.text;
        const initializer = declaration.initializer;
        if (!initializer) {
          return null;
        }

        const kind = classifyVariableLike(name, initializer, filePath);
        if (!kind) {
          return null;
        }

        return buildSymbolRecord({
          node: declaration,
          name,
          kind,
          sourceFile,
          filePath,
          moduleHint,
          sourceSet,
          container,
          signature: declaration.getText(sourceFile).replace(/\s+/g, " "),
          exported,
          visibility: null,
          tags: buildVariableTags(kind, initializer, filePath),
        });
      })
      .filter(Boolean);
  }

  if (ts.isClassDeclaration(node) && node.name) {
    return buildSymbolRecord({
      node,
      name: node.name.text,
      kind: "class",
      sourceFile,
      filePath,
      moduleHint,
      sourceSet,
      container,
      signature: buildHeritageSignature(node, sourceFile),
      exported: hasExportModifier(node),
      visibility: null,
      tags: buildPathTags(filePath),
    });
  }

  if (ts.isInterfaceDeclaration(node)) {
    return buildSymbolRecord({
      node,
      name: node.name.text,
      kind: "interface",
      sourceFile,
      filePath,
      moduleHint,
      sourceSet,
      container,
      signature: buildHeritageSignature(node, sourceFile),
      exported: hasExportModifier(node),
      visibility: null,
      tags: buildPathTags(filePath),
    });
  }

  if (ts.isTypeAliasDeclaration(node)) {
    return buildSymbolRecord({
      node,
      name: node.name.text,
      kind: "type",
      sourceFile,
      filePath,
      moduleHint,
      sourceSet,
      container,
      signature: node.getText(sourceFile).replace(/\s+/g, " "),
      exported: hasExportModifier(node),
      visibility: null,
      tags: buildPathTags(filePath),
    });
  }

  if (ts.isEnumDeclaration(node)) {
    return buildSymbolRecord({
      node,
      name: node.name.text,
      kind: "enum",
      sourceFile,
      filePath,
      moduleHint,
      sourceSet,
      container,
      signature: node.getText(sourceFile).replace(/\s+/g, " "),
      exported: hasExportModifier(node),
      visibility: null,
      tags: buildPathTags(filePath),
    });
  }

  return null;
}

function buildSymbolRecord({
  node,
  name,
  kind,
  sourceFile,
  filePath,
  moduleHint,
  sourceSet,
  container,
  signature,
  exported,
  visibility,
  tags,
}) {
  const start = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile));
  const end = sourceFile.getLineAndCharacterOfPosition(node.end);
  const absoluteFilePath = path.resolve(filePath);

  return {
    id: stableId(`${absoluteFilePath}|${kind}|${name}|${start.line + 1}|${start.character + 1}`),
    name,
    normalizedName: normalizeName(name),
    kind,
    language: "react-ts",
    filePath: absoluteFilePath,
    lineStart: start.line + 1,
    lineEnd: end.line + 1,
    columnStart: start.character + 1,
    columnEnd: end.character + 1,
    containerName: container?.name ?? null,
    containerKind: container?.kind ?? null,
    signature,
    exported,
    visibility,
    moduleHint,
    sourceSet,
    tags,
  };
}

function buildFunctionSignature(node, sourceFile) {
  const returnType = node.type ? `: ${node.type.getText(sourceFile)}` : "";
  const params = node.parameters.map((param) => param.getText(sourceFile)).join(", ");
  return `(${params})${returnType}`;
}

function buildHeritageSignature(node, sourceFile) {
  const heritage = node.heritageClauses?.map((clause) => clause.getText(sourceFile)).join(" ") ?? "";
  return `${node.name?.text ?? ""}${heritage ? ` ${heritage}` : ""}`.trim();
}

function classifyFunctionLike(name, node, sourceFile) {
  if (name.startsWith("use")) {
    return "hook";
  }
  if (looksLikeComponentName(name) && returnsJsx(node.body, sourceFile)) {
    return "component";
  }
  return "function";
}

function classifyVariableLike(name, initializer, filePath) {
  if (isCreateContextCall(initializer)) {
    return "context";
  }
  if (name.startsWith("use") && isFunctionLike(initializer)) {
    return "hook";
  }
  if (looksLikeComponentName(name) && (isFunctionLike(initializer) || isWrappedComponent(initializer))) {
    return "component";
  }
  if (ts.isArrowFunction(initializer) || ts.isFunctionExpression(initializer)) {
    return "function";
  }
  if (/\.(tsx|jsx)$/.test(filePath) && looksLikeComponentName(name)) {
    return "const";
  }
  return null;
}

function hasExportModifier(node) {
  return Boolean(node.modifiers?.some((modifier) => modifier.kind === ts.SyntaxKind.ExportKeyword));
}

function isContainerKind(kind) {
  return ["component", "function", "hook", "class", "interface", "type", "enum"].includes(kind);
}

function buildVariableTags(kind, initializer, filePath) {
  const tags = buildPathTags(filePath);
  if (kind === "component" && isWrappedComponent(initializer)) {
    tags.push("wrapped-component");
  }
  if (kind === "context") {
    tags.push("context");
  }
  return tags;
}

function buildPathTags(filePath) {
  const normalized = filePath.replaceAll("\\", "/").toLowerCase();
  const tags = [];
  if (normalized.includes("/pages/")) {
    tags.push("page");
  }
  if (normalized.includes("/components/")) {
    tags.push("component-file");
  }
  if (normalized.includes("/hooks/") || normalized.includes("/auth/")) {
    tags.push("hook-file");
  }
  if (normalized.includes("/routes/")) {
    tags.push("route-file");
  }
  return tags;
}

function returnsJsx(body, sourceFile) {
  if (!body) {
    return false;
  }
  let foundJsx = false;
  function visit(node) {
    if (
      ts.isJsxElement(node) ||
      ts.isJsxSelfClosingElement(node) ||
      ts.isJsxFragment(node)
    ) {
      foundJsx = true;
      return;
    }
    ts.forEachChild(node, visit);
  }
  visit(body);
  return foundJsx || /<[A-Z][A-Za-z0-9]*/.test(body.getText(sourceFile));
}

function isFunctionLike(node) {
  return ts.isArrowFunction(node) || ts.isFunctionExpression(node);
}

function isCreateContextCall(node) {
  return (
    ts.isCallExpression(node) &&
    ts.isIdentifier(node.expression) &&
    node.expression.text === "createContext"
  );
}

function isWrappedComponent(node) {
  return (
    ts.isCallExpression(node) &&
    ts.isIdentifier(node.expression) &&
    ["memo", "forwardRef"].includes(node.expression.text)
  );
}

function looksLikeComponentName(name) {
  return /^[A-Z]/.test(name);
}

function collectSourceFiles(rootDir) {
  const files = [];
  walk(rootDir);
  return files.sort();

  function walk(currentPath) {
    if (!fs.existsSync(currentPath)) {
      return;
    }

    const stat = fs.statSync(currentPath);
    if (stat.isDirectory()) {
      for (const entry of fs.readdirSync(currentPath)) {
        if (["node_modules", "build", "dist"].includes(entry)) {
          continue;
        }
        walk(path.join(currentPath, entry));
      }
      return;
    }

    if (/\.(ts|tsx)$/.test(currentPath) && !currentPath.endsWith(".d.ts")) {
      files.push(currentPath);
    }
  }
}

function compareSymbols(left, right) {
  return (
    left.filePath.localeCompare(right.filePath) ||
    left.lineStart - right.lineStart ||
    left.name.localeCompare(right.name) ||
    left.kind.localeCompare(right.kind)
  );
}

function groupBy(items, keyFn) {
  return items.reduce((accumulator, item) => {
    const key = keyFn(item);
    accumulator[key] ??= [];
    accumulator[key].push(item);
    return accumulator;
  }, {});
}

function encodeFileKey(filePath) {
  return filePath.replaceAll(path.sep, "_").replaceAll("/", "_");
}

function readSnippet(filePath, startLine, endLine) {
  const lines = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
  const snippetStart = Math.max(startLine - 2, 1);
  const snippetEnd = Math.min(endLine + 2, lines.length);
  const snippet = [];

  for (let index = snippetStart; index <= snippetEnd; index += 1) {
    const marker = index >= startLine && index <= endLine ? ">" : " ";
    snippet.push(`${marker} ${String(index).padStart(4, " ")} ${lines[index - 1]}`);
  }

  return snippet.join("\n");
}

function stableId(input) {
  return ts.sys.createHash ? ts.sys.createHash(input).slice(0, 12) : Buffer.from(input).toString("base64url").slice(0, 12);
}

function printFindResults(symbols) {
  if (!symbols.length) {
    console.log("No symbol found");
    return;
  }

  for (const symbol of symbols) {
    console.log(
      [
        symbol.id,
        symbol.kind,
        symbol.moduleHint,
        `${symbol.filePath}:${symbol.lineStart}-${symbol.lineEnd}`,
        symbol.signature ?? "",
      ].join(" | "),
    );
  }
}

function printShowResult(result) {
  console.log(`${result.symbol.name} (${result.symbol.kind})`);
  console.log(`${result.symbol.filePath}:${result.symbol.lineStart}-${result.symbol.lineEnd}`);
  if (result.symbol.signature) {
    console.log(result.symbol.signature);
  }
  console.log("");
  console.log(result.snippet);
}

async function main() {
  const { command, options } = parseArgs(process.argv.slice(2));

  if (command === "index") {
    const symbols = indexProject({
      rootDir: options["--root"] ? path.resolve(options["--root"]) : path.resolve(process.cwd(), "src"),
      outputDir: options["--output"] ? path.resolve(options["--output"]) : DEFAULT_OUTPUT_DIR,
      moduleHint: options["--module"] ?? "core-web",
      sourceSet: options["--source-set"] ?? "main",
    });
    console.log(`Indexed ${symbols.length} symbols into ${options["--output"] ?? DEFAULT_OUTPUT_DIR}`);
    return;
  }

  if (command === "find") {
    printFindResults(
      findSymbols({
        outputDir: options["--output"] ? path.resolve(options["--output"]) : DEFAULT_OUTPUT_DIR,
        name: options["--name"],
      }),
    );
    return;
  }

  if (command === "show") {
    printShowResult(
      showSymbol({
        outputDir: options["--output"] ? path.resolve(options["--output"]) : DEFAULT_OUTPUT_DIR,
        id: options["--id"],
      }),
    );
    return;
  }

  throw new Error(`Unknown command: ${command ?? ""}`);
}

const isEntrypoint = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isEntrypoint) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
