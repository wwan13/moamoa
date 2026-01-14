import Pagination from "@mui/material/Pagination"
import styles from "./PostList.module.css"
import PostItem from "../PostItem/PostItem.jsx"
import CategoryTabs from "../CategoryTab/CategoryTabs.jsx"
import { useEffect, useState } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"

const SKELETON_COUNT = 8
const SKELETON_DELAY_MS = 500

export default function PostList({
                                     posts = [],
                                     totalPages = 0,
                                     isBlogDetail,
                                     type,
                                     emptyMessage = "게시글이 존재하지 않습니다.",
                                     isLoading = false, // query 로딩
                                 }) {
    const [searchParams, setSearchParams] = useSearchParams()
    const page = Number(searchParams.get("page") ?? 1)

    const [categories, setCategories] = useState([])
    const [selected, setSelected] = useState(0)
    const navigate = useNavigate()

    // ✅ 스켈레톤 표시 여부 (딜레이 적용)
    const [showSkeleton, setShowSkeleton] = useState(false)

    useEffect(() => {
        let timer = null

        if (isLoading) {
            timer = setTimeout(() => {
                setShowSkeleton(true)
            }, SKELETON_DELAY_MS)
        } else {
            setShowSkeleton(false)
        }

        return () => {
            if (timer) clearTimeout(timer)
        }
    }, [isLoading])

    useEffect(() => {
        setCategories([{ id: 0, key: "ALL", title: "전체" }])
        setSelected(0)
    }, [type])

    const onChangePage = (nextPage) => {
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
                items={categories}
                id={selected}
                onChange={(next) => setSelected(next)}
                isSubscribing={type === "subscribed"}
                onClickSubscriptions={() => navigate("/subscription")}
                isLoading={showSkeleton}
            />

            <div className={styles.list}>
                {showSkeleton ? (
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