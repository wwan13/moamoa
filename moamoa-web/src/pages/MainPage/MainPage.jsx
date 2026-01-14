import { useEffect, useMemo } from "react"
import styles from "./MainPage.module.css"

import LeftSidebar from "../../components/LeftSideBar/LeftSidebar.jsx"
import PostList from "../../components/PostList/PostList.jsx"
import useAuth from "../../auth/AuthContext.jsx"
import { useSearchParams } from "react-router-dom"

import { useSubscribingBlogsQuery } from "../../queries/techBlogSubscription.queries.js"
import {
    usePostsQuery,
    usePostsByBookmarkQuery,
    usePostsBySubscriptionQuery,
    usePostsByTechBlogKeyQuery,
} from "../../queries/post.queries.js"

const TYPES = {
    ALL: "all",
    SUBSCRIBED: "subscribed",
    BOOKMARKED: "bookmarked",
}

export default function MainPage() {
    const { isLoggedIn } = useAuth()
    const [searchParams, setSearchParams] = useSearchParams()

    // ✅ URL이 단일 상태
    const type = searchParams.get("type") ?? TYPES.ALL
    const blogKey = searchParams.get("techBlog") ?? null
    const page = Number(searchParams.get("page") ?? 1)

    // ✅ 로그인 안 했는데 구독/북마크 진입 시 URL 정리
    useEffect(() => {
        if (!isLoggedIn && type !== TYPES.ALL) {
            setSearchParams((prev) => {
                const p = new URLSearchParams(prev)
                p.delete("type")
                p.delete("techBlog")
                p.delete("page")
                return p
            })
        }
    }, [isLoggedIn, type, setSearchParams])

    // ✅ 구독 블로그 목록 (LeftSidebar용)
    const subsQuery = useSubscribingBlogsQuery({
        enabled: isLoggedIn,
    })

    const subs = subsQuery.data ?? []

    // ✅ posts 쿼리: type/blogKey에 따라 1개만 활성화
    const allPostsQuery = usePostsQuery(
        { page },
        { enabled: type === TYPES.ALL }
    )

    const subscribingPostsQuery = usePostsBySubscriptionQuery(
        { page },
        { enabled: type === TYPES.SUBSCRIBED && !blogKey && isLoggedIn }
    )

    const techBlogPostsQuery = usePostsByTechBlogKeyQuery(
        { page, techBlogKey: blogKey },
        { enabled: type === TYPES.SUBSCRIBED && !!blogKey }
    )

    const bookmarkedPostsQuery = usePostsByBookmarkQuery(
        { page },
        { enabled: type === TYPES.BOOKMARKED && isLoggedIn }
    )

    const activeQuery = useMemo(() => {
        if (type === TYPES.ALL) return allPostsQuery
        if (type === TYPES.SUBSCRIBED) return blogKey ? techBlogPostsQuery : subscribingPostsQuery
        if (type === TYPES.BOOKMARKED) return bookmarkedPostsQuery
        return allPostsQuery
    }, [type, blogKey, allPostsQuery, subscribingPostsQuery, techBlogPostsQuery, bookmarkedPostsQuery])

    const postsRes = activeQuery.data
    const posts = postsRes?.posts ?? []
    const totalPages = postsRes?.meta?.totalPages ?? 0

    // ✅ 첫 로딩(1초 이상일 때만) 스켈레톤: PostList 내부에서 딜레이 처리하므로 isPending만 내려줌
    const isPostsLoading = !!activeQuery.isPending
    const isSubsLoading = !!subsQuery.isPending

    // ✅ empty message
    const emptyMessage = useMemo(() => {
        if (type === TYPES.ALL) return "게시글이 존재하지 않습니다."
        if (type === TYPES.BOOKMARKED) return "북마크한 게시글이 없습니다."

        // subscribed
        if (!blogKey) {
            if (!isLoggedIn) return "게시글이 존재하지 않습니다."
            if (subs.length === 0) return "구독중인 블로그가 없습니다."
            return "구독중인 블로그에 게시글이 존재하지 않습니다."
        }
        return "기술 블로그에 게시글이 존재하지 않습니다."
    }, [type, blogKey, isLoggedIn, subs.length])

    // ✅ 핸들러들: URL 업데이트만 함
    const onSelectType = (nextType) => {
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)

            if (nextType === TYPES.ALL) p.delete("type")
            else p.set("type", nextType)

            p.delete("techBlog")
            p.delete("page")
            return p
        })
    }

    const onSelectBlog = (nextBlogKey) => {
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)
            p.set("type", TYPES.SUBSCRIBED)
            p.set("techBlog", nextBlogKey)
            p.delete("page")
            return p
        })
    }

    return (
        <>
            <section className={styles.banner}>
                <img
                    src="https://i.imgur.com/Nl4XwSt.png"
                    alt="banner"
                    className={styles.bannerImage}
                />
            </section>

            {isLoggedIn ? (
                <div className={styles.layout}>
                    <aside className={styles.left}>
                        <LeftSidebar
                            subscriptions={subs}
                            type={type}
                            blogKey={blogKey}
                            onSelectType={onSelectType}
                            onSelectBlog={onSelectBlog}
                            isLoading={isSubsLoading}
                        />
                    </aside>

                    <section className={styles.content}>
                        <PostList
                            posts={posts}
                            totalPages={totalPages}
                            type={type}
                            emptyMessage={emptyMessage}
                            isLoading={isPostsLoading}
                        />
                    </section>
                </div>
            ) : (
                <section className={styles.content}>
                    <PostList
                        posts={posts}
                        totalPages={totalPages}
                        isBlogDetail={false}
                        emptyMessage={emptyMessage}
                        isLoading={isPostsLoading}
                    />
                </section>
            )}
        </>
    )
}