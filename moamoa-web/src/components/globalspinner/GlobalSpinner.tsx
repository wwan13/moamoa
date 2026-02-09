import styles from "./GlobalSpinner.module.css"
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

const GlobalSpinner = () => {
    return (
        <div className={styles.overlay}>
            <DotLottieReact
                src="/spinner.lottie"
                loop
                autoplay
                className={styles.spinner}
            />
        </div>
    )
}
export default GlobalSpinner
