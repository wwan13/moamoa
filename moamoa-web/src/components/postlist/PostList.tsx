import Pagination from "@mui/material/Pagination"
import styles from "./PostList.module.css"
import PostItem from "../postitem/PostItem"
import CategoryTabs from "../categorytab/CategoryTabs"
import { useNavigate, useSearchParams } from "react-router-dom"
import type { PostCategoryKey, PostSummary } from "../../api/post.api"

const SKELETON_COUNT = 8
const CATEGORY_ITEMS = [
  { id: 0, key: "all", title: "전체" },
  { id: 10, key: "engineering", title: "엔지니어링" },
  { id: 20, key: "product", title: "프로덕트" },
  { id: 30, key: "design", title: "디자인" },
  { id: 40, key: "etc", title: "기타" },
]
const VALID_CATEGORY_IDS = new Set(CATEGORY_ITEMS.map((item) => item.id))
const VALID_CATEGORY_KEYS = new Set(CATEGORY_ITEMS.map((item) => item.key))

type PostListProps = {
  posts?: PostSummary[]
  totalPages?: number
  isBlogDetail?: boolean
  type?: string
  emptyMessage?: string
  isLoading?: boolean
}

const PostList = ({
  posts = [],
  totalPages = 0,
  isBlogDetail = false,
  type = "all",
  emptyMessage = "게시글이 존재하지 않습니다.",
  isLoading = false,
}: PostListProps) => {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get("page") ?? 1)
  const rawCategory = (searchParams.get("category") ?? "all").toLowerCase()
  const selectedCategory = VALID_CATEGORY_KEYS.has(rawCategory)
    ? rawCategory
    : "all"
  const selected =
    CATEGORY_ITEMS.find((it) => it.key === selectedCategory)?.id ?? 0
  const navigate = useNavigate()

  const onChangePage = (nextPage: number) => {
    window.scrollTo({ top: 0, behavior: "smooth" })
    requestAnimationFrame(() => {
      setSearchParams((prev) => {
        const p = new URLSearchParams(prev)
        if (nextPage <= 1) p.delete("page")
        else p.set("page", String(nextPage))
        return p
      })
    })
  }
  const onChangeCategory = (nextCategoryId: number) => {
    if (!VALID_CATEGORY_IDS.has(nextCategoryId)) return
    window.scrollTo({ top: 0, behavior: "smooth" })
    requestAnimationFrame(() => {
      setSearchParams((prev) => {
        const p = new URLSearchParams(prev)
        const nextCategory =
          CATEGORY_ITEMS.find((it) => it.id === nextCategoryId)?.key ?? "all"
        if (nextCategory === "all") p.delete("category")
        else p.set("category", nextCategory)
        p.delete("page")
        return p
      })
    })
  }

  const showEmpty = !isLoading && posts.length === 0

  return (
    <>
      <CategoryTabs
        items={CATEGORY_ITEMS}
        id={selected}
        onChange={onChangeCategory}
        isSubscribing={type === "subscribed"}
        onClickSubscriptions={() => navigate("/subscription")}
        isLoading={false}
      />

      <div className={styles.list}>
        {isLoading ? (
          Array.from({ length: SKELETON_COUNT }).map((_, idx) => (
            <PostItem key={`s-${idx}`} isLoading isBlogDetail={isBlogDetail} />
          ))
        ) : showEmpty ? (
          <div className={styles.empty}>
            <p>{emptyMessage}</p>
          </div>
        ) : (
          posts.map((p) => (
            <PostItem key={p.id} post={p} isBlogDetail={isBlogDetail} />
          ))
        )}
      </div>

      {!isLoading && totalPages > 0 && (
        <div className={styles.paginationWrap}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={(_, value) => onChangePage(value)}
          />
        </div>
      )}
    </>
  )
}

export default PostList
