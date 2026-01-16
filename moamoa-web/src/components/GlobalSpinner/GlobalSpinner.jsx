import { ClipLoader } from "react-spinners"
import styles from "./GlobalSpinner.module.css"
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

export default function GlobalSpinner() {
    return (
        <div className={styles.overlay}>
            {/*<ClipLoader size={40} color="#111827" />*/}
            <DotLottieReact
                src="https://lottie.host/9934b555-fee3-4544-b9f2-f31df5748272/lAkteKQ8h5.lottie"
                loop
                autoplay
                className={styles.spinner}
            />
        </div>
    )
}