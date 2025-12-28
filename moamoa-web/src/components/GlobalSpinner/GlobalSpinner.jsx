import { ClipLoader } from "react-spinners"
import styles from "./GlobalSpinner.module.css"

export default function GlobalSpinner() {
    return (
        <div className={styles.overlay}>
            <ClipLoader size={40} color="#111827" />
        </div>
    )
}