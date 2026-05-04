import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

import { extractSymbolsFromFile, findSymbols, indexProject, normalizeName, showSymbol } from "./symbolify.mjs";

test("normalizeName lowercases symbols", () => {
  assert.equal(normalizeName("AuthContext"), "authcontext");
});

test("extractSymbolsFromFile indexes hooks, components and context", () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "symbolify-react-"));
  const filePath = path.join(tempDir, "AuthContext.tsx");
  fs.writeFileSync(
    filePath,
    `
      import { createContext } from "react";

      export const AuthContext = createContext(null);

      export function useAuth() {
        return AuthContext;
      }

      export const LoginModal = () => <div>Login</div>;
    `,
  );

  const symbols = extractSymbolsFromFile(filePath, "core-web", "main");
  assert.ok(symbols.some((symbol) => symbol.name === "AuthContext" && symbol.kind === "context"));
  assert.ok(symbols.some((symbol) => symbol.name === "useAuth" && symbol.kind === "hook"));
  assert.ok(symbols.some((symbol) => symbol.name === "LoginModal" && symbol.kind === "component"));
});

test("indexProject creates lookup files and show snippets", () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "symbolify-react-project-"));
  const srcDir = path.join(tempDir, "src");
  const outputDir = path.join(tempDir, "build", "symbolify");
  fs.mkdirSync(srcDir, { recursive: true });
  fs.writeFileSync(
    path.join(srcDir, "useAuth.ts"),
    `
      export function useAuth() {
        return "ok";
      }
    `,
  );

  const symbols = indexProject({
    rootDir: srcDir,
    outputDir,
    moduleHint: "core-web",
    sourceSet: "main",
  });
  assert.equal(symbols.length, 1);

  const found = findSymbols({ outputDir, name: "useAuth" });
  assert.equal(found.length, 1);
  assert.equal(found[0].normalizedName, "useauth");

  const shown = showSymbol({ outputDir, id: found[0].id });
  assert.match(shown.snippet, /useAuth/);
});
