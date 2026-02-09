import { useMemo } from "react"
import { useSearchParams } from "react-router-dom"

type UsePagingQueryResult = {
    page: number
    size: number
    setPage: (nextPage: number) => void
}

export const usePagingQuery = (): UsePagingQueryResult => {
    const [searchParams, setSearchParams] = useSearchParams()

    const page = useMemo(() => {
        const p = Number(searchParams.get("page") ?? 1)
        return Number.isFinite(p) && p >= 1 ? p : 1
    }, [searchParams])

    const size = 20 // ✅ 고정

    const setPage = (nextPage: number): void => {
        const p = Math.max(1, Number(nextPage) || 1)
        const next = new URLSearchParams(searchParams)

        if (p === 1) {
            next.delete("page")   // ✅ page=1이면 제거
        } else {
            next.set("page", String(p))
        }

        // size는 기본값이므로 URL에 안 넣음
        setSearchParams(next, { replace: true })
    }

    return { page, size, setPage }
}
