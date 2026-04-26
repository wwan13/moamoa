import styles from "./NoticeDetailPage.module.css"
import DOMPurify from "dompurify"
import { useEffect, useMemo } from "react"
import { useNavigate, useParams } from "react-router-dom"
import Button from "../../components/ui/Button.tsx"
import { showGlobalAlert } from "../../api/client"
import { useNoticeByIdQuery } from "../../queries/notice.queries"
import { formatDate } from "../../utils/date"

const NoticeDetailPage = () => {
  const { noticeId } = useParams()
  const navigate = useNavigate()
  const parsedNoticeId = useMemo(() => {
    const id = Number(noticeId)
    return Number.isFinite(id) && id > 0 ? id : null
  }, [noticeId])
  const noticeQuery = useNoticeByIdQuery({ noticeId: parsedNoticeId })
  const notice = noticeQuery.data
  const sanitizedNoticeContent = useMemo(() => {
    return DOMPurify.sanitize(notice?.content ?? "")
  }, [notice?.content])

  useEffect(() => {
    if (parsedNoticeId) return
    ;(async () => {
      await showGlobalAlert("잘못된 공지사항 경로입니다.")
      navigate("/notice", { replace: true })
    })()
  }, [parsedNoticeId, navigate])

  useEffect(() => {
    if (!noticeQuery.isError) return
    ;(async () => {
      await showGlobalAlert("공지사항 정보를 불러오지 못했어요.")
      navigate("/notice", { replace: true })
    })()
  }, [noticeQuery.isError, navigate])

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "smooth" })
  }, [parsedNoticeId])

  return (
    <div className={styles.wrap}>
      <p className={styles.title}>공지사항</p>
      <div className={styles.contentWrap}>
        <div className={styles.noticeInfo}>
          <div className={styles.noticeTitleWrap}>
            {noticeQuery.isPending ? (
              <>
                <div
                  className={`${styles.noticeChip} ${styles.skeleton} ${styles.skeletonChip}`}
                />
                <div
                  className={`${styles.noticeTitle} ${styles.skeleton} ${styles.skeletonTitle}`}
                />
              </>
            ) : (
              <>
                {!!notice?.chip && (
                  <div className={styles.noticeChip}>{notice.chip}</div>
                )}
                <p className={styles.noticeTitle}>{notice?.title ?? ""}</p>
              </>
            )}
          </div>
          <p className={styles.noticePublishedAt}>
            {noticeQuery.isPending
              ? ""
              : formatDate(notice?.publishedAt, { withTime: false })}
          </p>
        </div>

        <div className={styles.divider}></div>

        {noticeQuery.isPending ? (
          <div className={styles.noticeContent}>
            <div className={`${styles.skeleton} ${styles.skeletonContent}`} />
            <div className={`${styles.skeleton} ${styles.skeletonContent}`} />
            <div
              className={`${styles.skeleton} ${styles.skeletonContentShort}`}
            />
          </div>
        ) : (
          <div
            className={styles.noticeContent}
            dangerouslySetInnerHTML={{ __html: sanitizedNoticeContent }}
          />
        )}
      </div>

      <div className={styles.divider}></div>

      <div className={styles.buttonWrap}>
        <Button
          onClick={() => navigate("/notice")}
          variant="border"
          fullWidth={false}
        >
          목록으로
        </Button>
      </div>
    </div>
  )
}

export default NoticeDetailPage
