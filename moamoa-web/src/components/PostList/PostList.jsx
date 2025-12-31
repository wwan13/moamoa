import Pagination from "@mui/material/Pagination"
import styles from "./PostList.module.css"
import PostItem from "../PostItem/PostItem.jsx"
import CategoryTabs from "../CategoryTab/CategoryTabs.jsx"

const CATEGORY_ITEMS = [
    {label: "전체", value: "ALL"},
    // {label: "개발", value: "DEV"},
    // {label: "데이터/ML", value: "DATA_ML"},
    // {label: "디자인", value: "DESIGN"},
    // {label: "프로덕트", value: "PRODUCT"},
]

export default function PostList({
                                     posts,
                                     page,
                                     totalPages,
                                     onChangePage,
                                     category,
                                     onChangeCategory,
                                     isBlogDetail
                                 }) {

    return (
        <>
            <CategoryTabs
                items={CATEGORY_ITEMS}
                value="ALL"
                onChange={(next) => onChangeCategory(next)}
            />

            <div className={styles.list}>
                {posts.map((p) => (
                    <PostItem key={p.id} post={p} isBlogDetail={isBlogDetail}/>
                ))}
            </div>

            <div className={styles.paginationWrap}>
                <Pagination
                    count={totalPages}
                    page={page}
                    onChange={(_, value) => onChangePage(value)}
                />
            </div>
        </>
    )
}