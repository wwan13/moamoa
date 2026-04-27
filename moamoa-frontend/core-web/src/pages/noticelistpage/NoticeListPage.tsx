import styles from "./NoticeListPage.module.css"
import Pagination from "@mui/material/Pagination"
import {
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type KeyboardEvent,
} from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { formatDate } from "../../utils/date"
import { useNoticesQuery } from "../../queries/notice.queries"
import SearchBar from "./SearchBar"

const PAGE_SIZE = 10
const SKELETON_COUNT = 8

const NoticeListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()

  const query = searchParams.get("query") ?? ""
  const [inputQuery, setInputQuery] = useState(query)
  const page = useMemo(() => {
    const rawPage = Number(searchParams.get("page") ?? 1)
    return Number.isFinite(rawPage) && rawPage >= 1 ? rawPage : 1
  }, [searchParams])
  const noticesQuery = useNoticesQuery({ page, size: PAGE_SIZE, query })
  const navigate = useNavigate()

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "smooth" })
  }, [page, query])

  useEffect(() => {
    setInputQuery(query)
  }, [query])

  const notices = noticesQuery.data?.notices ?? []
  const totalPages = noticesQuery.data?.meta?.totalPages ?? 0
  const hasInputQuery = inputQuery.trim().length > 0

  useEffect(() => {
    if (!noticesQuery.data) return
    if (totalPages <= 0) return
    if (page > totalPages) {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)

          if (totalPages === 1) {
            next.delete("page")
          } else {
            next.set("page", String(totalPages))
          }

          return next
        },
        { replace: true },
      )
    }
  }, [page, totalPages, setSearchParams, noticesQuery.data])

  const onSearchChange = (event: ChangeEvent<HTMLInputElement>) => {
    setInputQuery(event.target.value)
  }

  const onSearch = () => {
    const nextQuery = inputQuery.trim()

    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)

        if (nextQuery.trim()) {
          next.set("query", nextQuery)
        } else {
          next.delete("query")
        }

        next.delete("page")
        return next
      },
      { replace: true },
    )
  }

  const onClearSearch = () => {
    setInputQuery("")
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        next.delete("query")
        next.delete("page")
        return next
      },
      { replace: true },
    )
  }

  const onSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key !== "Enter") return
    onSearch()
  }

  const onPageChange = (_: React.ChangeEvent<unknown>, nextPage: number) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)

        if (nextPage <= 1) {
          next.delete("page")
        } else {
          next.set("page", String(nextPage))
        }

        return next
      },
      { replace: true },
    )
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.titleWrap}>
        <span className={styles.title}>공지사항</span>
        <span className={styles.titleDescription}>
          모아모아의 서비스 개선 및 서비스 점검에 대한 소식을 전해드립니다
        </span>
      </div>

      <div className={styles.contentWrap}>
        <SearchBar
          query={inputQuery}
          hasInputQuery={hasInputQuery}
          onChange={onSearchChange}
          onKeyDown={onSearchKeyDown}
          onSearch={onSearch}
          onClear={onClearSearch}
        />

        <div className={styles.noticeList}>
          {noticesQuery.isPending &&
            Array.from({ length: SKELETON_COUNT }).map((_, index) => (
              <div
                key={`notice-skeleton-${index}`}
                className={`${styles.noticeItem} ${styles.skeletonItem}`}
                aria-busy="true"
              >
                <div className={styles.noticeItemTop}>
                  <div
                    className={`${styles.noticeChip} ${styles.skeleton} ${styles.skeletonChip}`}
                  />
                  <div
                    className={`${styles.skeleton} ${styles.skeletonTitle}`}
                  />
                </div>
                <div className={`${styles.skeleton} ${styles.skeletonDate}`} />
              </div>
            ))}

          {!noticesQuery.isPending &&
            !noticesQuery.isError &&
            notices.length === 0 && (
              <div className={styles.statusMessage}>
                {query.trim()
                  ? "검색 결과가 없습니다."
                  : "등록된 공지사항이 없습니다."}
              </div>
            )}

          {!noticesQuery.isPending &&
            notices.map((notice) => (
              <div
                key={notice.id}
                className={styles.noticeItem}
                onClick={() => navigate(`/notice/${notice.id}`)}
              >
                <div className={styles.noticeItemTop}>
                  {notice.chip.length > 0 && (
                    <div className={styles.noticeChip}>{notice.chip}</div>
                  )}
                  <span className={styles.noticeTitle}>{notice.title}</span>
                </div>
                <span className={styles.publishedAt}>
                  {formatDate(notice.publishedAt, { withTime: false })}
                </span>
              </div>
            ))}
        </div>
      </div>

      {totalPages > 0 && (
        <Pagination
          className={styles.pagination}
          count={Math.max(1, totalPages)}
          page={page}
          onChange={onPageChange}
        />
      )}
    </div>
  )
}

export default NoticeListPage
