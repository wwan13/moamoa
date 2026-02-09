import styles from './Banner.module.css'
import {useEffect, useState} from 'react';
import DrawIcon from '@mui/icons-material/Draw';

const texts = [
    "구독한 블로그의 최신 글을 이메일로 받아보세요!",
]

const Banner = () => {
    const [text, setText] = useState("")
    const [textIndex, setTextIndex] = useState(0)
    const [isDeleting, setIsDeleting] = useState(false)
    const [isPausing, setIsPausing] = useState(false)

    useEffect(() => {
        if (isPausing) return

        const current = texts[textIndex]
        const speed = isDeleting ? 40 : 80

        const timer = setTimeout(() => {
            if (!isDeleting) {
                const next = current.slice(0, text.length + 1)
                setText(next)

                if (next === current) {
                    setIsPausing(true)
                    setTimeout(() => {
                        setIsDeleting(true)
                        setIsPausing(false)
                    }, 1000)
                }
            } else {
                const next = current.slice(0, Math.max(0, text.length - 1))
                setText(next)

                if (next === "") {
                    setIsDeleting(false)
                    setTextIndex((prev) => (prev + 1) % texts.length)
                }
            }
        }, speed)

        return () => clearTimeout(timer)
    }, [text, textIndex, isDeleting, isPausing])

    return (
        <div className={styles.wrap}>

            <div className={styles.content}>
                <p className={styles.main}>moamoa</p>
                <p className={styles.sub}>구독한 블로그의 최신 글을 이메일로 받아보세요!</p>
            </div>

            <div className={styles.ornaments} aria-hidden="true">
                <div className={`${styles.ornament} ${styles.ornamentBlue}`}>
                    <img alt={"banner_blue"} src={"/banner_blue.png"}/>
                </div>

                <div className={`${styles.ornament} ${styles.ornamentRed}`}>
                    user
                </div>

                <div className={`${styles.ornament} ${styles.ornamentTeal}`}>
                    blog
                </div>

                <div className={`${styles.ornament} ${styles.ornamentYellow}`}>
                    <DrawIcon sx={{fontSize: 18}} />
                </div>

                <div className={`${styles.ornament} ${styles.ornamentGreen}`}>
                    &lt;/&gt;
                </div>
            </div>

            <div className={styles.backgroud}>
                <div
                    className={styles.bgRow}
                    style={{ transform: "translateX(-2%)" }}
                >
                    <p className={styles.bgKeyword}>Growth</p>
                    <p className={styles.bgKeyword}>Marketing</p>
                    <p className={styles.bgKeyword}>Backend</p>
                    <p className={styles.bgKeyword}>Interface</p>
                    <p className={styles.bgKeyword}>Layout</p>
                    <p className={styles.bgKeyword}>Architecture</p>
                    <p className={styles.bgKeyword}>Llm</p>
                    <p className={styles.bgKeyword}>LLM</p>
                    <p className={styles.bgKeyword}>LLM</p>
                </div>

                <div
                    className={styles.bgRow}
                    style={{ transform: "translateX(0%)" }}
                >
                    <p className={styles.bgKeyword}>Agent</p>
                    <p className={styles.bgKeyword}>Retention</p>
                    <p className={styles.bgKeyword}>Backend</p>
                    <p className={styles.bgKeyword}>Backlog</p>
                    <p className={styles.bgKeyword}>Agile</p>
                    <p className={styles.bgKeyword}>Cloud</p>
                    <p className={styles.bgKeyword}>Product</p>
                    <p className={styles.bgKeyword}>Layout</p>
                    <p className={styles.bgKeyword}>Layout</p>
                </div>

                <div
                    className={styles.bgRow}
                    style={{ transform: "translateX(-2%)" }}
                >
                    <p className={styles.bgKeyword}>Devops</p>
                    <p className={styles.bgKeyword}>Engineering</p>
                    <p className={styles.bgKeyword}>UI</p>
                    <p className={styles.bgKeyword}>Backlog</p>
                    <p className={styles.bgKeyword}>Test</p>
                    <p className={styles.bgKeyword}>QA</p>
                    <p className={styles.bgKeyword}>Performance</p>
                    <p className={styles.bgKeyword}>Rag</p>
                    <p className={styles.bgKeyword}>Layout</p>
                    <p className={styles.bgKeyword}>Layout</p>
                </div>

                <div
                    className={styles.bgRow}
                    style={{ transform: "translateX(-1%)" }}
                >
                    <p className={styles.bgKeyword}>Font</p>
                    <p className={styles.bgKeyword}>Backlog</p>
                    <p className={styles.bgKeyword}>AI</p>
                    <p className={styles.bgKeyword}>Backlog</p>
                    <p className={styles.bgKeyword}>Layout</p>
                    <p className={styles.bgKeyword}>Architecture</p>
                    <p className={styles.bgKeyword}>Software</p>
                    <p className={styles.bgKeyword}>UX</p>
                    <p className={styles.bgKeyword}>Layout</p>
                </div>
            </div>
        </div>
    )
}
export default Banner
