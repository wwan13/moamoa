import Pagination from "@mui/material/Pagination"
import styles from "./PostList.module.css"
import PostItem from "../postitem/PostItem"
import CategoryTabs from "../categorytab/CategoryTabs"
import { useNavigate, useSearchParams } from "react-router-dom"
import type { PostSummary } from "../../api/post.api"

const SKELETON_COUNT = 8
const CATEGORY_ITEMS = [{ id: 0, key: "ALL", title: "전체" }]

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

    const selected = 0
    const navigate = useNavigate()

    const onChangePage = (nextPage: number) => {
        window.scrollTo({ top: 0, behavior: "smooth" })
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)
            p.set("page", String(nextPage))
            return p
        })
    }

    const showEmpty = !isLoading && posts.length === 0

    return (
        <>
            <CategoryTabs
                items={CATEGORY_ITEMS}
                id={selected}
                onChange={() => {}}
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
