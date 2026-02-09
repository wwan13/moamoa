import { useEffect, useState } from "react"

export const useCountUp = (target: number, duration = 800): number => {
    const [value, setValue] = useState(0)

    useEffect(() => {
        let start: number | null = null
        let rafId: number | null = null

        const step = (timestamp: number): void => {
            if (start == null) start = timestamp
            const progress = Math.min((timestamp - start) / duration, 1)
            setValue(Math.floor(progress * progress * target))

            if (progress < 1) {
                rafId = requestAnimationFrame(step)
            }
        }

        rafId = requestAnimationFrame(step)
        return () => {
            if (rafId != null) cancelAnimationFrame(rafId)
        }
    }, [target, duration])

    return value
}
