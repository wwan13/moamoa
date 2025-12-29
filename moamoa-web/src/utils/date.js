function pad2(n) {
    return String(n).padStart(2, "0")
}

function isValidDate(d) {
    return d instanceof Date && !Number.isNaN(d.getTime())
}

/**
 * ✅ [2025,12,2,11,6] 같은 배열 또는
 * ✅ "2025-12-26T00:00:00" / "2025-12-26T00:00:00Z" / "...+09:00" 같은 문자열 -> Date
 *
 * 주의:
 * - "2025-12-26T00:00:00" (타임존 없음)은 JS가 로컬타임으로 해석하는 경우가 많음.
 * - "Z" 또는 "+09:00"이 있으면 해당 타임존 기준으로 파싱됨.
 */
export function toDate(input) {
    // 배열
    if (Array.isArray(input)) {
        if (input.length < 3) return null
        const [y, m, d, hh = 0, mm = 0] = input
        const dt = new Date(y, (m ?? 1) - 1, d ?? 1, hh ?? 0, mm ?? 0, 0, 0)
        return isValidDate(dt) ? dt : null
    }

    // 문자열(ISO)
    if (typeof input === "string") {
        const s = input.trim()
        if (!s) return null

        // 표준 ISO는 new Date로 처리 가능
        // (YYYY-MM-DDTHH:mm:ss[.SSS][Z|±HH:mm])
        let dt = new Date(s)
        if (isValidDate(dt)) return dt

        // 혹시 "2025-12-26" 같은 날짜만 오는 경우도 대응
        const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
        if (m) {
            const y = Number(m[1]), mo = Number(m[2]), d = Number(m[3])
            dt = new Date(y, mo - 1, d, 0, 0, 0, 0)
            return isValidDate(dt) ? dt : null
        }

        return null
    }

    // Date 객체
    if (input instanceof Date) {
        return isValidDate(input) ? input : null
    }

    return null
}

/**
 * ✅ 2025.12.02 (시간 있으면 2025.12.02 11:06)
 * - 배열: 길이 5 이상이면 시간 표시
 * - 문자열/Date: withTime=true면 시간 표시(기본)
 */
export function formatDate(input, { withTime = true } = {}) {
    const dt = toDate(input)
    if (!dt) return ""

    const y = dt.getFullYear()
    const m = pad2(dt.getMonth() + 1)
    const d = pad2(dt.getDate())

    const base = `${y}.${m}.${d}`
    if (!withTime) return base

    const showTime =
        Array.isArray(input) ? input.length >= 5 : true // 문자열/Date는 기본 시간 표시

    if (!showTime) return base

    const hh = pad2(dt.getHours())
    const mm = pad2(dt.getMinutes())
    return `${base} ${hh}:${mm}`
}

/**
 * ✅ 오늘이면 "오늘", 7일 이하면 "n일 전", 그 외는 YYYY.MM.DD
 */
export function formatRelativeDate(input) {
    const target = toDate(input)
    if (!target) return ""

    const today = new Date()

    const targetDate = new Date(target.getFullYear(), target.getMonth(), target.getDate())
    const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())

    const diffMs = todayDate - targetDate
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

    if (diffDays === 0) return "오늘"
    if (diffDays > 0 && diffDays <= 7) return `${diffDays}일 전`

    return formatDate(input, { withTime: false })
}