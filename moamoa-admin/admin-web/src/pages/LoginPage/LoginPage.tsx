import styles from './LoginPage.module.css'
import TextInput from "../../components/ui/TextInput.tsx";
import SubmitButton from "../../components/ui/SubmitButton.tsx";

const LoginPage = () => {
    return (
        <div className={styles.wrap}>
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
                <form className={styles.form}>
                    <div className={styles.inputWrap}>
                        <TextInput
                            label="이메일"
                            type="text"
                            value=""
                            onClick={() => console.log("")}
                            isValid={false}
                            errMessage=""
                        />
                        <TextInput
                            label="비밀번호"
                            type="password"
                            value=""
                            onClick={() => console.log("")}
                            isValid={false}
                            errMessage=""
                        />
                    </div>
                    <SubmitButton label="로그인" onClick={() => {
                    }}/>
                </form>
            </section>
        </div>
    )
}

export default LoginPage