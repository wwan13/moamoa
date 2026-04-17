import styles from "./FindPasswordPage.module.css"
import InputText from "../../components/ui/InputText"
import Button from "../../components/ui/Button"
import { useEffect, useState, type FormEvent } from "react"
import { useNavigate } from "react-router-dom"
import { useApplyTemporaryPasswordMutation } from "../../queries/member.queries"
import GlobalSpinner from "../../components/globalspinner/GlobalSpinner"
import { showGlobalAlert } from "../../api/client"

const FindPasswordPage = () => {
  const navigate = useNavigate()

  const [email, setEmail] = useState("")
  const [emailError, setEmailError] = useState("")

  const applyTemporaryPasswordMutation = useApplyTemporaryPasswordMutation()

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  const emailFormatErrorMessage = "이메일 형식이 올바르지 않습니다."

  const validateEmail = (value) => {
    const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
    if (!ok) {
      setEmailError(emailFormatErrorMessage)
    }
    return ok ? "" : emailFormatErrorMessage
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (validateEmail(email) != "") return

    try {
      await applyTemporaryPasswordMutation.mutateAsync({ email })
      navigate("/find/password/complete")
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : "알 수 없는 오류가 발생했어요."
      showGlobalAlert(message)
    }
  }

  return (
    <div className={styles.wrapper}>
      {applyTemporaryPasswordMutation.isPending && <GlobalSpinner />}
      <div className={styles.card}>
        <div className={styles.titleWrap}>
          <img
            onClick={() => navigate("/")}
            src="/moamoa_sub_logo.png"
            alt="moamoa"
            className={styles.moamoaIcon}
          />
          <span className={styles.title}>비밀번호 찾기</span>
          <span className={styles.copy}>
            가입 시 등록했던 이메일로 <br />
            임시 비밀번호를 전송해드립니다.
          </span>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.inputWrap}>
            <div className={styles.input}>
              <label className={styles.label}>이메일</label>
              <InputText
                placeholder="이메일 주소를 입력해 주세요"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  if (emailError) setEmailError("")
                }}
                onBlur={() => setEmailError(validateEmail(email))}
                hasError={emailError !== ""}
              />
              {emailError !== "" && (
                <span className={styles.error}>{emailError}</span>
              )}
            </div>
            <Button type="submit">임시 비밀번호 받기</Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default FindPasswordPage
