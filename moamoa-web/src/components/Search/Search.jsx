import styles from "./Search.module.css"
import ClearIcon from '@mui/icons-material/Clear';
import {useInfinitePostsQuery, usePostsQuery} from "../../queries/post.queries.js";
import {useEffect, useMemo, useRef, useState} from "react";
import PostItem from "../PostItem/PostItem.jsx";
import {useTechBlogsQuery} from "../../queries/techBlog.queries.js";
import useDebouncedValue from "../../hooks/useDebouncedValue.js";
import TechBlogItem from "../TechBlogItem/TechBlogItem.jsx";
import {DotLottieReact} from "@lottiefiles/dotlottie-react";
import {useNavigate} from "react-router-dom";

export default function Search({open, onClose}) {
    if (!open) return null

    const [query, setQuery] = useState("")
    const inputRef = useRef(null)
    const scrollRootRef = useRef(null)
    const sentinelRef = useRef(null)

    const navigate = useNavigate()

    useEffect(() => {
        if (!open) return
        inputRef.current?.focus()
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

    const searchPostsQuery = useInfinitePostsQuery(
        {size: 10, query: searchQuery},
        {enabled: isSearching}
    )

    const searchTechBlogsQuery = useTechBlogsQuery(
        {query: searchQuery},
        {enabled: isSearching}
    )

    const posts = isSearching
        ? (searchPostsQuery.data?.pages?.flatMap((page) => page?.posts ?? []) ?? [])
        : []

    const techBlogs = isSearching
        ? (searchTechBlogsQuery.data?.techBlogs ?? [])
        : []

    const onInputChange = (e) => setQuery(e.target.value)
    const isInitialSearchLoading = isSearching
        && (searchTechBlogsQuery.isPending || searchPostsQuery.isPending)

    useEffect(() => {
        if (!isSearching) return
        if (!scrollRootRef.current || !sentinelRef.current) return

        const observer = new IntersectionObserver(
            (entries) => {
                const entry = entries[0]
                if (!entry?.isIntersecting) return
                if (!searchPostsQuery.hasNextPage) return
                if (searchPostsQuery.isFetchingNextPage) return
                searchPostsQuery.fetchNextPage()
            },
            {
                root: scrollRootRef.current,
                rootMargin: "0px 0px 240px 0px",
                threshold: 0.1,
            }
        )

        observer.observe(sentinelRef.current)
        return () => observer.disconnect()
    }, [
        isSearching,
        searchQuery,
        searchPostsQuery.hasNextPage,
        searchPostsQuery.isFetchingNextPage,
        searchPostsQuery.fetchNextPage,
    ])

    return (
        <div className={styles.wrap} ref={scrollRootRef}>
            <div className={styles.contentWrap}>
                <div className={styles.titleWrap}>
                    <div className={styles.icon} onClick={() => {
                        navigate("/")
                        onClose()
                    }}>
                        <img alt="moamoa" src="https://i.imgur.com/CHYokw0.png"/>
                    </div>

                    <button className={styles.cancelButton} onClick={onClose}>
                        <ClearIcon sx={{fontSize: 22, color: "#787878", fontWeight: 800}}/>
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
                                        <TechBlogItem key={techBlog.id} techBlog={techBlog} onItemClick={() => {
                                            onClose()
                                            navigate(`/${techBlog.id}`)
                                        }} />
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

                            <div ref={sentinelRef} className={styles.sentinel} />
                        </>
                    )}

                    {((isSearching && techBlogs.length === 0 && posts.length === 0
                        && !isInitialSearchLoading))&& (
                        <div className={styles.empty}>검색 결과가 없습니다.</div>
                    )}

                    {(isSearching && isInitialSearchLoading ||
                        (!isSearching && initialPostsQuery.isPending)) && (
                        <div className={styles.loading}>
                            <DotLottieReact
                                src="/spinner.lottie"
                                loop
                                autoplay
                                className={styles.spinner}
                            />
                        </div>
                    )}

                    {(isSearching && searchPostsQuery.isFetchingNextPage) && (
                        <div className={styles.nextPageLoading}>
                            <DotLottieReact
                                src="/spinner.lottie"
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
