import { useEffect, useMemo, useRef, useState } from "react"
import styles from "./LoginModal.module.css"
import useModalAccessibility from "../../hooks/useModalAccessibility.js"
import ModalShell from "../ModalShell/ModalShell.jsx"
import TextInput from "../ui/TextInput.jsx"
import Button from "../ui/Button.jsx"
import useAuth from "../../auth/AuthContext.jsx";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_REGEX = /^(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{8,32}$/

export default function LoginModal({ open, onClose, onSubmit, onClickSignup }) {
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const panelRef = useRef(null)
    const { login } = useAuth()

    const [touched, setTouched] = useState({
        email: false,
        password: false,
    })

    useModalAccessibility({ open, onClose, panelRef })

    useEffect(() => {
        if (!open) return
        setEmail("")
        setPassword("")
        setTouched({ email: false, password: false })
        setTimeout(() => panelRef.current?.querySelector("input")?.focus(), 0)
    }, [open])

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

        return e
    }, [email, password, touched])

    if (!open) return null

    const canSubmit =
        email.trim() &&
        password &&
        EMAIL_REGEX.test(email) &&
        !errors.email &&
        !errors.password

    const handleSubmit = async (e) => {
        e.preventDefault()

        setTouched({ email: true, password: true })
        if (!canSubmit) return

        // const res = await loginApi(email, password, (err) => {
        //     showGlobalAlert("이메일 또는 메세지가 일치하지 않습니다.")
        // })
        //
        // localStorage.setItem("accessToken", res.accessToken)
        // localStorage.setItem("refreshToken", res.refreshToken)

        try {
            await login({ email, password })
            onClose?.() // 로그인 성공 시에만 닫힘
        } catch {
        }
    }

    return (
        <ModalShell open={open} title="로그인" onClose={onClose}>
            <form className={styles.form} onSubmit={handleSubmit}>
                <TextInput
                    label="이메일"
                    placeholder="you@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    onBlur={() => setTouched((t) => ({ ...t, email: true }))}
                    autoComplete="email"
                    error={errors.email}
                />

                <TextInput
                    label="비밀번호"
                    type="password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onBlur={() => setTouched((t) => ({ ...t, password: true }))}
                    autoComplete="current-password"
                    error={errors.password}
                />

                <div className={styles.submitButtonWrap}>
                    <Button type="submit" disabled={!canSubmit} aria-disabled={!canSubmit}>
                        로그인
                    </Button>
                </div>

                <div className={styles.footer}>
                    <span>계정이 없나요?</span>
                    <Button type="button" variant="link" onClick={onClickSignup}>
                        회원가입
                    </Button>
                </div>
            </form>
        </ModalShell>
    )
}