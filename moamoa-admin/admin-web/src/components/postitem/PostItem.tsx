import { useState } from "react"
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
}

const categoryOptions = [
    { value: "0", label: "전체" },
    { value: "1", label: "백엔드" },
    { value: "2", label: "프론트엔드" },
    { value: "3", label: "프로덕트" },
    { value: "4", label: "디자인" },
    { value: "5", label: "기타" },
    { value: "999", label: "미분류" },
]

const PostItem = ({
    post,
    templateColumns = "240px 160px 100px 180px",
    open,
    defaultOpen,
    onOpenChange,
}: PostItemProps) => {
    const postUrlHost = (() => {
        try {
            return new URL(post.url).host
        } catch {
            return post.url
        }
    })()
    const [selectedCategory, setSelectedCategory] = useState("0")

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
                        onChange={(value) => setSelectedCategory(value)}
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
