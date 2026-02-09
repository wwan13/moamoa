import styles from "./Oauth2EmailInputPage.module.css"
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner"
import InputText from "../../components/ui/InputText"
import Button from "../../components/ui/Button"
import { useMemo, useState, type FormEvent } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { useCreateSocialMemberMutation } from "../../queries/member.queries"
import { useLoginSocialSessionMutation } from "../../queries/auth.queries"
import useAuth from "../../auth/useAuth"
import { showGlobalAlert } from "../../api/client"
import type { Provider } from "../../api/member.api"

const Oauth2EmailInputPage = () => {
    const [searchParams] = useSearchParams()

    const provider = useMemo<Provider>(() => {
        const from = String(searchParams.get("from") ?? "GOOGLE").toUpperCase()
        if (from === "GITHUB" || from === "INTERNAL") return from
        return "GOOGLE"
    }, [searchParams])

    const providerKey = useMemo(() => searchParams.get("id") ?? "", [searchParams])

    const [email, setEmail] = useState("")

    const emailFormatErrorMessage = "이메일 형식이 올바르지 않습니다."
    const emailAlreadyRegisteredErrorMessage = "이미 가입된 이메일 입니다."

    const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)
    const [emailError, setEmailError] = useState("")

    const [agreementChecked, setAgreementChecked] = useState(false)

    const agreementErrorMessage = "개인정보 수집·이용에 동의해 주세요."
    const [totalError, setTotalError] = useState("")

    const { socialLogin } = useAuth()
    const navigate = useNavigate()

    const validateEmail = (value: string): string => {
        if (value === registeredEmail) return emailAlreadyRegisteredErrorMessage

        const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
        return ok ? "" : emailFormatErrorMessage
    }

    const validateAgreement = (checked: boolean): boolean => {
        const err = checked ? "" : agreementErrorMessage
        setTotalError(err)
        return err === ""
    }

    const createSocialMemberMutation = useCreateSocialMemberMutation()
    const loginSocialSessionMutation = useLoginSocialSessionMutation()

    const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
        e.preventDefault()

        const eErr = validateEmail(email)
        setEmailError(eErr)

        const emailOk = eErr === ""
        const agreeOk = validateAgreement(agreementChecked)

        if (!emailOk || !agreeOk) return

        try {
            const res = await createSocialMemberMutation.mutateAsync({ email, provider, providerKey })

            try {
                const memberId = res?.member?.id
                const token = res?.token

                const loginRes = await loginSocialSessionMutation.mutateAsync({ memberId, token })
                const accessToken = loginRes?.accessToken
                const refreshToken = loginRes?.refreshToken

                await socialLogin({ accessToken, refreshToken, isNew: true })
                navigate("/?welcome=true")
            } catch {
                await showGlobalAlert("다시 시도해 주세요")
                navigate("/")
            }
        } catch (error: unknown) {
            const message = error instanceof Error ? error.message : "알 수 없는 오류가 발생했어요."
            if (message === emailAlreadyRegisteredErrorMessage) {
                setRegisteredEmail(email)
                setEmailError(emailAlreadyRegisteredErrorMessage)
                return
            }
            setTotalError(message)
        }
    }

    return (
        <div className={styles.wrapper}>
            {createSocialMemberMutation.isPending && <GlobalSpinner />}
            <div className={styles.card}>
                <div className={styles.titleWrap}>
                    <img
                        onClick={() => navigate("/")}
                        src="https://i.imgur.com/l8hsGOD.png"
                        alt="moamoa"
                        className={styles.moamoaIcon}
                    />
                    <span className={styles.title}>이메일 입력</span>
                    <span className={styles.copy}>
                        모아모아 서비스 이용을 위해 <br />
                        이메일을 입력해 주세요.
                    </span>
                </div>

                <form className={styles.form} onSubmit={handleSubmit}>
                    <div className={styles.inputWrap}>
                        <div className={styles.input}>
                            <label className={styles.label}>이메일</label>
                            <InputText
                                placeholder="example@moamoa.dev"
                                value={email}
                                onChange={(event) => {
                                    setEmail(event.target.value)
                                    if (emailError) setEmailError("")
                                }}
                                onBlur={() => setEmailError(validateEmail(email))}
                                hasError={emailError !== ""}
                            />
                            {emailError !== "" && (
                                <span className={styles.error}>✕ {emailError}</span>
                            )}
                        </div>
                    </div>

                    <div className={styles.agreement}>
                        <input
                            type="checkbox"
                            checked={agreementChecked}
                            onChange={(event) => {
                                setAgreementChecked(event.target.checked)
                                if (totalError) setTotalError("")
                            }}
                            onBlur={() => validateAgreement(agreementChecked)}
                        />
                        <span>개인정보 수집·이용에 동의합니다 (필수)</span>
                    </div>

                    {totalError !== "" && (
                        <div className={styles.totalError}>
                            <span className={styles.error}>✕ {totalError}</span>
                        </div>
                    )}

                    <Button type="submit">완료</Button>
                </form>
            </div>
        </div>
    )
}

export default Oauth2EmailInputPage
