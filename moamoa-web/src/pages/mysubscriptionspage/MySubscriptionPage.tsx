import styles from "./MySubscriptionPage.module.css"
import { useEffect, useMemo } from "react"
import useAuth from "../../auth/useAuth"
import { useNavigate } from "react-router-dom"

import { useSubscribingTechBlogsQuery } from "../../queries/techBlog.queries"
import TechBlogItem from "../../components/techblogitem/TechBlogItem"
import ScrollTopButton from "../../components/scrolltopbutton/ScrollTopButton"

const SKELETON_COUNT = 8

const MySubscriptionPage = () => {
  const { isLoggedIn } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  useEffect(() => {
    if (!isLoggedIn) navigate("/")
  }, [isLoggedIn, navigate])

  const techBlogsQuery = useSubscribingTechBlogsQuery()
  const techBlogs = useMemo(
    () => techBlogsQuery.data?.techBlogs ?? [],
    [techBlogsQuery.data],
  )

  const list = useMemo(() => {
    if (techBlogsQuery.isPending) {
      return Array.from({ length: SKELETON_COUNT }).map((_, i) => (
        <TechBlogItem key={`s-${i}`} isSkeleton />
      ))
    }

    return techBlogs.map((techBlog) => (
      <TechBlogItem
        key={techBlog.id}
        techBlog={techBlog}
        onItemClick={() => {
          navigate(`/blog/${techBlog.id}`)
        }}
      />
    ))
  }, [techBlogsQuery.isPending, techBlogs, navigate])

  return (
    <>
      <div className={styles.wrap}>
        <p className={styles.title}>모든 구독 블로그</p>
        <div className={styles.itemsWrap}>{list}</div>
      </div>
      <ScrollTopButton />
    </>
  )
}
export default MySubscriptionPage
