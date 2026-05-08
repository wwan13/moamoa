import styles from "./TechBlogItem.module.css"
import { useQueryClient } from "@tanstack/react-query"
import useAuth from "../../auth/useAuth"
import { showGlobalConfirm, showToast } from "../../api/client"
import {
  useDisableNotificationMutation,
  useEnableNotificationMutation,
  patchSubscribedTechBlogsCache,
  useSubscribeMutation,
  useUnsubscribeMutation,
} from "../../queries/techBlogSubscription.queries"
import type { TechBlogSummary } from "../../api/techBlog.api"
import TechBlogSubscriptionControl from "../techblogsubscriptioncontrol/TechBlogSubscriptionControl"

type TechBlogItemProps = {
  techBlog?: TechBlogSummary
  isSkeleton?: boolean
  onItemClick?: () => void
  syncSubscribedListCache?: boolean
}

type TechBlogListCache = {
  techBlogs: TechBlogSummary[]
}

const TechBlogItem = ({
  techBlog,
  isSkeleton = false,
  onItemClick,
  syncSubscribedListCache = true,
}: TechBlogItemProps) => {
  const qc = useQueryClient()
  const { isLoggedIn, openLogin, authScope } = useAuth()

  const subscribeMutation = useSubscribeMutation({
    invalidateOnSuccess: false,
  })
  const unsubscribeMutation = useUnsubscribeMutation({
    invalidateOnSuccess: false,
  })
  const enableNotificationMutation = useEnableNotificationMutation()
  const disableNotificationMutation = useDisableNotificationMutation()
  const isMutating =
    subscribeMutation.isPending ||
    unsubscribeMutation.isPending ||
    enableNotificationMutation.isPending ||
    disableNotificationMutation.isPending

  const patchBlog = (
    techBlogId: number,
    patcher: (blog: TechBlogSummary) => TechBlogSummary,
  ) => {
    qc.setQueriesData(
      { queryKey: ["techBlogs"], exact: false },
      (old: unknown) => {
        const cache = old as TechBlogListCache | undefined
        if (!cache?.techBlogs) return old
        return {
          ...cache,
          techBlogs: cache.techBlogs.map((b) =>
            b.id === techBlogId ? patcher(b) : b,
          ),
        }
      },
    )
  }

  const subscriptionToggle = async () => {
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

    if (!techBlog) return
    if (isMutating) return

    const wasSubscribed = !!techBlog.subscribed
    const techBlogId = techBlog.id

    if (wasSubscribed) {
      const ok = await showGlobalConfirm({
        title: "구독 해제",
        message: "이 기술 블로그 구독을 해제할까요?",
        confirmText: "해제",
        cancelText: "유지",
      })
      if (!ok) return
    }

    try {
      if (wasSubscribed) {
        await unsubscribeMutation.mutateAsync({ techBlogId })
      } else {
        await subscribeMutation.mutateAsync({ techBlogId })
      }
      patchBlog(techBlogId, (b) => ({
        ...b,
        subscribed: !wasSubscribed,
        subscriptionCount: Math.max(
          0,
          (b.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
        ),
        notificationEnabled: wasSubscribed ? false : true,
      }))
      if (syncSubscribedListCache) {
        patchSubscribedTechBlogsCache({
          queryClient: qc,
          authScope,
          techBlog: {
            ...techBlog,
            subscribed: !wasSubscribed,
            subscriptionCount: Math.max(
              0,
              (techBlog.subscriptionCount ?? 0) + (wasSubscribed ? -1 : 1),
            ),
            notificationEnabled: wasSubscribed ? false : true,
          },
          subscribed: !wasSubscribed,
          notificationEnabled: wasSubscribed ? false : true,
        })
      }
      showToast(wasSubscribed ? "구독을 해제했어요." : "구독했어요.")
    } catch {
      showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
    }
  }

  const notificationToggle = async () => {
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

    if (!techBlog) return
    if (isMutating) return

    const wasEnabled = !!techBlog.notificationEnabled
    const techBlogId = techBlog.id

    if (wasEnabled) {
      const ok = await showGlobalConfirm({
        title: "알림 해제",
        message: "이 기술 블로그 알림을 해제할까요?",
        confirmText: "해제",
        cancelText: "유지",
      })
      if (!ok) return
    }

    try {
      if (wasEnabled) {
        await disableNotificationMutation.mutateAsync({ techBlogId })
      } else {
        await enableNotificationMutation.mutateAsync({ techBlogId })
      }
      patchBlog(techBlogId, (b) => ({
        ...b,
        notificationEnabled: !wasEnabled,
      }))
      if (syncSubscribedListCache) {
        patchSubscribedTechBlogsCache({
          queryClient: qc,
          authScope,
          techBlog: {
            ...techBlog,
            notificationEnabled: !wasEnabled,
          },
          subscribed: true,
          notificationEnabled: !wasEnabled,
        })
      }
      showToast(wasEnabled ? "알람을 해제했어요." : "알람을 설정했어요.")
    } catch {
      showToast("처리 중 오류가 발생했어요. 다시 시도해 주세요.")
    }
  }

  if (isSkeleton) {
    return (
      <div className={styles.item} aria-busy="true">
        <div
          className={`${styles.iconWrap} ${styles.skeleton} ${styles.skeletonCircle}`}
        >
          <div className={styles.icon} />
        </div>
        <div className={styles.infoWrap}>
          <div className={styles.left}>
            <div className={`${styles.skeletonLine} ${styles.skeleton}`} />
            <div className={`${styles.skeletonLineShort} ${styles.skeleton}`} />
            <div className={`${styles.skeletonLineShort} ${styles.skeleton}`} />
          </div>
          <div className={styles.right}>
            <div className={`${styles.skeletonBtn} ${styles.skeleton}`} />
            <div className={`${styles.skeletonBtnCircle} ${styles.skeleton}`} />
          </div>
        </div>
      </div>
    )
  }

  if (!techBlog) return null

  return (
    <div className={styles.item} onClick={onItemClick}>
      <div className={styles.iconWrap}>
        <img src={techBlog.icon} alt="icon" className={styles.icon} />
      </div>

      <div className={styles.infoWrap}>
        <div className={styles.left}>
          <p className={styles.techBlogTitle}>{techBlog.title}</p>
          <p className={styles.techBlogSub}>
            구독자 {techBlog.subscriptionCount}명 · 게시글 {techBlog.postCount}
            개
          </p>
          <p
            className={styles.techBlogUrl}
            onClick={() => techBlog?.blogUrl && window.open(techBlog.blogUrl)}
          >
            {techBlog.blogUrl}
          </p>
        </div>

        <div className={styles.right}>
          <TechBlogSubscriptionControl
            subscribed={!!techBlog.subscribed}
            notificationEnabled={!!techBlog.notificationEnabled}
            onSubscriptionClick={(e) => {
              e.stopPropagation()
              subscriptionToggle()
            }}
            onNotificationClick={(e) => {
              e.stopPropagation()
              notificationToggle()
            }}
            isSubscriptionDisabled={isMutating}
            isNotificationDisabled={isMutating}
          />
        </div>
      </div>
    </div>
  )
}

export default TechBlogItem
