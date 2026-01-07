import Pagination from "@mui/material/Pagination"
import styles from "./PostList.module.css"
import PostItem from "../PostItem/PostItem.jsx"
import CategoryTabs from "../CategoryTab/CategoryTabs.jsx"
import {useEffect, useState} from "react";
import {categoryApi} from "../../api/category.api.js";

export default function PostList({
                                     posts,
                                     page,
                                     totalPages,
                                     onChangePage,
                                     category,
                                     onChangeCategory,
                                     isBlogDetail
                                 }) {

    const [categories, setCategories] = useState([])
    const [selected, setSelected] = useState(0)

    useEffect(() => {
        const fetchCategories = async () => {
            const res = await categoryApi()
            setCategories([
                { id: 0, key: "ALL", title: "전체" },
                ...res,
            ])
        }
        fetchCategories()
    }, []);

    return (
        <>
            <CategoryTabs
                items={categories}
                id={selected}
                onChange={(next) => setSelected(next)}
            />

            <div className={styles.list}>
                {
                    posts.length === 0 ? (
                        <div className={styles.empty}>
                            <p>게시글이 존재하지 않습니다.</p>
                        </div>
                    ) : (
                        posts.map((p) => (
                            <PostItem key={p.id} post={p} isBlogDetail={isBlogDetail}/>
                        ))
                    )
                }
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