import { PuffLoader } from "react-spinners"
import styles from "./GlobalSpinner.module.css"

type GlobalSpinnerProps = {
    size?: number
    color?: string
}

const GlobalSpinner = ({
    size = 80,
    color = "#0E4BBC",
}: GlobalSpinnerProps) => {
    return (
        <div className={styles.overlay}>
            <PuffLoader size={size} color={color} />
        </div>
    )
}

export default GlobalSpinner
