import { useCallback, useEffect, useRef, useState } from "react"
import styles from "./LeftSidebar.module.css"
import Subscriptions from "../subscriptions/Subscriptions"
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos"
import type { TechBlogSummary } from "../../api/techBlog.api"
type LeftSidebarProps = {
  subscriptions?: TechBlogSummary[]
  type: "all" | "subscribed" | "bookmarked"
  blogKey?: string | null
  onSelectType: (nextType: "all" | "subscribed" | "bookmarked") => void
  onSelectBlog: (blogId: number | string) => void
  isLoading?: boolean
}
const LeftSidebar = ({
  subscriptions = [],
  type,
  blogKey,
  onSelectType,
  onSelectBlog,
  isLoading = false,
}: LeftSidebarProps) => {
  const isAllActive = type === "all"
  const isBookmarkedActive = type === "bookmarked"
  const isSubscribedTabActive = type === "subscribed" && !blogKey
  const disabled = isLoading
  const wrapRef = useRef<HTMLElement | null>(null)
  const allButtonRef = useRef<HTMLButtonElement | null>(null)
  const subscribedButtonRef = useRef<HTMLButtonElement | null>(null)
  const bookmarkButtonRef = useRef<HTMLButtonElement | null>(null)
  const scrollAreaRef = useRef<HTMLDivElement | null>(null)
  const [hasOverflow, setHasOverflow] = useState(false)
  const [sidebarHeight, setSidebarHeight] = useState<number | null>(null)

  const updateSidebarHeight = useCallback(() => {
    if (window.matchMedia("(max-width: 900px)").matches) {
      setSidebarHeight((prev) => (prev === null ? prev : null))
      return
    }

    const wrapEl = wrapRef.current
    if (!wrapEl) return

    const rectTop = Math.max(0, wrapEl.getBoundingClientRect().top)
    const nextHeight = Math.max(
      0,
      Math.floor(window.innerHeight - rectTop - 16),
    )

    setSidebarHeight((prev) => (prev === nextHeight ? prev : nextHeight))
  }, [])

  const updateOverflow = useCallback(() => {
    const wrapEl = wrapRef.current
    const allButtonEl = allButtonRef.current
    const subscribedButtonEl = subscribedButtonRef.current
    const bookmarkButtonEl = bookmarkButtonRef.current
    const el = scrollAreaRef.current
    if (
      !el ||
      !wrapEl ||
      !allButtonEl ||
      !subscribedButtonEl ||
      !bookmarkButtonEl
    ) {
      setHasOverflow(false)
      return
    }

    const styles = window.getComputedStyle(wrapEl)
    const gap = Number.parseFloat(styles.rowGap || styles.gap || "0") || 0
    const reservedHeight =
      allButtonEl.offsetHeight +
      subscribedButtonEl.offsetHeight +
      bookmarkButtonEl.offsetHeight +
      gap * 3
    const availableHeight = wrapEl.clientHeight - reservedHeight

    setHasOverflow(el.scrollHeight - availableHeight > 1)
  }, [])

  useEffect(() => {
    const el = scrollAreaRef.current
    if (!el) return

    const recalculate = () => {
      updateSidebarHeight()
      requestAnimationFrame(() => {
        updateOverflow()
      })
    }

    recalculate()

    const observer = new ResizeObserver(() => {
      recalculate()
    })

    const wrapEl = wrapRef.current
    const allButtonEl = allButtonRef.current
    const subscribedButtonEl = subscribedButtonRef.current
    const bookmarkButtonEl = bookmarkButtonRef.current

    observer.observe(el)
    if (wrapEl) observer.observe(wrapEl)
    if (allButtonEl) observer.observe(allButtonEl)
    if (subscribedButtonEl) observer.observe(subscribedButtonEl)
    if (bookmarkButtonEl) observer.observe(bookmarkButtonEl)
    const child = el.firstElementChild
    if (child instanceof HTMLElement) observer.observe(child)

    const mutationObserver = new MutationObserver(() => {
      recalculate()
      const nextChild = el.firstElementChild
      if (nextChild instanceof HTMLElement) observer.observe(nextChild)
    })
    mutationObserver.observe(el, { childList: true, subtree: true })

    window.addEventListener("resize", recalculate)
    window.addEventListener("scroll", recalculate, { passive: true })

    return () => {
      observer.disconnect()
      mutationObserver.disconnect()
      window.removeEventListener("resize", recalculate)
      window.removeEventListener("scroll", recalculate)
    }
  }, [updateOverflow, updateSidebarHeight, subscriptions.length, isLoading])

  return (
    <aside
      ref={wrapRef}
      className={`${styles.wrap} ${hasOverflow ? styles.wrapOverflow : ""}`}
      style={
        sidebarHeight !== null ? { height: `${sidebarHeight}px` } : undefined
      }
    >
      <button
        ref={allButtonRef}
        type="button"
        className={`${styles.header} ${isAllActive ? styles.active : ""}`}
        onClick={() => onSelectType("all")}
        disabled={disabled}
      >
        <span className={styles.title}>전체</span>
        <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
      </button>
      <button
        ref={subscribedButtonRef}
        type="button"
        className={`${styles.header} ${isSubscribedTabActive ? styles.active : ""}`}
        onClick={() => onSelectType("subscribed")}
        disabled={disabled}
      >
        <span className={styles.title}>구독</span>
        <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
      </button>
      <div className={styles.scrollArea} ref={scrollAreaRef}>
        <Subscriptions
          items={subscriptions}
          activeBlogKey={blogKey}
          onClickItem={(item) => onSelectBlog(item.id)}
          isLoading={isLoading}
        />
      </div>
      <button
        ref={bookmarkButtonRef}
        type="button"
        className={`${styles.header} ${styles.bookmark} ${isBookmarkedActive ? styles.active : ""}`}
        onClick={() => onSelectType("bookmarked")}
        disabled={disabled}
      >
        <span className={styles.title}>북마크</span>
        <ArrowForwardIosIcon sx={{ fontSize: 14, color: "#252525" }} />
      </button>
    </aside>
  )
}
export default LeftSidebar
