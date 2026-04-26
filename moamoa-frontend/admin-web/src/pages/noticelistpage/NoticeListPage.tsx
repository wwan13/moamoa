import { useEffect, useState, type ReactNode } from "react"
import Pagination from "@mui/material/Pagination"
import OpenInNewOutlinedIcon from "@mui/icons-material/OpenInNewOutlined"
import styles from "./NoticeListPage.module.css"
import { showGlobalAlert } from "../../api/client"
import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import Button from "../../components/ui/Button.tsx"
import { Dropdown } from "../../components/ui/Dropdown.tsx"
import { Search } from "../../components/ui/Search.tsx"
import { ListHeader } from "../../components/ui/ListHeader.tsx"
import { ListItem } from "../../components/ui/ListItem.tsx"
import {
  useNoticesQuery,
  useUpdateNoticePublishedMutation,
} from "../../queries/notice.queries"
import { useNavigate } from "react-router-dom"

const statusDropdownOptions = [
  { value: "all", label: "전체 상태" },
  { value: "published", label: "공개" },
  { value: "draft", label: "비공개" },
]

const noticeStatusOptions = [
  { value: "published", label: "공개" },
  { value: "draft", label: "비공개" },
]

const formatPublishedAt = (value: string): string => {
  if (!value) return "-"

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date)
}

const NoticeListPage = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState("")
  const [query, setQuery] = useState("")
  const [statusValue, setStatusValue] = useState("all")
  const [page, setPage] = useState(1)
  const size = 20

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  const published =
    statusValue === "all" ? undefined : statusValue === "published"
  const [pendingStatuses, setPendingStatuses] = useState<Record<number, string>>(
    {},
  )
  const [updatingNoticeIds, setUpdatingNoticeIds] = useState<number[]>([])
  const { data, isLoading } = useNoticesQuery({
    page,
    size,
    query: query || undefined,
    published,
  })
  const updateNoticePublishedMutation = useUpdateNoticePublishedMutation()
  const totalPages = Math.max(data?.meta.totalPages ?? 0, 1)

  const handleStatusChange = async (noticeId: number, nextValue: string) => {
    if (updatingNoticeIds.includes(noticeId)) return

    const currentNotice = data?.notices.find((notice) => notice.id === noticeId)
    if (!currentNotice) return

    const prevValue = currentNotice.published ? "published" : "draft"
    if (nextValue === prevValue) return

    setPendingStatuses((prev) => ({ ...prev, [noticeId]: nextValue }))
    setUpdatingNoticeIds((prev) => [...prev, noticeId])

    try {
      await updateNoticePublishedMutation.mutateAsync({
        noticeId,
        published: nextValue === "published",
      })
      setPendingStatuses((prev) => {
        const next = { ...prev }
        delete next[noticeId]
        return next
      })
    } catch {
      setPendingStatuses((prev) => {
        const next = { ...prev }
        delete next[noticeId]
        return next
      })
      await showGlobalAlert("공지사항 상태 변경에 실패했습니다.")
    } finally {
      setUpdatingNoticeIds((prev) => prev.filter((id) => id !== noticeId))
    }
  }

  const rows: ReactNode[][] = (data?.notices ?? []).map((notice) => [
    <div key={`title-${notice.id}`} className={styles.cellTitle}>
      {!!notice.chip && <span className={styles.chip}>{notice.chip}</span>}
      <span className={styles.title}>{notice.title}</span>
    </div>,
    <span key={`publishedAt-${notice.id}`} className={styles.publishedAt}>
      {formatPublishedAt(notice.publishedAt)}
    </span>,
    <div key={`published-${notice.id}`} className={styles.statusControl}>
      <Dropdown
        options={noticeStatusOptions}
        value={pendingStatuses[notice.id] ?? (notice.published ? "published" : "draft")}
        disabled={updatingNoticeIds.includes(notice.id)}
        width={110}
        onChange={(value) => {
          void handleStatusChange(notice.id, value)
        }}
        placeholder="상태"
      />
    </div>,
    <a
      key={`link-${notice.id}`}
      href={`https://moamoa.dev/notice/${notice.id}`}
      target="_blank"
      rel="noreferrer"
      className={styles.link}
      aria-label={`공지사항 ${notice.id} 외부 페이지 열기`}
    >
      <OpenInNewOutlinedIcon sx={{ fontSize: 18 }} />
    </a>,
  ])

  return (
    <div className={styles.wrap}>
      <PageTitle value="공지사항" />

      <section className={styles.filters}>
        <div className={styles.filterLeft}>
          <div className={styles.searchWrap}>
            <Search
              value={searchInput}
              onChange={setSearchInput}
              onSearch={(value) => {
                setPage(1)
                setQuery(value.trim())
              }}
            />
          </div>
        </div>
        <div className={styles.filterRight}>
          <Dropdown
            options={statusDropdownOptions}
            placeholder="상태"
            value={statusValue}
            onChange={(value) => {
              setPage(1)
              setStatusValue(value)
            }}
          />
          <Button type="button" onClick={() => navigate("/notice/create")}>
            공지사항 등록
          </Button>
        </div>
      </section>

      <section className={styles.listWrap}>
        <ListHeader
          templateColumns="minmax(0, 1.9fr) 180px 120px 56px"
          columns={[
            { key: "title", label: "제목" },
            { key: "publishedAt", label: "게시일" },
            { key: "published", label: "상태" },
            { key: "link", label: "" },
          ]}
        />

        {isLoading && <div className={styles.empty}>불러오는 중...</div>}
        {!isLoading && rows.length === 0 && (
          <div className={styles.empty}>등록된 공지사항이 없습니다.</div>
        )}
        {!isLoading &&
          rows.map((cells, index) => (
            <ListItem
              key={data?.notices[index]?.id ?? index}
              cells={cells}
              templateColumns="minmax(0, 1.9fr) 180px 120px 56px"
            />
          ))}
      </section>

      <section className={styles.paginationWrap}>
        <Pagination
          count={totalPages}
          page={page}
          onChange={(_, value) => setPage(value)}
        />
      </section>
    </div>
  )
}

export default NoticeListPage
