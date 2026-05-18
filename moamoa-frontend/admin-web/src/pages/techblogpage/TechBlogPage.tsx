import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import { useEffect, useState, type ReactNode } from "react"
import styles from "./TechBlogPage.module.css"
import { Search } from "../../components/ui/Search.tsx"
import { ListHeader } from "../../components/ui/ListHeader.tsx"
import { ListItem } from "../../components/ui/ListItem.tsx"
import Button from "../../components/ui/Button.tsx"
import { showGlobalAlert, showGlobalConfirm, showToast } from "../../api/client"
import {
  useCollectTechBlogPostsMutation,
  useDeleteTechBlogPostsMutation,
  useTechBlogsQuery,
} from "../../queries/techblog.queries"

const TechBlogPage = () => {
  const [searchInput, setSearchInput] = useState("")
  const [collectingTechBlogId, setCollectingTechBlogId] = useState<number | null>(
    null,
  )
  const [collectStartedAt, setCollectStartedAt] = useState<number | null>(null)
  const [elapsedSeconds, setElapsedSeconds] = useState(0)

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  useEffect(() => {
    if (collectingTechBlogId === null || collectStartedAt === null) {
      setElapsedSeconds(0)
      return
    }

    setElapsedSeconds(Math.floor((Date.now() - collectStartedAt) / 1000))
    const timerId = window.setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - collectStartedAt) / 1000))
    }, 1000)

    return () => window.clearInterval(timerId)
  }, [collectStartedAt, collectingTechBlogId])

  const { data, isLoading } = useTechBlogsQuery()
  const collectTechBlogPostsMutation = useCollectTechBlogPostsMutation()
  const deleteTechBlogPostsMutation = useDeleteTechBlogPostsMutation()
  const normalizedQuery = searchInput.trim().toLowerCase()
  const techBlogs = (data ?? []).filter((techBlog) => {
    if (!normalizedQuery) return true

    return [techBlog.title, techBlog.key, techBlog.blogUrl].some((value) =>
      value.toLowerCase().includes(normalizedQuery),
    )
  })

  const rows: ReactNode[][] = techBlogs.map((techBlog) => [
    <div key={`blog-${techBlog.id}`} className={styles.blogCell}>
      <img
        src={techBlog.icon}
        alt={`${techBlog.title} 아이콘`}
        className={styles.blogIcon}
      />
      <div className={styles.blogText}>
        <span className={styles.blogTitle}>{techBlog.title}</span>
        <span className={styles.blogKey}>{techBlog.key}</span>
      </div>
    </div>,
    <span key={`blogUrl-${techBlog.id}`} className={styles.blogUrl}>
      <a
        href={techBlog.blogUrl}
        target="_blank"
        rel="noreferrer"
        className={styles.urlLink}
      >
        {techBlog.blogUrl}
      </a>
    </span>,
    <span key={`postCount-${techBlog.id}`} className={styles.metric}>
      {techBlog.postCount.toLocaleString("ko-KR")}
    </span>,
    <span key={`subscriptionCount-${techBlog.id}`} className={styles.metric}>
      {techBlog.subscriptionCount.toLocaleString("ko-KR")}
    </span>,
    <div key={`collect-${techBlog.id}`} className={styles.buttonCell}>
      <Button
        type="button"
        disabled={
          collectingTechBlogId === techBlog.id ||
          deleteTechBlogPostsMutation.isPending
        }
        onClick={() => {
          void handleCollectPosts(techBlog.id, techBlog.title)
        }}
      >
        {collectingTechBlogId === techBlog.id
          ? formatElapsedSeconds(elapsedSeconds)
          : "게시글 수집"}
      </Button>
      <Button
        type="button"
        variant="outline"
        className={styles.deleteButton}
        disabled={
          collectingTechBlogId !== null || deleteTechBlogPostsMutation.isPending
        }
        onClick={() => {
          void handleDeletePosts(techBlog.id, techBlog.title, techBlog.postCount)
        }}
      >
        {deleteTechBlogPostsMutation.isPending ? "삭제 중..." : "전체 삭제"}
      </Button>
    </div>,
  ])

  const handleCollectPosts = async (techBlogId: number, techBlogTitle: string) => {
    if (collectingTechBlogId !== null) return

    setCollectingTechBlogId(techBlogId)
    setCollectStartedAt(Date.now())

    try {
      const result = await collectTechBlogPostsMutation.mutateAsync({ techBlogId })
      await showGlobalAlert({
        title: "게시글 수집 완료",
        message: `${result.techBlog.title} 게시글 수집이 완료되었습니다.\n신규 ${result.newPostCount}건, 업데이트 ${result.updatedPostCount}건`,
      })
    } catch (error) {
      const message =
        error instanceof DOMException && error.name === "AbortError"
          ? `${techBlogTitle} 게시글 수집 시간이 너무 오래 걸려 중단되었습니다. 잠시 후 다시 시도해 주세요.`
          : `${techBlogTitle} 게시글 수집에 실패했습니다.`

      await showGlobalAlert(message)
    } finally {
      setCollectingTechBlogId(null)
      setCollectStartedAt(null)
    }
  }

  const handleDeletePosts = async (
    techBlogId: number,
    techBlogTitle: string,
    postCount: number,
  ) => {
    const confirmed = await showGlobalConfirm({
      title: "게시글 전체 삭제",
      message: `${techBlogTitle} 게시글 ${postCount.toLocaleString("ko-KR")}건을 모두 삭제하시겠어요?`,
      confirmText: "전체 삭제",
    })
    if (!confirmed) return

    try {
      const result = await deleteTechBlogPostsMutation.mutateAsync({ techBlogId })
      showToast(
        `${result.techBlog.title} 게시글 ${result.deletedPostCount.toLocaleString("ko-KR")}건을 삭제했습니다.`,
        { type: "success" },
      )
    } catch {
      await showGlobalAlert(`${techBlogTitle} 게시글 전체 삭제에 실패했습니다.`)
    }
  }

  return (
    <div className={styles.wrap}>
      <PageTitle value="기술블로그" />

      <section className={styles.filters}>
        <div className={styles.searchWrap}>
          <Search
            value={searchInput}
            onChange={setSearchInput}
            placeholder="기술 블로그 이름, key, URL 검색"
          />
        </div>
        <div className={styles.count}>
          총 {techBlogs.length.toLocaleString("ko-KR")}개
        </div>
      </section>

      <section className={styles.listWrap}>
        <ListHeader
          templateColumns="minmax(0, 1.2fr) minmax(0, 1.7fr) 120px 120px 240px"
          columns={[
            { key: "title", label: "블로그" },
            { key: "blogUrl", label: "URL" },
            { key: "postCount", label: "게시글 수", align: "center" },
            { key: "subscriptionCount", label: "구독자 수", align: "center" },
            { key: "collect", label: "", align: "center" },
          ]}
        />

        {isLoading && <div className={styles.empty}>불러오는 중...</div>}
        {!isLoading && rows.length === 0 && (
          <div className={styles.empty}>등록된 기술 블로그가 없습니다.</div>
        )}
        {!isLoading &&
          rows.map((cells, index) => (
            <ListItem
              key={techBlogs[index]?.id ?? index}
              cells={cells}
              templateColumns="minmax(0, 1.2fr) minmax(0, 1.7fr) 120px 120px 240px"
            />
          ))}
      </section>
    </div>
  )
}

const formatElapsedSeconds = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainSeconds = seconds % 60

  return `${String(minutes).padStart(2, "0")}:${String(remainSeconds).padStart(2, "0")}`
}

export default TechBlogPage
