import styles from './TextInput.module.css'
import {useState} from "react";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined"
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined"

type TextInputProps = {
    label: string
    type: string
    value: string
    onClick: () => void
    isValid: boolean,
    errMessage: string
}

const TextInput = ({
                       label,
                       type,
                       value,
                       onClick,
                       isValid,
                       errMessage,
}: TextInputProps) => {
    const isPassword = type === "password"
    const [show, setShow] = useState(false)

    return (
        <div className={styles.wrap}>
            <label className={styles.label}>{label}</label>
            <div className={styles.inputWrap}>
                <input
                    className={styles.input}
                    type={type}
                    onClick={onClick}
                    value={value}
                />
                {isPassword && (
                    <button
                        type="button"
                        className={styles.eye}
                        onClick={() => setShow((v) => !v)}
                        tabIndex={-1}
                        aria-label={show ? "비밀번호 숨기기" : "비밀번호 보기"}
                    >
                        {show ? (
                            <VisibilityOffOutlinedIcon fontSize="small" />
                        ) : (
                            <VisibilityOutlinedIcon fontSize="small" />
                        )}
                    </button>
                )}
            </div>
            {!isValid && <span>{errMessage}</span>}
        </div>
    )
}

export default TextInput