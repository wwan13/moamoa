import { useState } from "react"
import styles from "./InputText.module.css"
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined"
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined"

export default function InputText({ type, hasError=false, ...props }) {
    const isPassword = type === "password"
    const [show, setShow] = useState(false)

    return (
        <div className={styles.wrap}>
            <input
                className={`${styles.input} ${hasError ? styles.error : ""}`}
                type={isPassword && show ? "text" : type}
                {...props}
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
    )
}