import { useEffect, useMemo, useRef, useState } from "react"
import styles from "./LoginModal.module.css"
import useModalAccessibility from "../../hooks/useModalAccessibility.js"
import ModalShell from "../ModalShell/ModalShell.jsx"
import Button from "../ui/Button.jsx"
import useAuth from "../../auth/AuthContext.jsx"
import InputText from "../ui/InputText.jsx";
import GlobalSpinner from "../GlobalSpinner/GlobalSpinner.jsx";

export default function LoginModal({
                                       open,
                                       onClose,
                                       onClickSignup,
                                        onClickPasswordFind
                                   }) {
    const [isLoading, setIsLoading] = useState(false)

    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const panelRef = useRef(null)
    const [hasError, setHasError] = useState(false)

    const { login, isLoginLoading } = useAuth()

    useModalAccessibility({ open, onClose, panelRef })

    const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

    useEffect(() => {
        setIsLoading(false)
        if (!open) return
        setHasError(false)
        setEmail("")
        setPassword("")
        setTimeout(() => panelRef.current?.querySelector("input")?.focus(), 0)
    }, [open])

    if (!open) return null

    const handleSubmit = async (e) => {
        e.preventDefault()

        try {
            await login({ email, password })
            setHasError(true)
            onClose?.()
        } catch {
            setHasError(true)
        }
    }

    return (
        <ModalShell open={open} title="" onClose={onClose}>
            {isLoginLoading || isLoading && <GlobalSpinner />}

            <form className={styles.form} onSubmit={handleSubmit}>
                <img
                    src="https://i.imgur.com/CHYokw0.png"
                    alt="moamoa"
                    className={styles.moamoaIcon}
                />
                <InputText
                    placeholder="이메일"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="email"
                    hasError={false}
                />

                <InputText
                    type="password"
                    placeholder="비밀번호"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="current-password"
                    hasError={false}
                />

                {
                    hasError ? (
                        <div className={styles.errorArea}>
                            이메일 혹은 비밀번호를 확인해 주세요.
                        </div>
                    ) : (<></>)
                }

                <div className={styles.submitButtonWrap}>
                    <Button type="submit">
                        로그인
                    </Button>
                </div>

                <div className={styles.footer}>
                    <Button
                        type="button"
                        variant="link"
                        onClick={onClickSignup}
                    >
                        회원가입
                    </Button>

                    <Button
                        type="button"
                        variant="link"
                        onClick={onClickPasswordFind}
                    >
                        비밀번호 찾기
                    </Button>
                </div>
            </form>
            <div className={styles.socialSection}>
                <span className={styles.else}>또는</span>
                <div className={styles.socials}>
                    <button
                        className={styles.social}
                        onClick={() => {
                            setIsLoading(true)
                            window.location.href = `${API_BASE}/oauth2/authorization/google`;
                        }}
                    >
                        <img
                            className={styles.socialImg}
                            src="https://i.imgur.com/xWcCM6A.png"
                            alt="Google 계정으로 계속하기"/>
                    </button>
                    <button
                        className={styles.social}
                        onClick={() => {
                            setIsLoading(true)
                            window.location.href = `${API_BASE}/oauth2/authorization/github`;
                        }}
                    >
                        <img
                            className={styles.socialImg}
                            src="https://upload.wikimedia.org/wikipedia/commons/thumb/9/95/Font_Awesome_5_brands_github.svg/250px-Font_Awesome_5_brands_github.svg.png"
                            alt="Github 계정으로 계속하기"/>
                    </button>
                </div>
            </div>
        </ModalShell>
    )
}