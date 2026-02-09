import { useEffect, useMemo } from "react"
import styles from "./MainPage.module.css"

import LeftSidebar from "../../components/LeftSideBar/LeftSidebar"
import PostList from "../../components/PostList/PostList"
import useAuth from "../../auth/useAuth"
import { useSearchParams } from "react-router-dom"

import {
    usePostsQuery,
    usePostsByBookmarkQuery,
    usePostsBySubscriptionQuery,
    usePostsByTechBlogIdQuery,
} from "../../queries/post.queries"
import { useSubscribingTechBlogsQuery } from "../../queries/techBlog.queries"
import Banner from "../../components/banner/Banner"

const TYPES = {
    ALL: "all",
    SUBSCRIBED: "subscribed",
    BOOKMARKED: "bookmarked",
} as const

type FeedType = typeof TYPES[keyof typeof TYPES]

const MainPage = () => {
    const { isLoggedIn } = useAuth()
    const [searchParams, setSearchParams] = useSearchParams()

    // ✅ URL이 단일 상태
    const rawType = searchParams.get("type")
    const type: FeedType =
        rawType === TYPES.SUBSCRIBED || rawType === TYPES.BOOKMARKED
            ? rawType
            : TYPES.ALL
    const techBlogId = searchParams.get("techBlogId") ?? null
    const page = Number(searchParams.get("page") ?? 1)
    const isWelcome = searchParams.get("welcome")

    // ✅ 로그인 안 했는데 구독/북마크 진입 시 URL 정리
    useEffect(() => {
        if (isWelcome) {
            setSearchParams((prev) => {
                const p = new URLSearchParams(prev)
                p.delete("welcome")
                return p
            })
        }

        if (!isLoggedIn && type !== TYPES.ALL) {
            setSearchParams((prev) => {
                const p = new URLSearchParams(prev)
                p.delete("type")
                p.delete("techBlogId")
                p.delete("page")
                return p
            })
        }
    }, [isLoggedIn, type, isWelcome, setSearchParams])

    // ✅ 구독 블로그 목록 (LeftSidebar용)
    const subsQuery = useSubscribingTechBlogsQuery()

    const subs = subsQuery.data?.techBlogs ?? []

    // ✅ posts 쿼리: type/blogKey에 따라 1개만 활성화
    const allPostsQuery = usePostsQuery(
        { page },
        { enabled: type === TYPES.ALL }
    )

    const subscribingPostsQuery = usePostsBySubscriptionQuery(
        { page },
        { enabled: type === TYPES.SUBSCRIBED && !techBlogId && isLoggedIn }
    )

    const techBlogPostsQuery = usePostsByTechBlogIdQuery(
        { page, techBlogId },
        { enabled: type === TYPES.SUBSCRIBED && !!techBlogId }
    )

    const bookmarkedPostsQuery = usePostsByBookmarkQuery(
        { page },
        { enabled: type === TYPES.BOOKMARKED && isLoggedIn }
    )

    const activeQuery = useMemo(() => {
        if (type === TYPES.ALL) return allPostsQuery
        if (type === TYPES.SUBSCRIBED) return techBlogId ? techBlogPostsQuery : subscribingPostsQuery
        if (type === TYPES.BOOKMARKED) return bookmarkedPostsQuery
        return allPostsQuery
    }, [type, techBlogId, allPostsQuery, subscribingPostsQuery, techBlogPostsQuery, bookmarkedPostsQuery])

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
        if (!techBlogId) {
            if (!isLoggedIn) return "게시글이 존재하지 않습니다."
            if (subs.length === 0) return "구독중인 블로그가 없습니다."
            return "구독중인 블로그에 게시글이 존재하지 않습니다."
        }
        return "기술 블로그에 게시글이 존재하지 않습니다."
    }, [type, techBlogId, isLoggedIn, subs.length])

    // ✅ 핸들러들: URL 업데이트만 함
    const onSelectType = (nextType: string) => {
        window.scrollTo({ top: 0, behavior: "smooth" })
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)

            if (nextType === TYPES.ALL) p.delete("type")
            else p.set("type", nextType)

            p.delete("techBlogId")
            p.delete("page")
            return p
        })
    }

    const onSelectBlog = (nextBlogId: string) => {
        window.scrollTo({ top: 0, behavior: "smooth" })
        setSearchParams((prev) => {
            const p = new URLSearchParams(prev)
            p.set("type", TYPES.SUBSCRIBED)
            p.set("techBlogId", nextBlogId)
            p.delete("page")
            return p
        })
    }

    return (
        <>
            {/*<section className={styles.banner}>*/}
            {/*    <img*/}
            {/*        src="https://i.imgur.com/Nl4XwSt.png"*/}
            {/*        alt="banner"*/}
            {/*        className={styles.bannerImage}*/}
            {/*    />*/}
            {/*</section>*/}

            <section className={styles.banner}>
                <Banner />
            </section>

            {isLoggedIn ? (
                <div className={styles.layout}>
                    <aside className={styles.left}>
                        <LeftSidebar
                            subscriptions={subs}
                            type={type}
                            blogKey={techBlogId}
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

export default MainPage
