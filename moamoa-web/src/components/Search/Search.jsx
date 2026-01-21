import styles from "./Search.module.css"
import ClearIcon from '@mui/icons-material/Clear';
import {usePostsQuery} from "../../queries/post.queries.js";
import {useEffect, useMemo, useRef, useState} from "react";
import PostItem from "../PostItem/PostItem.jsx";
import {useTechBlogsQuery} from "../../queries/techBlog.queries.js";
import useDebouncedValue from "../../hooks/useDebouncedValue.js";
import TechBlogItem from "../TechBlogItem/TechBlogItem.jsx";
import {DotLottieReact} from "@lottiefiles/dotlottie-react";

export default function Search({open, onClose}) {
    if (!open) return null

    const [query, setQuery] = useState("")
    const inputRef = useRef(null)

    const restoreRef = useRef(null)

    // ✅ 모달 열릴 때 input focus
    useEffect(() => {
        if (!open) return
    }, [open])

    // ✅ 검색어 정리 + 디바운스
    const debouncedQuery = useDebouncedValue(query, 250)
    const searchQuery = useMemo(() => {
        const t = debouncedQuery.trim()
        return t.length > 0 ? t : undefined
    }, [debouncedQuery])

    const isSearching = !!searchQuery

    const initialPostsQuery = usePostsQuery(
        {page: 1, size: 3},
        {enabled: !isSearching}
    )
    const initialPosts = initialPostsQuery.data?.posts

    const searchPostsQuery = usePostsQuery(
        {page: 1, size: 10, query: searchQuery},
        {enabled: isSearching}
    )

    const searchTechBlogsQuery = useTechBlogsQuery(
        {query: searchQuery},
        {enabled: isSearching}
    )

    const posts = isSearching
        ? (searchPostsQuery.data?.posts ?? [])
        : []

    const techBlogs = isSearching
        ? (searchTechBlogsQuery.data?.techBlogs ?? [])
        : []

    const isLoading = isSearching
        ? (searchPostsQuery.isPending || searchTechBlogsQuery.isPending)
        : (initialPostsQuery.isPending)

    const onInputChange = (e) => setQuery(e.target.value)

    return (
        <div className={styles.wrap}>
            <div className={styles.contentWrap}>
                <div className={styles.titleWrap}>
                    <div className={styles.icon}>
                        <img alt="moamoa" src="https://i.imgur.com/nqleqcc.png"/>
                    </div>

                    <button className={styles.cancelButton} onClick={onClose}>
                        <ClearIcon sx={{fontSize: 24, color: "#787878", fontWeight: 800}}/>
                    </button>
                </div>

                <input
                    ref={inputRef}
                    className={styles.searchInput}
                    type="text"
                    placeholder="주제, 블로그 검색"
                    value={query}
                    onChange={onInputChange}
                />

                <div className={styles.content}>
                    {!isSearching && (
                        <div className={styles.posts}>
                            {(initialPosts ?? []).map((p) => (
                                <PostItem
                                    key={p.id}
                                    post={p}
                                    isBlogDetail={false}
                                    isLoading={false}
                                />
                            ))}
                        </div>
                    )}

                    {isSearching && (
                        <>
                            {techBlogs.length > 0 && (
                                <div className={styles.techBlogs}>
                                    {(techBlogs ?? []).map((techBlog) => (
                                        <TechBlogItem key={techBlog.id} techBlog={techBlog} />
                                    ))}
                                </div>
                            )}

                            {(techBlogs.length > 0 && posts.length > 0) && (
                                <div className={styles.divider}></div>
                            )}

                            {posts.length > 0 && (
                                <div className={styles.posts}>
                                    {(posts ?? []).map((p) => (
                                        <PostItem
                                            key={p.id}
                                            post={p}
                                            isBlogDetail={false}
                                            isLoading={false}
                                        />
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                    {((isSearching && techBlogs.length === 0 && posts.length === 0
                        && !searchTechBlogsQuery.isPending && !searchPostsQuery.isPending))&& (
                        <div className={styles.empty}>검색 결과가 없습니다.</div>
                    )}

                    {(isSearching && (searchTechBlogsQuery.isPending || searchPostsQuery.isPending) ||
                        (!isSearching && initialPostsQuery.isPending)) && (
                        <div className={styles.loading}>
                            <DotLottieReact
                                src="https://lottie.host/9934b555-fee3-4544-b9f2-f31df5748272/lAkteKQ8h5.lottie"
                                loop
                                autoplay
                                className={styles.spinner}
                            />
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}