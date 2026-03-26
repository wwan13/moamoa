import { useEffect, useState } from "react"
import styles from "./ScrollTopButton.module.css"

const ScrollTopButton = () => {
    const [isVisible, setIsVisible] = useState(false)

    useEffect(() => {
        const updateVisibility = () => {
            setIsVisible(window.scrollY > 0)
        }

        updateVisibility()
        window.addEventListener("scroll", updateVisibility, { passive: true })

        return () => {
            window.removeEventListener("scroll", updateVisibility)
        }
    }, [])

    const onClick = () => {
        window.scrollTo({ top: 0, behavior: "smooth" })
    }

    return (
        <button
            type="button"
            className={`${styles.scrollTopButton} ${isVisible ? styles.visible : ""}`}
            onClick={onClick}
            aria-label="맨 위로 이동"
        >
            ↑
        </button>
    )
}

export default ScrollTopButton
