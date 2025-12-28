import { useEffect } from "react"

export default function useModalAccessibility({ open, onClose, panelRef }) {
    useEffect(() => {
        if (!open) return

        const onKeyDown = (e) => {
            if (e.key === "Escape") {
                onClose()
                return
            }

            if (e.key !== "Tab") return

            const focusables = panelRef.current?.querySelectorAll(
                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
            )
            if (!focusables || focusables.length === 0) return

            const first = focusables[0]
            const last = focusables[focusables.length - 1]

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