import { useEffect } from "react"
import type { RefObject } from "react"

type UseModalAccessibilityParams = {
    open: boolean
    onClose: () => void
    panelRef: RefObject<HTMLElement | null>
}

const useModalAccessibility = ({ open, onClose, panelRef }: UseModalAccessibilityParams): void => {
    useEffect(() => {
        if (!open) return

        const onKeyDown = (e: KeyboardEvent): void => {
            if (e.key === "Escape") {
                onClose()
                return
            }

            if (e.key !== "Tab") return

            const focusables = panelRef.current?.querySelectorAll(
                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
            )
            if (!focusables || focusables.length === 0) return

            const first = focusables[0] as HTMLElement
            const last = focusables[focusables.length - 1] as HTMLElement

            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault()
                last.focus()
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault()
                first.focus()
            }
        }

        document.addEventListener("keydown", onKeyDown)
        return () => document.removeEventListener("keydown", onKeyDown)
    }, [open, onClose, panelRef])
}
export default useModalAccessibility
