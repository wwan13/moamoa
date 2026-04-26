import CheckCircleOutlineRoundedIcon from "@mui/icons-material/CheckCircleOutlineRounded"
import { useEffect } from "react"
import { useNavigate } from "react-router-dom"
import Button from "../../components/ui/Button"
import styles from "./FindPasswordCompletePage.module.css"

const FindPasswordCompletePage = () => {
  const navigate = useNavigate()

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  return (
    <div className={styles.wrapper}>
      <div className={styles.content}>
        <CheckCircleOutlineRoundedIcon className={styles.icon} />

        <div className={styles.textGroup}>
          <h1 className={styles.title}>이메일로 발송 완료되었습니다.</h1>
          <p className={styles.description}>
            로그인 후 마이페이지에서 반드시 비밀번호를 변경해주세요!
          </p>
        </div>

        <Button
          type="button"
          className={styles.button}
          onClick={() => navigate("/login")}
        >
          로그인 하러가기
        </Button>
      </div>
    </div>
  )
}

export default FindPasswordCompletePage
