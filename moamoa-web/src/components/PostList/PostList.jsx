import styles from "./PostList.module.css"
import PostItem from "../PostItem/PostItem.jsx";
import {useState} from "react";
import {Pagination} from "@mui/material";

export default function PostList({posts}) {

    const [page, setPage] = useState(1)

    return (
        <>
            <div className={styles.list}>
                {posts.map((p) => (
                    <PostItem
                        key={p.id}
                        post={p}
                    />
                ))}
            </div>

            <div className={styles.paginationWrap}>
                <Pagination
                    count={20}
                    page={page}
                    onChange={(_, value) => setPage(value)}
                />
            </div>
        </>
    )
}