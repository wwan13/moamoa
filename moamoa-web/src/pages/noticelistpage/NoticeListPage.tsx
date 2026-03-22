import styles from "./NoticeListPage.module.css"
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import Pagination from "@mui/material/Pagination";

const NoticeListPage = () => {
    return (
        <div className={styles.wrap}>
            <div className={styles.titleWrap}>
                <span className={styles.title}>공지사항</span>
                <span className={styles.titleDescription}>모아모아의 서비스 개선 및 서비스 점검에 대한 소식을 전해드립니다</span>
            </div>

            <div className={styles.contentWrap}>
                <div className={styles.searchForm}>
                    <input id="search" className={styles.search} type="text" placeholder="제목, 내용을 검색해 보세요"/>
                    <SearchOutlinedIcon sx={{fontSize: 20, color: "#252525"}}/>
                </div>

                <div className={styles.noticeItem}>
                    <div className={styles.noticeItemTop}>
                        <div className={styles.noticeChip}>긴급공지</div>
                        <span className={styles.noticeTitle}>[서비스 점검] 엣지 브라우저 일부 버전 접속불가로 인해 서비스 점검을 합니다</span>
                    </div>
                    <span className={styles.publishedAt}>2026.03.31</span>
                </div>
            </div>

            <Pagination
                className={styles.pagination}
                count={20}
                page={1}
                onChange={(_, value) => console.log("")}
            />
        </div>
    )
}

export default NoticeListPage