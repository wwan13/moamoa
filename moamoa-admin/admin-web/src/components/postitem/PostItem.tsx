import { useEffect, useState } from "react"
import styles from "./PostItem.module.css"
import { Dropdown } from "../ui/Dropdown.tsx"
import { ListItem } from "../ui/ListItem.tsx"

export type Post = {
    id: number
    title: string
    description: string
    thumbnail: string
    url: string
    categoryId: number

    techBlogId: number
    techBlogIcon: string
    techBlogTitle: string
}

type PostItemProps = {
    post: Post
    templateColumns?: string
    open?: boolean
    defaultOpen?: boolean
    onOpenChange?: (open: boolean) => void
    onCategoryChange?: (
        postId: number,
        nextCategoryId: number,
        prevCategoryId: number
    ) => Promise<boolean>
}

const categoryOptions = [
    { value: "10", label: "엔지니어링" },
    { value: "20", label: "프로덕트" },
    { value: "30", label: "디자인" },
    { value: "40", label: "기타" },
    { value: "999", label: "미분류" },
]

const PostItem = ({
    post,
    templateColumns = "240px 160px 100px 180px",
    open,
    defaultOpen,
    onOpenChange,
    onCategoryChange,
}: PostItemProps) => {
    const postUrlHost = (() => {
        try {
            return new URL(post.url).host
        } catch {
            return post.url
        }
    })()
    const [selectedCategory, setSelectedCategory] = useState(String(post.categoryId))
    const [isUpdating, setIsUpdating] = useState(false)

    useEffect(() => {
        setSelectedCategory(String(post.categoryId))
    }, [post.categoryId, post.id])

    const handleCategoryChange = async (value: string) => {
        if (isUpdating || value === selectedCategory) return

        const prevCategoryId = Number(selectedCategory)
        const nextCategoryId = Number(value)

        setSelectedCategory(value)

        if (!onCategoryChange) return

        setIsUpdating(true)
        const isSuccess = await onCategoryChange(post.id, nextCategoryId, prevCategoryId)
        if (!isSuccess) {
            setSelectedCategory(String(prevCategoryId))
        }
        setIsUpdating(false)
    }

    return (
        <ListItem
            templateColumns={templateColumns}
            cells={[
                <div key="title" className={styles.titleCell}>
                    <strong className={styles.title}>{post.title}</strong>
                </div>,
                <div key="blog" className={styles.blogCell}>
                    <img
                        src={post.techBlogIcon}
                        alt={post.techBlogTitle}
                        className={styles.blogIcon}
                    />
                    <span className={styles.blogTitle}>{post.techBlogTitle}</span>
                </div>,
                <span key="url" className={styles.url}>
                    {postUrlHost}
                </span>,
                <div
                    key="category"
                    className={styles.categoryCell}
                    data-no-row-toggle="true"
                >
                    <Dropdown
                        options={categoryOptions}
                        value={selectedCategory}
                        disabled={isUpdating}
                        onChange={(value) => {
                            void handleCategoryChange(value)
                        }}
                        placeholder="카테고리"
                    />
                </div>,
            ]}
            open={open}
            defaultOpen={defaultOpen}
            onOpenChange={onOpenChange}
        >
            <article className={styles.detail}>
                <img
                    src={post.thumbnail}
                    alt={post.title}
                    className={styles.thumbnail}
                />
                <div className={styles.content}>
                    <p className={styles.description}>{post.description}</p>
                    <a
                        href={post.url}
                        target="_blank"
                        rel="noreferrer"
                        className={styles.link}
                    >
                        원문 보기
                    </a>
                </div>
            </article>
        </ListItem>
    )
}

export default PostItem
