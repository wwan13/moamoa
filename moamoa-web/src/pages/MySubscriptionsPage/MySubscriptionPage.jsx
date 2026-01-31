import styles from "./MySubscriptionPage.module.css"
import { useEffect, useMemo, useState } from "react"
import useAuth from "../../auth/AuthContext.jsx"
import { useNavigate } from "react-router-dom"

import { useSubscribingTechBlogsQuery } from "../../queries/techBlog.queries.js"
import TechBlogItem from "../../components/TechBlogItem/TechBlogItem.jsx"

const SKELETON_COUNT = 8

export default function MySubscriptionPage() {
    const { isLoggedIn } = useAuth()
    const navigate = useNavigate()

    useEffect(() => {
        if (!isLoggedIn) navigate("/")
    }, [isLoggedIn, navigate])

    const techBlogsQuery = useSubscribingTechBlogsQuery()
    const techBlogs = techBlogsQuery.data?.techBlogs ?? []

    const list = useMemo(() => {
        if (techBlogsQuery.isPending) {
            return Array.from({ length: SKELETON_COUNT }).map((_, i) => (
                <TechBlogItem key={`s-${i}`} isSkeleton />
            ))
        }

        return techBlogs.map((techBlog) => (
            <TechBlogItem key={techBlog.id} techBlog={techBlog} onItemClick={() => {
                navigate(`/${techBlog.id}`)
            }} />
        ))
    }, [techBlogsQuery.isPending, techBlogs])

    return (
        <div className={styles.wrap}>
            <p className={styles.title}>모든 구독 블로그</p>
            <div className={styles.itemsWrap}>{list}</div>
        </div>
    )
}