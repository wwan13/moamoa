import { useEffect, useState } from "react"
import { EditorContent, useEditor } from "@tiptap/react"
import StarterKit from "@tiptap/starter-kit"
import Link from "@tiptap/extension-link"
import { useNavigate } from "react-router-dom"
import FormatBoldOutlinedIcon from "@mui/icons-material/FormatBoldOutlined"
import FormatItalicOutlinedIcon from "@mui/icons-material/FormatItalicOutlined"
import FormatListBulletedOutlinedIcon from "@mui/icons-material/FormatListBulletedOutlined"
import FormatQuoteOutlinedIcon from "@mui/icons-material/FormatQuoteOutlined"
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined"
import { showGlobalAlert, showToast } from "../../api/client"
import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import Button from "../../components/ui/Button.tsx"
import styles from "./NoticeCreatePage.module.css"
import { useCreateNoticeMutation } from "../../queries/notice.queries"

const headingOptions = [
  { label: "H1", level: 1 },
  { label: "H2", level: 2 },
  { label: "H3", level: 3 },
]

const NoticeCreatePage = () => {
  const navigate = useNavigate()
  const createNoticeMutation = useCreateNoticeMutation()
  const [title, setTitle] = useState("")
  const [chip, setChip] = useState("")

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  const editor = useEditor({
    extensions: [
      StarterKit,
      Link.configure({
        openOnClick: false,
        autolink: true,
      }),
    ],
    content: "<p></p>",
    immediatelyRender: false,
  })

  const handleLink = async () => {
    if (!editor) return

    const previousUrl = editor.getAttributes("link").href as string | undefined
    const nextUrl = window.prompt("링크 URL을 입력해 주세요.", previousUrl ?? "")

    if (nextUrl === null) return

    if (!nextUrl.trim()) {
      editor.chain().focus().extendMarkRange("link").unsetLink().run()
      return
    }

    editor
      .chain()
      .focus()
      .extendMarkRange("link")
      .setLink({ href: nextUrl.trim() })
      .run()
  }

  const handleSubmit = async () => {
    const trimmedTitle = title.trim()
    const trimmedChip = chip.trim()
    const html = editor?.getHTML() ?? ""
    const text = editor?.getText().trim() ?? ""

    if (!trimmedTitle) {
      await showGlobalAlert("제목을 입력해 주세요.")
      return
    }

    if (!trimmedChip) {
      await showGlobalAlert("칩 문구를 입력해 주세요.")
      return
    }

    if (!text) {
      await showGlobalAlert("본문을 입력해 주세요.")
      return
    }

    try {
      await createNoticeMutation.mutateAsync({
        title: trimmedTitle,
        chip: trimmedChip,
        content: html,
        published: false,
      })
      showToast("공지사항을 등록했습니다.", { type: "success" })
      navigate("/notice")
    } catch {
      await showGlobalAlert("공지사항 등록에 실패했습니다.")
    }
  }

  return (
    <div className={styles.wrap}>
      <PageTitle value="공지사항 등록" />

      <section className={styles.form}>
        <div className={styles.row}>
          <label className={styles.field}>
            <span className={styles.label}>칩</span>
            <input
              className={styles.input}
              value={chip}
              onChange={(event) => setChip(event.target.value)}
              placeholder="예: 긴급공지"
              maxLength={256}
            />
          </label>

          <label className={styles.field}>
            <span className={styles.label}>제목</span>
            <input
              className={styles.input}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="공지사항 제목을 입력해 주세요"
              maxLength={2048}
            />
          </label>
        </div>

        <section className={styles.editorSection}>
          <div className={styles.editorFrame}>
            <div className={styles.toolbar}>
              <div className={styles.toolGroup}>
                {headingOptions.map((heading) => (
                  <button
                    key={heading.label}
                    type="button"
                    className={`${styles.toolButton} ${
                      editor?.isActive("heading", { level: heading.level })
                        ? styles.toolButtonActive
                        : ""
                    }`}
                    onClick={() =>
                      editor
                        ?.chain()
                        .focus()
                        .toggleHeading({ level: heading.level })
                        .run()
                    }
                    disabled={!editor}
                  >
                    {heading.label}
                  </button>
                ))}
              </div>
              <div className={styles.toolGroup}>
                <button
                  type="button"
                  className={`${styles.toolButton} ${
                    editor?.isActive("bold") ? styles.toolButtonActive : ""
                  }`}
                  onClick={() => editor?.chain().focus().toggleBold().run()}
                  disabled={!editor}
                >
                  <FormatBoldOutlinedIcon sx={{ fontSize: 16 }} />
                </button>
                <button
                  type="button"
                  className={`${styles.toolButton} ${
                    editor?.isActive("italic") ? styles.toolButtonActive : ""
                  }`}
                  onClick={() => editor?.chain().focus().toggleItalic().run()}
                  disabled={!editor}
                >
                  <FormatItalicOutlinedIcon sx={{ fontSize: 16 }} />
                </button>
              </div>
              <div className={styles.toolGroup}>
                <button
                  type="button"
                  className={`${styles.toolButton} ${
                    editor?.isActive("bulletList") ? styles.toolButtonActive : ""
                  }`}
                  onClick={() => editor?.chain().focus().toggleBulletList().run()}
                  disabled={!editor}
                >
                  <FormatListBulletedOutlinedIcon sx={{ fontSize: 16 }} />
                </button>
                <button
                  type="button"
                  className={`${styles.toolButton} ${
                    editor?.isActive("blockquote")
                      ? styles.toolButtonActive
                      : ""
                  }`}
                  onClick={() => editor?.chain().focus().toggleBlockquote().run()}
                  disabled={!editor}
                >
                  <FormatQuoteOutlinedIcon sx={{ fontSize: 16 }} />
                </button>
              </div>
              <div className={styles.toolGroup}>
                <button
                  type="button"
                  className={`${styles.toolButton} ${
                    editor?.isActive("link") ? styles.toolButtonActive : ""
                  }`}
                  onClick={() => {
                    void handleLink()
                  }}
                  disabled={!editor}
                >
                  <LinkOutlinedIcon sx={{ fontSize: 16 }} />
                </button>
              </div>
            </div>
            <div className={styles.editorBody}>
              <EditorContent editor={editor} />
            </div>
          </div>
        </section>

        <div className={styles.actions}>
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate("/notice")}
          >
            목록으로
          </Button>
          <Button
            type="button"
            onClick={() => {
              void handleSubmit()
            }}
            disabled={createNoticeMutation.isPending || !editor}
          >
            등록하기
          </Button>
        </div>
      </section>
    </div>
  )
}

export default NoticeCreatePage
