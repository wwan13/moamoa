import styles from './LoginPage.module.css'
import TextInput from "../../components/ui/TextInput.tsx"
import SubmitButton from "../../components/ui/SubmitButton.tsx"
import {type SyntheticEvent, useState} from "react"
import useAuth from "../../auth/AuthContext.tsx"
import {showGlobalAlert} from "../../api/client.ts";
import GlobalSpinner from "../../components/spinner/GlobalSpinner.tsx";
import {useNavigate} from "react-router-dom";

const LoginPage = () => {
    const { login, isLoginLoading } = useAuth()
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [error, setError] = useState("")
    const navigate = useNavigate()

    const handleSubmit = async (e: SyntheticEvent) => {
        e.preventDefault()
        setError("")
        try {
            await login({ email, password })
            navigate("/")
        } catch {
            showGlobalAlert({
                title: "로그인 실패",
                message: "이메일 혹은 비밀번호를 확인해 주세요.",
            })
        }
    }

    return (
        <div className={styles.wrap}>
            {isLoginLoading && (
                <GlobalSpinner />
            )}
            <section className={styles.content}>
                <div className={styles.logoWrap}>
                    <img
                        className={styles.miniLogo}
                        alt="moamoa admin mini logo"
                        src="https://i.imgur.com/muKhO2B.png"
                    />
                    <img
                        className={styles.fullLogo}
                        alt="moamoa admin full logo"
                        src="https://i.imgur.com/TYLb1ty.png"
                    />
                </div>
                <form className={styles.form} onSubmit={handleSubmit}>
                    <div className={styles.inputWrap}>
                        <TextInput
                            label="이메일"
                            type="text"
                            value={email}
                            onChange={setEmail}
                            isValid={error === ""}
                            errMessage={error}
                        />
                        <TextInput
                            label="비밀번호"
                            type="password"
                            value={password}
                            onChange={setPassword}
                            isValid={error === ""}
                            errMessage={error}
                        />
                    </div>
                    <SubmitButton label="로그인" onClick={() => {}}/>
                </form>
            </section>
        </div>
    )
}

export default LoginPage
