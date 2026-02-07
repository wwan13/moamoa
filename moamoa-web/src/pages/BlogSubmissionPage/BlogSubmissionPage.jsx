import styles from "./BlogSubmissionPage.module.css"
import {useEffect, useState} from "react";
import useAuth from "../../auth/AuthContext.jsx";
import {useNavigate} from "react-router-dom";
import InputText from "../../components/ui/InputText.jsx";
import Button from "../../components/ui/Button.jsx";
import {useCreateSubmissionMutation} from "../../queries/submission.queries.js";
import {showToast} from "../../api/client.js";
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";

export default function BlogSubmissionPage() {
    const {isLoggedIn} = useAuth()
    const navigate = useNavigate()

    const [title, setTitle] = useState("")
    const [url, setUrl] = useState("")
    const [agreementChecked, setAgreementChecked] = useState(false)

    const [titleError, setTitleError] = useState("")
    const [urlError, setUrlError] = useState("")

    const createSubmissionsMutation = useCreateSubmissionMutation()

    const validateTitle = () => {
        if (title.length === 0) {
            setTitleError("기술 블로그 이름을 입력해 주세요")
        }
        setTitleError("")
    }

    const validateUrl = () => {
        if (url.length === 0) {
            setUrlError("기술 블로그 URL을 입력해 주세요.")
        }

        try {
            const parsed = new URL(url)
            if (!["http:", "https:"].includes(parsed.protocol)) {
                setUrlError("http 또는 https까지 입력해 주세요")
            }
            return setUrlError("")
        } catch {
            return setUrlError("올바른 URL 형식이 아닙니다.")
        }
    }

    const onSubmit = async (e) => {
        e.preventDefault()

        validateTitle()
        validateUrl()
        const titleOk = titleError === ""
        const urlOk = urlError === ""

        if (!titleOk || !urlOk) return

        await createSubmissionsMutation
            .mutateAsync({ blogTitle : title, blogUrl : url, notificationEnabled : agreementChecked })

        showToast("요청되었습니다.")
        navigate("/")
    }

    useEffect(() => {
        if (!isLoggedIn) {
            navigate("/")
        }
    }, [isLoggedIn]);

    return (
        <div className={styles.wrap}>
            {createSubmissionsMutation.isPending && (
                <GlobalSpinner />
            )}
            <div className={styles.titleWrap}>
                <span className={styles.title}>기술 블로그 요청</span>
                <span className={styles.description}>
                    찾으시는 기술 블로그가 없다면 개발자에게 요청해 주세요. <br/>
                    최대한 빨리 추가해 드릴게요!
                </span>
            </div>

            <form className={styles.form} onSubmit={onSubmit}>
                <div className={styles.inputWrap}>
                    <div className={styles.input}>
                        <label className={styles.label}>기술 블로그 이름</label>
                        <InputText
                            placeholder="모아모아 기술 블로그"
                            value={title}
                            onChange={(e) => {
                                setTitle(e.target.value)
                                if (titleError) validateTitle()
                            }}
                            onBlur={() => validateTitle()}
                            hasError={titleError !== ""}
                        />
                        {titleError !== "" && (
                            <span className={styles.error}>✕ {titleError}</span>
                        )}
                    </div>

                    <div className={styles.input}>
                        <label className={styles.label}>기술 블로그 URL</label>
                        <InputText
                            placeholder="https://moamoa.dev"
                            value={url}
                            onChange={(e) => {
                                setUrl(e.target.value)
                                if (titleError) validateUrl()
                            }}
                            onBlur={() => validateUrl()}
                            hasError={urlError !== ""}
                        />
                        {urlError !== "" && (
                            <span className={styles.error}>✕ {urlError}</span>
                        )}
                    </div>
                </div>

                <div className={styles.agreement}>
                    <input
                        type="checkbox"
                        checked={agreementChecked}
                        onChange={(e) => {
                            setAgreementChecked(e.target.checked)
                            // if (totalError) setTotalError("")
                        }}
                    />
                    <span>블로그가 추가되면 이메일로 알려드릴까요?</span>
                </div>

                <Button type="submit">
                    요청
                </Button>
            </form>
        </div>
    )
}