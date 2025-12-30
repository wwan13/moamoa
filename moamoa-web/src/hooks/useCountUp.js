import { useEffect, useState } from "react"

export function useCountUp(target, duration = 800) {
    const [value, setValue] = useState(0)

    useEffect(() => {
        let start = null
        let rafId = null

        const step = (timestamp) => {
            if (start == null) start = timestamp
            const progress = Math.min((timestamp - start) / duration, 1)
            setValue(Math.floor(progress * progress * target));

            if (progress < 1) {
                rafId = requestAnimationFrame(step)
            }
        }

        rafId = requestAnimationFrame(step)
        return () => rafId && cancelAnimationFrame(rafId)
    }, [target, duration])

    return value
}