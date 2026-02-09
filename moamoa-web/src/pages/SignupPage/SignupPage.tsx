import styles from "./SignupPage.module.css"
import InputText from "../../components/ui/InputText"
import Button from "../../components/ui/Button"
import { useState, type FormEvent } from "react"
import {useNavigate} from "react-router-dom";
import {useSignupMutation} from "../../queries/auth.queries";
import useAuth from "../../auth/useAuth";
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner";

const SignupPage = () => {
    const navigate = useNavigate()

    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [passwordConfirm, setPasswordConfirm] = useState("")

    const [registeredEmail, setRegisteredEmail] = useState(null)

    const emailFormatErrorMessage = "이메일 형식이 올바르지 않습니다."
    const emailAlreadyRegisteredErrorMessage = "이미 가입된 이메일 입니다."
    const passwordSizeMessage = "8자리 이상 32자리 이하로 구성해 주세요. (공백 제외)"
    const passwordCombinedMessage = "영문·숫자·특수문자를 모두 포함해 주세요."
    const passwordNotMatchErrorMessage = "비밀번호가 일치하지 않습니다."

    const [emailError, setEmailError] = useState("")
    const [passwordSizeValid, setPasswordSizeValid] = useState("")
    const [passwordCombineValid, setPasswordCombineValid] = useState("")
    const [passwordConfirmError, setPasswordConfirmError] = useState("")

    const [agreementChecked, setAgreementChecked] = useState(false)

    const agreementErrorMessage = "개인정보 수집·이용에 동의해 주세요."
    const [totalError, setTotalError] = useState("")

    const signupMutation = useSignupMutation()
    const { login, openLogin, isLoginLoading } = useAuth()

    const [isLoading, setIsLoading] = useState(false)

    const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

    const validateEmail = (value) => {
        if (value === registeredEmail) return emailAlreadyRegisteredErrorMessage

        const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
        return ok ? "" : emailFormatErrorMessage
    }

    const validatePassword = (value) => {
        const noSpace = !/\s/.test(value)
        const sizeOk = value.length >= 8 && value.length <= 32 && noSpace
        const combineOk =
            /[a-zA-Z]/.test(value) && /\d/.test(value) && /[^a-zA-Z0-9]/.test(value)

        setPasswordSizeValid(sizeOk ? "ok" : "error")
        setPasswordCombineValid(combineOk ? "ok" : "error")

        return sizeOk && combineOk
    }

    const validatePasswordChange = (value) => {
        const noSpace = !/\s/.test(value)
        const sizeOk = value.length >= 8 && value.length <= 32 && noSpace
        const combineOk =
            /[a-zA-Z]/.test(value) && /\d/.test(value) && /[^a-zA-Z0-9]/.test(value)

        setPasswordSizeValid(sizeOk ? "ok" : "")
        setPasswordCombineValid(combineOk ? "ok" : "")
    }

    const validatePasswordConfirm = (value) => {
        const err = !value || value !== password ? passwordNotMatchErrorMessage : ""
        setPasswordConfirmError(err)
        return err === ""
    }

    const validateAgreement = (checked) => {
        const err = checked ? "" : agreementErrorMessage
        setTotalError(err)
        return err === ""
    }

    const validateAll = () => {
        const eErr = validateEmail(email)
        setEmailError(eErr)

        const pwOk = validatePassword(password)
        const pwcOk = validatePasswordConfirm(passwordConfirm)
        const agreeOk = validateAgreement(agreementChecked)

        // 비밀번호 확인은 비밀번호가 바뀌었을 때도 영향받음
        // (submit 기준으로 한번 더 확정)
        if (!passwordConfirm) {
            setPasswordConfirmError("")
        }

        return eErr === "" && pwOk && pwcOk && agreeOk
    }

    // ===== handlers =====
    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()

        if (!validateAll()) return

        try {
            await signupMutation.mutateAsync({ email, password, passwordConfirm })

            try {
                await login({ email, password })
                navigate("/?welcome=true")
            } catch {
                navigate("/")
                openLogin()
            }
        } catch (error: unknown) {
            const message = error instanceof Error ? error.message : "알 수 없는 오류가 발생했어요."
            if (message === emailAlreadyRegisteredErrorMessage) {
                setRegisteredEmail(email)
                setEmailError(emailAlreadyRegisteredErrorMessage)
                return
            }
            if (message === passwordNotMatchErrorMessage) {
                setPasswordConfirmError(passwordNotMatchErrorMessage)
                return
            }
            setTotalError(message)
        }
    }

    return (
        <div className={styles.wrapper}>
            { (signupMutation.isPending || isLoginLoading || isLoading) && <GlobalSpinner /> }
            <div className={styles.card}>
                <div className={styles.titleWrap}>
                    <img
                        onClick={() => navigate("/")}
                        src="https://i.imgur.com/l8hsGOD.png"
                        alt="moamoa"
                        className={styles.moamoaIcon}
                    />
                    <span className={styles.title}>회원가입</span>
                    <span className={styles.copy}>
            관심 있는 기술 블로그를 구독하고, <br />
            이메일로 새 게시글 소식을 받아보세요.
          </span>
                </div>

                <form className={styles.form} onSubmit={handleSubmit}>
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

                        {/* 비밀번호 */}
                        <div className={styles.input}>
                            <label className={styles.label}>비밀번호</label>
                            <InputText
                                type="password"
                                placeholder="**********"
                                value={password}
                                onChange={(e) => {
                                    const next = e.target.value
                                    setPassword(next)

                                    // 비밀번호 실시간 검증
                                    validatePasswordChange(next)

                                    // 비밀번호가 바뀌면 확인도 같이 재검증
                                    if (passwordConfirm) {
                                        const err =
                                            passwordConfirm !== next ? passwordNotMatchErrorMessage : ""
                                        setPasswordConfirmError(err)
                                    }
                                }}
                                hasError={
                                    passwordCombineValid === "error" || passwordSizeValid === "error"
                                }
                                onBlur={() => validatePassword(password)}
                            />

                            <div>
                                {passwordSizeValid !== "error" ? (
                                    <span
                                        className={`${styles.pwDescription} ${passwordSizeValid === "ok" && styles.success}`}
                                    >✓ {passwordSizeMessage}</span>
                                ) : (
                                    <span className={styles.error}>✕ {passwordSizeMessage}</span>
                                )}
                                <br />
                                {passwordCombineValid !== "error" ? (
                                    <span
                                        className={`${styles.pwDescription} ${passwordCombineValid === "ok" && styles.success}`}
                                    >✓ {passwordCombinedMessage}</span>
                                ) : (
                                    <span className={styles.error}>✕ {passwordCombinedMessage}</span>
                                )}
                            </div>
                        </div>

                        {/* 비밀번호 확인 */}
                        <div className={styles.input}>
                            <label className={styles.label}>비밀번호 확인</label>
                            <InputText
                                type="password"
                                placeholder="**********"
                                value={passwordConfirm}
                                onChange={(e) => setPasswordConfirm(e.target.value)}
                                onBlur={() => validatePasswordConfirm(passwordConfirm)}
                                hasError={passwordConfirmError !== ""}
                            />
                            {passwordConfirmError !== "" && (
                                <span className={styles.error}>✕ {passwordNotMatchErrorMessage}</span>
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
                        회원가입
                    </Button>

                    <div className={styles.socialSection}>
                        <span className={styles.simple}>간편 회원가입</span>
                        <div className={styles.socials}>
                            <button
                                className={styles.social}
                                type="button"
                                onClick={() => {
                                    setIsLoading(true)
                                    window.location.href = `${API_BASE}/oauth2/authorization/google`;
                                }}
                            >
                                <img
                                    className={styles.socialImg}
                                    src="https://i.imgur.com/xWcCM6A.png"
                                    alt="Google 계정으로 계속하기"
                                />
                            </button>
                            <button
                                className={styles.social}
                                type="button"
                                onClick={() => {
                                    setIsLoading(true)
                                    window.location.href = `${API_BASE}/oauth2/authorization/google`;
                                }}
                            >
                                <img
                                    className={styles.socialImg}
                                    src="https://upload.wikimedia.org/wikipedia/commons/thumb/9/95/Font_Awesome_5_brands_github.svg/250px-Font_Awesome_5_brands_github.svg.png"
                                    alt="Github 계정으로 계속하기"
                                />
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    )
}
export default SignupPage
