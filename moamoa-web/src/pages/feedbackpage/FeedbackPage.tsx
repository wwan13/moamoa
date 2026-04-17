import { useEffect, useState } from "react"
import type { FormEvent, ReactNode } from "react"
import PersonRoundedIcon from "@mui/icons-material/PersonRounded"
import MailOutlineRoundedIcon from "@mui/icons-material/MailOutlineRounded"
import SettingsRoundedIcon from "@mui/icons-material/SettingsRounded"
import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded"
import FeedOutlinedIcon from "@mui/icons-material/FeedOutlined"
import ChatBubbleOutlineRoundedIcon from "@mui/icons-material/ChatBubbleOutlineRounded"
import InputText from "../../components/ui/InputText"
import TextArea from "../../components/ui/TextArea"
import { showGlobalAlert } from "../../api/client"
import styles from "./FeedbackPage.module.css"

type FeedbackCategory = {
  id: string
  label: string
  icon: ReactNode
}

const FEEDBACK_CATEGORIES: FeedbackCategory[] = [
  {
    id: "account",
    label: "계정/ 로그인",
    icon: <PersonRoundedIcon sx={{ fontSize: 18 }} />,
  },
  {
    id: "subscription",
    label: "이메일 구독",
    icon: <MailOutlineRoundedIcon sx={{ fontSize: 18 }} />,
  },
  {
    id: "bug",
    label: "기능 오류 / 버그",
    icon: <SettingsRoundedIcon sx={{ fontSize: 18 }} />,
  },
  {
    id: "feature",
    label: "기능 제안",
    icon: <AutoAwesomeRoundedIcon sx={{ fontSize: 18 }} />,
  },
  {
    id: "content",
    label: "블로그 / 콘텐츠",
    icon: <FeedOutlinedIcon sx={{ fontSize: 18 }} />,
  },
  {
    id: "service",
    label: "서비스 피드백",
    icon: <ChatBubbleOutlineRoundedIcon sx={{ fontSize: 18 }} />,
  },
]

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const CONTENT_MAX_LENGTH = 1000

const FeedbackPage = () => {
  const [selectedCategory, setSelectedCategory] = useState<string>("")
  const [content, setContent] = useState<string>("")
  const [email, setEmail] = useState<string>("")
  const [contentError, setContentError] = useState<string>("")
  const [emailError, setEmailError] = useState<string>("")

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  const validateEmail = (nextEmail: string): boolean => {
    const trimmedEmail = nextEmail.trim()

    if (trimmedEmail.length === 0) {
      setEmailError("이메일을 입력해 주세요.")
      return false
    }

    if (!EMAIL_REGEX.test(trimmedEmail)) {
      setEmailError("올바른 이메일 형식이 아닙니다.")
      return false
    }

    setEmailError("")
    return true
  }

  const isSubmitEnabled =
    selectedCategory !== "" &&
    content.trim().length > 0 &&
    content.length <= CONTENT_MAX_LENGTH &&
    email.trim().length > 0 &&
    EMAIL_REGEX.test(email.trim())

  const validateContent = (nextContent: string): boolean => {
    if (nextContent.trim().length === 0) {
      setContentError("내용을 입력해 주세요.")
      return false
    }

    if (nextContent.length > CONTENT_MAX_LENGTH) {
      setContentError(`최대 ${CONTENT_MAX_LENGTH}자까지 입력 가능해요.`)
      return false
    }

    setContentError("")
    return true
  }

  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> => {
    event.preventDefault()

    const contentValid = validateContent(content)
    const emailValid = validateEmail(email)

    if (selectedCategory === "") {
      await showGlobalAlert("문의 유형과 내용을 모두 입력해 주세요.")
      return
    }

    if (!contentValid) {
      await showGlobalAlert(content.trim().length === 0 ? "내용을 입력해 주세요." : `최대 ${CONTENT_MAX_LENGTH}자까지 입력 가능해요.`)
      return
    }

    if (!emailValid) {
      await showGlobalAlert("이메일 형식을 다시 확인해 주세요.")
      return
    }

    await showGlobalAlert("문의 접수 기능은 준비 중입니다.")
  }

  return (
    <section className={styles.page}>
      <div className={styles.hero}>
        <h1 className={styles.title}>문의 및 피드백</h1>
        <p className={styles.description}>
          작은 의견 하나하나가 모아모아를 만들어가요.
          <br />
          남겨주신 내용은 다 읽고 답변드릴게요.
        </p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.group}>
          <label className={styles.label}>어떤 내용인가요?</label>
          <div className={styles.categoryGrid}>
            {FEEDBACK_CATEGORIES.map((category) => {
              const isSelected = selectedCategory === category.id
              return (
                <button
                  key={category.id}
                  type="button"
                  className={`${styles.categoryButton} ${isSelected ? styles.categoryButtonSelected : ""}`}
                  onClick={() => setSelectedCategory(category.id)}
                >
                  <span className={styles.categoryIcon}>{category.icon}</span>
                  <span>{category.label}</span>
                </button>
              )
            })}
          </div>
        </div>

        <div className={styles.group}>
          <label htmlFor="feedback-content" className={styles.label}>
            내용
          </label>
          <TextArea
            id="feedback-content"
            placeholder="내용을 입력해 주세요"
            value={content}
            hasError={contentError !== ""}
            onChange={(event) => {
              const nextContent = event.target.value
              setContent(nextContent)

              if (contentError !== "") {
                validateContent(nextContent)
              }
            }}
            onBlur={() => validateContent(content)}
          />
          {contentError !== "" && (
            <span className={styles.errorMessage}>{contentError}</span>
          )}
        </div>

        <div className={styles.group}>
          <label htmlFor="feedback-email" className={styles.label}>
            이메일
          </label>
          <InputText
            id="feedback-email"
            type="email"
            placeholder="답변 받을 이메일 주소를 입력해 주세요"
            value={email}
            onChange={(event) => {
              const nextEmail = event.target.value
              setEmail(nextEmail)
              if (emailError !== "") {
                validateEmail(nextEmail)
              }
            }}
            onBlur={() => validateEmail(email)}
            hasError={emailError !== ""}
          />
          {emailError !== "" && (
            <span className={styles.errorMessage}>{emailError}</span>
          )}
        </div>

        <button
          type="submit"
          className={styles.submitButton}
          disabled={!isSubmitEnabled}
        >
          보내기
        </button>
      </form>
    </section>
  )
}

export default FeedbackPage
