import { useEffect, useRef, useState } from "react"
import styles from "./LoginModal.module.css"
import useModalAccessibility from "../../hooks/useModalAccessibility"
import ModalShell from "../modalshell/ModalShell"
import Button from "../ui/Button"
import useAuth from "../../auth/useAuth"
import InputText from "../ui/InputText"
import GlobalSpinner from "../globalspinner/GlobalSpinner"
import type { FormEvent } from "react"

type LoginModalProps = {
    open: boolean
    onClose: () => void
    onClickSignup: () => void
    onClickPasswordFind?: () => void
}

const LoginModal = ({
    open,
    onClose,
    onClickSignup,
    onClickPasswordFind,
}: LoginModalProps) => {
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const panelRef = useRef<HTMLDivElement | null>(null)
    const [hasError, setHasError] = useState(false)

    const { login, isLoginLoading } = useAuth()

    useModalAccessibility({ open, onClose, panelRef })

    const API_BASE = import.meta.env.VITE_API_BASE_URL || ""

    useEffect(() => {
        if (!open) return
        setTimeout(() => panelRef.current?.querySelector("input")?.focus(), 0)
    }, [open])

    if (!open) return null

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
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
            {isLoginLoading && <GlobalSpinner />}

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

export default LoginModal
