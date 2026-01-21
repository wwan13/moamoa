import styles from "./PasswordChangePage.module.css"
import {useEffect, useState} from "react";
import useAuth from "../../auth/AuthContext.jsx";
import {useNavigate} from "react-router-dom";
import InputText from "../../components/ui/InputText.jsx";
import Button from "../../components/ui/Button.jsx";
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";
import {useChangePasswordMutation} from "../../queries/member.queries.js";
import {showGlobalAlert, showGlobalConfirm, showToast} from "../../api/client.js";

export default function PasswordChangePage() {
    const {isLoggedIn} = useAuth()
    const navigate = useNavigate()

    const [oldPassword, setOldPassword] = useState("")
    const [newPassword, setNewPassword] = useState("")
    const [passwordConfirm, setPasswordConfirm] = useState("")

    const [passwordSizeValid, setPasswordSizeValid] = useState("")
    const [passwordCombineValid, setPasswordCombineValid] = useState("")
    const [extraPasswordError, setExtraPasswordError] = useState("")

    const passwordSizeMessage = "8자리 이상 32자리 이하로 구성해 주세요. (공백 제외)"
    const passwordCombinedMessage = "영문·숫자·특수문자를 모두 포함해 주세요."
    const passwordNotMatchErrorMessage = "비밀번호가 일치하지 않습니다."

    const [oldPasswordError, setOldPasswordError] = useState("")
    const [passwordConfirmError, setPasswordConfirmError] = useState("")

    const changePasswordMutation = useChangePasswordMutation()

    useEffect(() => {
        if (!isLoggedIn) {
            navigate("/")
        }

        setOldPasswordError("")
        setPasswordSizeValid("")
        setPasswordCombineValid("")
        setExtraPasswordError("")
        setPasswordConfirmError("")
    }, [isLoggedIn]);

    const validateOldPassword = () => {
        if (oldPassword === "") {
            setOldPasswordError("기존 비밀번호를 입력해 주세요")
        }
    }

    const validateNewPassword = (value) => {
        const noSpace = !/\s/.test(value)
        const sizeOk = value.length >= 8 && value.length <= 32 && noSpace
        const combineOk =
            /[a-zA-Z]/.test(value) && /\d/.test(value) && /[^a-zA-Z0-9]/.test(value)
        const same = oldPassword !== "" && value === oldPassword

        setPasswordSizeValid(sizeOk ? "ok" : "error")
        setPasswordCombineValid(combineOk ? "ok" : "error")
        setExtraPasswordError(same ?"같은 비밀번호는 사용할 수 없습니다." : "")

        return sizeOk && combineOk && !same
    }

    const validatePasswordConfirm = (value) => {
        const err = !value || value !== newPassword ? passwordNotMatchErrorMessage : ""
        setPasswordConfirmError(err)
        return err === ""
    }

    const validatePasswordChange = (value) => {
        const noSpace = !/\s/.test(value)
        const sizeOk = value.length >= 8 && value.length <= 32 && noSpace
        const combineOk =
            /[a-zA-Z]/.test(value) && /\d/.test(value) && /[^a-zA-Z0-9]/.test(value)
        const same = oldPassword !== "" && value === oldPassword

        setPasswordSizeValid(sizeOk ? "ok" : "")
        setPasswordCombineValid(combineOk ? "ok" : "")
        setExtraPasswordError(same ?"같은 비밀번호는 사용할 수 없습니다." : "")
    }

    const onSubmit = async (e) => {
        e.preventDefault()

        const oldPasswordOk = oldPasswordError === ""
        const newPasswordOk = validateNewPassword(newPassword)
        const passwordConfirmOk = validatePasswordConfirm(passwordConfirm)

        if (!oldPasswordOk || !newPasswordOk || !passwordConfirmOk) {
            return
        }

        await showGlobalConfirm({
            message: "비밀번호를 변경하시겠습니까?"
        })

        try {
            await changePasswordMutation.mutateAsync({ oldPassword, newPassword, passwordConfirm })

            showToast("변경되었습니다.")
            navigate("/my")
        } catch (e) {
            if (e.message === "비밀번호가 일치하지 않습니다.") {
                setPasswordConfirmError(e.message)
                return
            } else if (e.message === "기존 비밀번호가 일치하지 않습니다.") {
                setOldPasswordError(e.message)
                return
            } else if (e.message === "같은 비밀번호는 사용할 수 없습니다.") {
                setExtraPasswordError(e.message)
                return
            }

            showGlobalAlert(e.message)
            navigate("/my")
        }
    }

    return (
        <div className={styles.wrap}>
            {changePasswordMutation.isPending && (
                <GlobalSpinner />
            )}
            <div className={styles.titleWrap}>
                <span className={styles.title}>비밀번호 변경</span>
            </div>

            <form className={styles.form} onSubmit={onSubmit}>
                <div className={styles.inputWrap}>
                    <div className={styles.input}>
                        <label className={styles.label}>기존 비밀번호</label>
                        <InputText
                            type="password"
                            placeholder="**********"
                            value={oldPassword}
                            onChange={(e) => {
                                setOldPassword(e.target.value)
                                if (oldPasswordError) validateOldPassword()
                            }}
                            onBlur={() => validateOldPassword()}
                            hasError={oldPasswordError !== ""}
                        />
                        {oldPasswordError !== "" && (
                            <span className={styles.error}>✕ {oldPasswordError}</span>
                        )}
                    </div>

                    <div className={styles.input}>
                        <label className={styles.label}>새 비밀번호</label>
                        <InputText
                            type="password"
                            placeholder="**********"
                            value={newPassword}
                            onChange={(e) => {
                                const next = e.target.value
                                setNewPassword(next)

                                // 비밀번호 실시간 검증
                                validatePasswordChange(next)

                                // 비밀번호가 바뀌면 확인도 같이 재검증
                                if (passwordConfirm) {
                                    const err = passwordConfirm !== next ? passwordNotMatchErrorMessage : ""
                                    setPasswordConfirmError(err)
                                }
                            }}
                            onBlur={() => validateNewPassword(newPassword)}
                            hasError={passwordCombineValid === "error" || passwordSizeValid === "error"}
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
                            {extraPasswordError !== "" && (
                                <>
                                    <br />
                                    <span className={styles.error}>✕ {extraPasswordError}</span>
                                </>
                            )}
                        </div>
                    </div>

                    <div className={styles.input}>
                        <label className={styles.label}>비밀번호 확인</label>
                        <InputText
                            type="password"
                            placeholder="**********"
                            value={passwordConfirm}
                            onChange={(e) => {
                                setPasswordConfirm(e.target.value)
                                if (passwordConfirmError) validatePasswordConfirm(passwordConfirm)
                            }}
                            onBlur={() => validatePasswordConfirm(passwordConfirm)}
                            hasError={passwordConfirmError !== ""}
                        />
                        {passwordConfirmError !== "" && (
                            <span className={styles.error}>✕ {passwordConfirmError}</span>
                        )}
                    </div>
                </div>

                <Button type="submit">
                    변경
                </Button>
            </form>
        </div>
    )
}