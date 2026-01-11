import Pagination from "@mui/material/Pagination"
import styles from "./PostList.module.css"
import PostItem from "../PostItem/PostItem.jsx"
import CategoryTabs from "../CategoryTab/CategoryTabs.jsx"
import { useEffect, useState } from "react"
import {useNavigate, useSearchParams} from "react-router-dom"

export default function PostList({
                                     posts,
                                     totalPages,
                                     isBlogDetail,
                                     type,
                                     emptyMessage = "게시글이 존재하지 않습니다." }) {
    const [searchParams, setSearchParams] = useSearchParams()

    const page = Number(searchParams.get("page") ?? 1)

    const [categories, setCategories] = useState([])
    const [selected, setSelected] = useState(0)
    const navigate = useNavigate()

    useEffect(() => {
        setCategories([{ id: 0, key: "ALL", title: "전체" }])
    }, [type])

    const onChangePage = (nextPage) => {
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)
            p.set("page", nextPage)
            return p
        })
    }

    return (
        <>
            <CategoryTabs
                items={categories}
                id={selected}
                onChange={(next) => setSelected(next)}
                isSubscribing={type==="subscribed"}
                onClickSubscriptions={() => navigate("/subscription")}
            />

            <div className={styles.list}>
                {posts.length === 0 ? (
                    <div className={styles.empty}>
                        <p>{emptyMessage}</p>
                    </div>
                ) : (
                    posts.map((p) => <PostItem key={p.id} post={p} isBlogDetail={isBlogDetail} />)
                )}
            </div>

            {totalPages > 0 && (
                <div className={styles.paginationWrap}>
                    <Pagination count={totalPages} page={page} onChange={(_, value) => onChangePage(value)} />
                </div>
            )}
        </>
    )
}