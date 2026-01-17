import styles from "./Oauth2EmailInputPage.module.css"
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";
import InputText from "../../components/ui/InputText.jsx";
import Button from "../../components/ui/Button.jsx";
import {useEffect, useState} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {useCreateSocialMemberMutation} from "../../queries/member.queries.js";
import {useLoginSocialSessionMutation} from "../../queries/auth.queries.js";
import useAuth from "../../auth/AuthContext.jsx";
import {showGlobalAlert} from "../../api/client.js";

export default function Oauth2EmailInputPage() {
    const [searchParams, setSearchParams] = useSearchParams()

    const [provider, setProvider] = useState("")
    const [providerKey, setProviderKey] = useState("")

    const [email, setEmail] = useState("")

    const emailFormatErrorMessage = "이메일 형식이 올바르지 않습니다."
    const emailAlreadyRegisteredErrorMessage = "이미 가입된 이메일 입니다."

    const [registeredEmail, setRegisteredEmail] = useState(null)
    const [emailError, setEmailError] = useState("")

    const [agreementChecked, setAgreementChecked] = useState(false)

    const agreementErrorMessage = "개인정보 수집·이용에 동의해 주세요."
    const [totalError, setTotalError] = useState("")

    const [isLoading, setIsLoading] = useState(false)

    const { socialLogin } = useAuth()
    const navigate = useNavigate()

    const validateEmail = (value) => {
        if (value === registeredEmail) return emailAlreadyRegisteredErrorMessage

        const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
        return ok ? "" : emailFormatErrorMessage
    }

    const validateAgreement = (checked) => {
        const err = checked ? "" : agreementErrorMessage
        setTotalError(err)
        return err === ""
    }

    useEffect(() => {
        setProvider(searchParams.get("from"))
        setProvider((p) => {
            return p.toUpperCase()
        })
        setProviderKey(searchParams.get("id"))
    }, [setSearchParams]);

    const createSocialMemberMutation = useCreateSocialMemberMutation()
    const loginSocialSessionMutation = useLoginSocialSessionMutation()

    const handleSubmit = async (e) => {
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
                const isNew = true
                socialLogin({accessToken, refreshToken, isNew})
                navigate(`/?welcome=true}`)
            } catch (e) {
                await showGlobalAlert("다시 시도해 주세요")
                navigate(`/`)
            }

        } catch(e) {
            if (e.message === emailAlreadyRegisteredErrorMessage) {
                setRegisteredEmail(email)
                setEmailError(emailAlreadyRegisteredErrorMessage)
                return
            }
            setTotalError(e.message)
        }
    }

    return (
        <div className={styles.wrapper}>
            { (createSocialMemberMutation.isPending) && <GlobalSpinner /> }
            <div className={styles.card}>
                <div className={styles.titleWrap}>
                    <img
                        onClick={() => navigate("/")}
                        src="https://i.imgur.com/sYjlI5P.png"
                        alt="moamoa"
                        className={styles.moamoaIcon}
                    />
                    <span className={styles.title}>이메일 입력</span>
                    <span className={styles.copy}>
            모아모아 서비스 이용을 위해 <br/>
            이메일을 입력해 주세요.
          </span>
                </div>

                <form className={styles.form} onSubmit={(e) => handleSubmit(e)}>
                    <div className={styles.inputWrap}>
                        {/* 이메일 */}
                        <div className={styles.input}>
                            <label className={styles.label}>이메일</label>
                            <InputText
                                placeholder="example@moamoa.dev"
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value)
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

                    {/* 동의 */}
                    <div className={styles.agreement}>
                        <input
                            type="checkbox"
                            checked={agreementChecked}
                            onChange={(e) => {
                                setAgreementChecked(e.target.checked)
                                if (totalError) setTotalError("")
                            }}
                            onBlur={() => validateAgreement(agreementChecked)}
                        />
                        <span>개인정보 수집·이용에 동의합니다 (필수)</span>
                    </div>

                    {
                        totalError !== "" && (
                            <div className={styles.totalError}>
                                <span className={styles.error}>✕ {totalError}</span>
                            </div>
                        )
                    }

                    <Button type="submit">
                        완료
                    </Button>
                </form>
            </div>
        </div>
    )
}