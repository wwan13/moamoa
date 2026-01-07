import { useEffect, useMemo, useState } from "react"
import styles from "./SignupModal.module.css"
import ModalShell from "../ModalShell/ModalShell.jsx"
import TextInput from "../ui/TextInput.jsx"
import Button from "../ui/Button.jsx"
import {emailVerificationApi, emailVerificationConfirmApi} from "../../api/auth.api.js";
import {showGlobalAlert, showToast} from "../../api/client.js";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_REGEX = /^(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{8,32}$/

const CODE_TTL_SEC = 180

function formatMMSS(totalSec) {
    const m = String(Math.floor(totalSec / 60)).padStart(2, "0")
    const s = String(totalSec % 60).padStart(2, "0")
    return `${m}:${s}`
}

export default function SignupModal({ open, onClose, onSubmit, onClickLogin }) {
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [passwordConfirm, setPasswordConfirm] = useState("")
    const [code, setCode] = useState("")
    const [codeSent, setCodeSent] = useState(false)

    const [expiresSec, setExpiresSec] = useState(0)      // ✅ 추가: 타이머
    const [codeVerified, setCodeVerified] = useState(false) // ✅ 추가: 인증 완료 여부

    const [touched, setTouched] = useState({
        email: false,
        password: false,
        passwordConfirm: false,
        code: false,
    })

    useEffect(() => {
        if (!open) return
        setEmail("")
        setPassword("")
        setPasswordConfirm("")
        setCode("")
        setCodeSent(false)
        setExpiresSec(0)
        setCodeVerified(false)
        setTouched({ email: false, password: false, passwordConfirm: false, code: false })
    }, [open])

    // ✅ 타이머 감소 (인증 완료면 멈춤)
    useEffect(() => {
        if (!codeSent) return
        if (codeVerified) return
        if (expiresSec <= 0) return

        const id = setInterval(() => setExpiresSec((s) => s - 1), 1000)
        return () => clearInterval(id)
    }, [codeSent, codeVerified, expiresSec])

    const errors = useMemo(() => {
        const e = {}

        if (touched.email) {
            if (!email.trim()) e.email = "이메일을 입력해 주세요."
            else if (!EMAIL_REGEX.test(email)) e.email = "이메일 형식이 올바르지 않아요."
        }

        if (touched.password) {
            if (!password) e.password = "비밀번호를 입력해 주세요."
            else if (!PASSWORD_REGEX.test(password)) {
                e.password = "8~32자리 + 특수문자 1개 이상 포함해야 해요."
            }
        }

        if (touched.passwordConfirm) {
            if (!passwordConfirm) e.passwordConfirm = "비밀번호 확인을 입력해 주세요."
            else if (passwordConfirm !== password) e.passwordConfirm = "비밀번호가 일치하지 않아요."
        }

        if (codeSent && touched.code && !codeVerified) {
            if (expiresSec <= 0) e.code = "시간이 만료됐어요."
            else if (!code.trim()) e.code = "인증번호를 입력해 주세요."
            else if (!/^[0-9]{6}$/.test(code)) e.code = "인증번호는 6자리 숫자예요."
        }

        return e
    }, [email, password, passwordConfirm, code, codeSent, touched, expiresSec, codeVerified])

    if (!open) return null

    const canSendCode = EMAIL_REGEX.test(email) && !codeVerified
    const canVerifyCode =
        codeSent &&
        !codeVerified &&
        expiresSec > 0 &&
        /^[0-9]{6}$/.test(code)

    const canSubmit =
        codeVerified && // ✅ 인증 완료 필수
        !errors.email &&
        !errors.password &&
        !errors.passwordConfirm &&
        email &&
        password &&
        passwordConfirm

    const handleSendCode = async () => {
        setTouched((t) => ({ ...t, email: true }))

        if (!EMAIL_REGEX.test(email)) return

        await emailVerificationApi(email, (err) => {
            showGlobalAlert("이미 존재하는 이메일 입니다.")
        })
        showToast("인증번호가 전송되었습니다.")

        setCodeSent(true)
        setCodeVerified(false)
        setCode("")
        setExpiresSec(CODE_TTL_SEC)
    }

    const handleVerifyCode = async () => {
        setTouched((t) => ({ ...t, code: true }))

        if (!canVerifyCode) return

        await emailVerificationConfirmApi(email, code, (err) => {
            showGlobalAlert("인증번호가 올바르지 않습니다.")
        })

        showToast("인증되었습니다.")
        setCodeVerified(true)
    }

    const handleSubmit = (e) => {
        e.preventDefault()

        setTouched({ email: true, password: true, passwordConfirm: true, code: true })

        if (!canSubmit) return

        onSubmit?.({ email, password, code })
    }

    return (
        <ModalShell open={open} title="회원가입" onClose={onClose}>
            <form className={styles.form} onSubmit={handleSubmit}>
                <TextInput
                    label="이메일"
                    placeholder="you@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.id)}
                    onBlur={() => setTouched((t) => ({ ...t, email: true }))}
                    error={errors.email}
                    right={
                        <Button
                            type="button"
                            variant="secondary"
                            onClick={handleSendCode}
                            disabled={!canSendCode}
                            aria-disabled={!canSendCode}
                        >
                            인증번호 전송
                        </Button>
                    }
                    disabled={codeVerified}  /* ✅ 인증 완료 시 이메일 잠금(원치 않으면 제거) */
                />

                {codeSent ? (
                    <TextInput
                        label="인증번호"
                        placeholder="6자리 숫자"
                        value={code}
                        onChange={(e) => setCode(e.target.id)}
                        onBlur={() => setTouched((t) => ({ ...t, code: true }))}
                        error={errors.code}
                        success={codeVerified ? "인증 완료" : ""}   /* ✅ 에러 자리 = 성공 메시지 */
                        labelRight={!codeVerified ? formatMMSS(expiresSec) : ""} /* ✅ 3분 타이머 */
                        inputMode="numeric"
                        disabled={codeVerified} /* ✅ 인증 완료 시 입력 비활성화 */
                        right={
                            <Button
                                type="button"
                                variant="secondary"
                                onClick={handleVerifyCode}
                                disabled={!canVerifyCode}
                                aria-disabled={!canVerifyCode}
                            >
                                확인
                            </Button>
                        }
                    />
                ) : null}

                <TextInput
                    label="비밀번호"
                    type="password"
                    placeholder="8~32자리, 특수문자 포함"
                    value={password}
                    onChange={(e) => setPassword(e.target.id)}
                    onBlur={() => setTouched((t) => ({ ...t, password: true }))}
                    error={errors.password}
                />

                <TextInput
                    label="비밀번호 확인"
                    type="password"
                    placeholder="비밀번호 재입력"
                    value={passwordConfirm}
                    onChange={(e) => setPasswordConfirm(e.target.id)}
                    onBlur={() => setTouched((t) => ({ ...t, passwordConfirm: true }))}
                    error={errors.passwordConfirm}
                />

                <div className={styles.submitButtonWrap}>
                    <Button type="submit" disabled={!canSubmit} aria-disabled={!canSubmit}>
                        회원가입
                    </Button>
                </div>

                <div className={styles.footer}>
                    <span>이미 계정이 있나요?</span>
                    <Button type="button" variant="link" onClick={onClickLogin}>
                        로그인
                    </Button>
                </div>
            </form>
        </ModalShell>
    )
}