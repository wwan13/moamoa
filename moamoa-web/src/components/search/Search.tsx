import styles from "./Search.module.css"
import ClearIcon from "@mui/icons-material/Clear"
import { useInfinitePostsQuery, usePostsQuery } from "../../queries/post.queries"
import { useEffect, useMemo, useRef, useState, type ChangeEvent } from "react"
import PostItem from "../postitem/PostItem"
import { useTechBlogsQuery } from "../../queries/techBlog.queries"
import useDebouncedValue from "../../hooks/useDebouncedValue"
import TechBlogItem from "../techblogitem/TechBlogItem"
import { DotLottieReact } from "@lottiefiles/dotlottie-react"
import { useNavigate } from "react-router-dom"

type SearchProps = {
    open: boolean
    onClose: () => void
}

const Search = ({ open, onClose }: SearchProps) => {
    const [query, setQuery] = useState("")
    const inputRef = useRef<HTMLInputElement | null>(null)
    const scrollRootRef = useRef<HTMLDivElement | null>(null)
    const sentinelRef = useRef<HTMLDivElement | null>(null)

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

    const onInputChange = (e: ChangeEvent<HTMLInputElement>) => setQuery(e.target.value)
    const isInitialSearchLoading = isSearching
        && (searchTechBlogsQuery.isPending || searchPostsQuery.isPending)
    const hasNextPage = searchPostsQuery.hasNextPage
    const isFetchingNextPage = searchPostsQuery.isFetchingNextPage
    const fetchNextPage = searchPostsQuery.fetchNextPage

    useEffect(() => {
        if (!open) return
        if (!isSearching) return
        if (!scrollRootRef.current || !sentinelRef.current) return

        const observer = new IntersectionObserver(
            (entries) => {
                const entry = entries[0]
                if (!entry?.isIntersecting) return
                if (!hasNextPage) return
                if (isFetchingNextPage) return
                fetchNextPage()
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
        open,
        isSearching,
        searchQuery,
        hasNextPage,
        isFetchingNextPage,
        fetchNextPage,
    ])

    if (!open) return null

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

export default Search
