import { useMemo, useState } from "react"
import Pagination from "@mui/material/Pagination"
import styles from './PostPage.module.css'
import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import { Dropdown } from "../../components/ui/Dropdown.tsx"
import { Search } from "../../components/ui/Search.tsx"
import { ListHeader } from "../../components/ui/ListHeader.tsx"
import PostItem, {type Post } from "../../components/postitem/PostItem"
import { showGlobalAlert } from "../../api/client"
import { usePostsQuery, useUpdatePostCategoryMutation } from "../../queries/post.queries"

const categoryDropdownOptions = [
    { value: "0", label: "전체" },
    { value: "10", label: "엔지니어링" },
    { value: "20", label: "프로덕트" },
    { value: "30", label: "디자인" },
    { value: "40", label: "기타" },
    { value: "999", label: "미분류" },
]

const PostsPage = () => {
    const [searchInput, setSearchInput] = useState("")
    const [query, setQuery] = useState("")
    const [categoryValue, setCategoryValue] = useState("0")
    const [page, setPage] = useState(1)
    const size = 20

    const categoryId = categoryValue === "0" ? undefined : Number(categoryValue)
    const { data, isLoading } = usePostsQuery({
        page,
        size,
        query: query || undefined,
        categoryId,
    })
    const updatePostCategoryMutation = useUpdatePostCategoryMutation()

    const posts: Post[] = useMemo(
        () =>
            (data?.posts ?? []).map((post) => ({
                id: post.postId,
                title: post.title,
                description: post.description,
                thumbnail: post.thumbnail,
                url: post.url,
                categoryId: post.categoryId,
                techBlogId: post.techBlog.id,
                techBlogIcon: post.techBlog.icon,
                techBlogTitle: post.techBlog.title,
            })),
        [data]
    )
    const totalPages = Math.max(data?.meta.totalPages ?? 0, 1)

    const handleCategoryChange = async (
        postId: number,
        nextCategoryId: number,
        prevCategoryId: number
    ): Promise<boolean> => {
        if (nextCategoryId === prevCategoryId) return true

        try {
            const result = await updatePostCategoryMutation.mutateAsync({
                postId,
                categoryId: nextCategoryId,
            })
            if (!result.success) {
                await showGlobalAlert("카테고리 변경에 실패했습니다.")
                return false
            }
            return true
        } catch {
            await showGlobalAlert("카테고리 변경에 실패했습니다.")
            return false
        }
    }

    return (
        <div className={styles.wrap}>
            <PageTitle value="게시글" />

            <section className={styles.filters}>
                <div className={styles.filterLeft}>
                    <div className={styles.searchWrap}>
                        <Search
                            value={searchInput}
                            onChange={setSearchInput}
                            onSearch={(value) => {
                                setPage(1)
                                setQuery(value.trim())
                            }}
                        />
                    </div>
                </div>
                <div className={styles.filterRight}>
                    <Dropdown
                        options={categoryDropdownOptions}
                        placeholder="카테고리"
                        value={categoryValue}
                        onChange={(value) => {
                            setPage(1)
                            setCategoryValue(value)
                        }}
                    />
                </div>
            </section>

            <section className={styles.listWrap}>
                <ListHeader
                    templateColumns="360px 200px 100px 180px"
                    columns={[
                        { key: "title", label: "Title" },
                        { key: "blog", label: "Tech Blog" },
                        { key: "url", label: "Link" },
                        { key: "category", label: "Category" },
                    ]}
                />

                {isLoading && <div className={styles.empty}>불러오는 중...</div>}
                {!isLoading && posts.length === 0 && (
                    <div className={styles.empty}>조회된 게시글이 없습니다.</div>
                )}
                {!isLoading &&
                    posts.map((post) => (
                        <PostItem
                            key={post.id}
                            post={post}
                            templateColumns="360px 200px 100px 180px"
                            onCategoryChange={handleCategoryChange}
                        />
                    ))}
            </section>

            <section className={styles.paginationWrap}>
                <Pagination
                    count={totalPages}
                    page={page}
                    onChange={(_, value) => setPage(value)}
                />
            </section>
        </div>
    )
}

export default PostsPage
