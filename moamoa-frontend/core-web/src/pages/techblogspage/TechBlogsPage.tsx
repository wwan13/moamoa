import { useEffect, useMemo, useState, type MouseEvent } from "react"
import styles from "./TechBlogsPage.module.css"
import { useCountUp } from "../../hooks/useCountUp"
import useAuth from "../../auth/useAuth"
import { showGlobalConfirm, showToast } from "../../api/client"
import { useNavigate } from "react-router-dom"
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined"
import NotificationsOffOutlinedIcon from "@mui/icons-material/NotificationsOffOutlined"
import { useQueryClient } from "@tanstack/react-query"
import type { TechBlogSummary, TechBlogList } from "../../api/techBlog.api"

import { useTechBlogsQuery } from "../../queries/techBlog.queries"
import {
  useDisableNotificationMutation,
  useEnableNotificationMutation,
  useSubscribeMutation,
  useUnsubscribeMutation,
} from "../../queries/techBlogSubscription.queries"
import ScrollTopButton from "../../components/scrolltopbutton/ScrollTopButton"
import SearchBar from "../noticelistpage/SearchBar"

const SKELETON_COUNT = 12

const TechBlogsPage = () => {
  const navigate = useNavigate()
  const { isLoggedIn, openLogin } = useAuth()
  const qc = useQueryClient()

  const [search, setSearch] = useState("")

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  // ✅ tech blogs list (query)
  const techBlogsQuery = useTechBlogsQuery()

  const rawBlogs = useMemo(
    () => techBlogsQuery.data?.techBlogs ?? [],
    [techBlogsQuery.data],
  )
  const totalCount = techBlogsQuery.data?.meta?.totalCount ?? rawBlogs.length
  const animated = useCountUp(totalCount, 900)

  const filteredBlogs = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rawBlogs

    return rawBlogs.filter((b) => {
      const title = (b.title ?? "").toLowerCase()
      const key = (b.key ?? "").toLowerCase()
      return title.includes(q) || key.includes(q)
    })
  }, [rawBlogs, search])

  // ✅ subscribe mutations
  const subscribeMutation = useSubscribeMutation({
    invalidateOnSuccess: false,
  })
  const unsubscribeMutation = useUnsubscribeMutation({
    invalidateOnSuccess: false,
  })
  const enableNotificationMutation = useEnableNotificationMutation()
  const disableNotificationMutation = useDisableNotificationMutation()
  const isSubscriptionPending =
    subscribeMutation.isPending || unsubscribeMutation.isPending
  const isNotificationPending =
    enableNotificationMutation.isPending ||
    disableNotificationMutation.isPending
  const isMutating = isSubscriptionPending || isNotificationPending

  const patchTechBlogCaches = (
    techBlogId: number,
    patcher: (blog: TechBlogSummary) => TechBlogSummary,
  ) => {
    qc.setQueriesData({ queryKey: ["techBlogs"] }, (old: unknown) => {
      const cache = old as TechBlogList | undefined
      if (!cache?.techBlogs) return old
      return {
        ...cache,
        techBlogs: cache.techBlogs.map((b) =>
          b.id === techBlogId ? patcher(b) : b,
        ),
      }
    })

    qc.setQueriesData({ queryKey: ["techBlog"] }, (old: unknown) => {
      const blog = old as TechBlogSummary | null | undefined
      if (!blog || blog.id !== techBlogId) return old
      return patcher(blog)
    })
  }

  const invalidateTechBlogCaches = () => {
    qc.invalidateQueries({ queryKey: ["techBlogs"], refetchType: "inactive" })
    qc.invalidateQueries({ queryKey: ["techBlog"], refetchType: "inactive" })
    qc.invalidateQueries({
      queryKey: ["techBlogs", "subscribed"],
      refetchType: "inactive",
    })
  }

  const subscriptionToggle = async (blog: TechBlogSummary) => {
    if (!isLoggedIn) {
      const ok = await showGlobalConfirm({
        title: "로그인",
        message: "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
        confirmText: "로그인",
      })
      if (!ok) {
        return
      }
      openLogin()
      return
    }
    if (isSubscriptionPending) return

    const wasSubscribed = !!blog.subscribed
    const wasNotificationEnabled = !!blog.notificationEnabled
    const techBlogId = blog.id

    if (wasSubscribed) {
      const ok = await showGlobalConfirm({
        title: "구독 해제",
        message: "이 기술 블로그 구독을 해제할까요?",
        confirmText: "해제",
        cancelText: "유지",
      })
      if (!ok) return
    }

    // optimistic
    patchTechBlogCaches(techBlogId, (b) => ({
      ...b,
      subscribed: !wasSubscribed,
      subscriptionCount: Math.max(
        0,
        (b.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
      ),
      notificationEnabled: wasSubscribed ? false : true,
    }))

    try {
      if (wasSubscribed) {
        await unsubscribeMutation.mutateAsync({ techBlogId })
      } else {
        await subscribeMutation.mutateAsync({ techBlogId })
      }
      invalidateTechBlogCaches()
      showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
    } catch {
      // rollback
      patchTechBlogCaches(techBlogId, (b) => ({
        ...b,
        subscribed: wasSubscribed,
        subscriptionCount: Math.max(
          0,
          (b.subscriptionCount ?? 0) + (wasSubscribed ? 1 : -1),
        ),
        notificationEnabled: wasNotificationEnabled,
      }))
      showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
    }
  }

  const notificationToggle = async (blog: TechBlogSummary) => {
    if (!isLoggedIn) {
      const ok = await showGlobalConfirm({
        title: "로그인",
        message: "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
        confirmText: "로그인",
      })
      if (!ok) {
        return
      }
      openLogin()
      return
    }
    if (isNotificationPending) return
    if (!blog.subscribed) return

    const wasEnabled = !!blog.notificationEnabled
    const techBlogId = blog.id

    if (wasEnabled) {
      const ok = await showGlobalConfirm({
        title: "알림 해제",
        message: "이 기술 블로그 알림을 해제할까요?",
        confirmText: "해제",
        cancelText: "유지",
      })
      if (!ok) return
    }

    patchTechBlogCaches(techBlogId, (b) => ({
      ...b,
      notificationEnabled: !wasEnabled,
    }))

    try {
      if (wasEnabled) {
        await disableNotificationMutation.mutateAsync({ techBlogId })
      } else {
        await enableNotificationMutation.mutateAsync({ techBlogId })
      }
      invalidateTechBlogCaches()
      showToast(wasEnabled ? "알림을 해제했어요." : "알림을 설정했어요.")
    } catch {
      patchTechBlogCaches(techBlogId, (b) => ({
        ...b,
        notificationEnabled: wasEnabled,
      }))
      showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
    }
  }

  const stop = (e: MouseEvent<HTMLElement>) => {
    e.preventDefault()
    e.stopPropagation()
  }

  const handleSubmissionButton = async () => {
    if (!isLoggedIn) {
      const ok = await showGlobalConfirm({
        title: "로그인",
        message: "로그인이 필요한 기능입니다. 로그인 하시겠습니까?",
        confirmText: "로그인",
      })
      if (!ok) {
        return
      }
      openLogin()
      return
    }

    navigate("/submission")
  }

  return (
    <>
      <div className={styles.wrap}>
        <section className={styles.info}>
          <div className={styles.infoInner}>
            <div className={styles.textSection}>
              <h1 className={styles.title}>
                <span className={styles.count}>{animated}</span>
                개의 기술 블로그를
                <br />
                모아보고 있어요
              </h1>
            </div>

            <div className={styles.buttonSection}>
              <p className={styles.ctaText}>찾으시는 기술 블로그가 없다면</p>
              <div className={styles.ctaRow}>
                <button
                  className={styles.primaryButton}
                  onClick={handleSubmissionButton}
                >
                  요청하기
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.listSection}>
          <div className={styles.listHeader}>
            <SearchBar
              query={search}
              hasInputQuery={search.trim().length > 0}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={() => {}}
              onSearch={() => {}}
              onClear={() => setSearch("")}
              placeholder="기술 블로그를 검색해 보세요"
              disabled={techBlogsQuery.isPending}
              className={styles.searchBar}
            />
          </div>

          {techBlogsQuery.isPending ? (
            <div className={styles.grid} aria-busy="true">
              {Array.from({ length: SKELETON_COUNT }).map((_, i) => (
                <article
                  key={`s-${i}`}
                  className={`${styles.card} ${styles.skeletonCard}`}
                >
                  <div className={styles.logoWrap}>
                    <div
                      className={`${styles.logo} ${styles.skeleton} ${styles.skeletonCircle}`}
                    />
                  </div>
                  <div
                    className={`${styles.skeletonLine} ${styles.skeleton}`}
                  />
                  <div
                    className={`${styles.skeletonLineShort} ${styles.skeleton}`}
                  />
                  <div className={styles.subscribedActions}>
                    <div
                      className={`${styles.skeletonBtn} ${styles.skeleton}`}
                    />
                    <span>·</span>
                    <div
                      className={`${styles.skeletonIcon} ${styles.skeleton}`}
                    />
                  </div>
                </article>
              ))}
            </div>
          ) : filteredBlogs.length === 0 ? (
            <div className={styles.empty}>
              <p>기술 블로그가 존재하지 않습니다.</p>
            </div>
          ) : (
            <div className={styles.grid}>
              {filteredBlogs.map((blog) => (
                <article
                  key={blog.id}
                  className={`${styles.card} ${
                    blog.subscribed
                      ? styles.cardSubscribed
                      : styles.cardUnsubscribed
                  }`}
                  onClick={() => navigate(`/blog/${blog.id}`)}
                >
                  <div className={styles.logoWrap}>
                    <img
                      src={blog.icon}
                      alt="thumbnail"
                      className={styles.logo}
                    />
                  </div>

                  <p className={styles.blogName}>{blog.title}</p>

                  <div className={styles.meta}>
                    <p className={styles.subscriptionCount}>
                      게시글 {blog.postCount}개 · 구독자{" "}
                      {blog.subscriptionCount}명
                    </p>
                  </div>

                  {blog.subscribed ? (
                    <div className={styles.subscribedActions}>
                      <button
                        className={styles.subscribedButton}
                        onClick={(e) => {
                          stop(e)
                          subscriptionToggle(blog)
                        }}
                        disabled={isMutating}
                      >
                        <span className={styles.subscribedCheck}>✓</span>
                        구독중
                      </button>

                      <span className={styles.actionDivider} aria-hidden="true">
                        ·
                      </span>

                      <button
                        className={
                          blog.notificationEnabled
                            ? styles.alarmActiveButton
                            : styles.alarmInactiveButton
                        }
                        onClick={(e) => {
                          stop(e)
                          notificationToggle(blog)
                        }}
                        disabled={isMutating}
                        aria-label={
                          blog.notificationEnabled ? "알림 해제" : "알림 설정"
                        }
                      >
                        {blog.notificationEnabled ? (
                          <NotificationsNoneOutlinedIcon fontSize="small" />
                        ) : (
                          <NotificationsOffOutlinedIcon fontSize="small" />
                        )}
                      </button>
                    </div>
                  ) : (
                    <div className={styles.hoverAction}>
                      <div className={styles.subscribeButtonWrap}>
                        {!isLoggedIn && (
                          <div className={styles.loginTooltip} role="tooltip">
                            로그인 후 구독할 수 있어요
                          </div>
                        )}

                        <button
                          className={styles.hoverSubscribeButton}
                          onClick={(e) => {
                            stop(e)
                            subscriptionToggle(blog)
                          }}
                          disabled={isMutating}
                        >
                          구독
                        </button>
                      </div>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
      <ScrollTopButton />
    </>
  )
}

export default TechBlogsPage
