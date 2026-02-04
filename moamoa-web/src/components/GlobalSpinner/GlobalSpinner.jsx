import { ClipLoader } from "react-spinners"
import styles from "./GlobalSpinner.module.css"
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

export default function GlobalSpinner() {
    return (
        <div className={styles.overlay}>
            {/*<ClipLoader size={40} color="#111827" />*/}
            <DotLottieReact
                src="/spinner.lottie"
                loop
                autoplay
                className={styles.spinner}
            />
        </div>
    )
}