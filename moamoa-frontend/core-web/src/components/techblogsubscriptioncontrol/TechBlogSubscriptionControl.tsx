import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined"
import NotificationsOffOutlinedIcon from "@mui/icons-material/NotificationsOffOutlined"
import type { MouseEventHandler } from "react"
import styles from "./TechBlogSubscriptionControl.module.css"

type TechBlogSubscriptionControlProps = {
  subscribed: boolean
  notificationEnabled: boolean
  onSubscriptionClick: MouseEventHandler<HTMLButtonElement>
  onNotificationClick: MouseEventHandler<HTMLButtonElement>
  isSubscriptionDisabled?: boolean
  isNotificationDisabled?: boolean
  withTopSpacing?: boolean
}

const TechBlogSubscriptionControl = ({
  subscribed,
  notificationEnabled,
  onSubscriptionClick,
  onNotificationClick,
  isSubscriptionDisabled = false,
  isNotificationDisabled = false,
  withTopSpacing = false,
}: TechBlogSubscriptionControlProps) => {
  return (
    <div
      className={`${styles.control} ${withTopSpacing ? styles.withTopSpacing : ""}`.trim()}
    >
      {!subscribed ? (
        <button
          className={styles.subscribeButton}
          onClick={onSubscriptionClick}
          disabled={isSubscriptionDisabled}
        >
          구독
        </button>
      ) : (
        <>
          <button
            className={styles.subscribedButton}
            onClick={onSubscriptionClick}
            disabled={isSubscriptionDisabled}
          >
            <span className={styles.subscribedCheck}>✓</span>
            구독중
          </button>

          <span className={styles.actionDivider} aria-hidden="true">
            ·
          </span>

          <button
            className={
              notificationEnabled
                ? styles.notificationEnabledButton
                : styles.notificationDisabledButton
            }
            onClick={onNotificationClick}
            disabled={isNotificationDisabled}
            aria-label={notificationEnabled ? "알림 해제" : "알림 설정"}
          >
            {notificationEnabled ? (
              <NotificationsOffOutlinedIcon fontSize="small" />
            ) : (
              <NotificationsNoneOutlinedIcon fontSize="small" />
            )}
          </button>
        </>
      )}
    </div>
  )
}

export default TechBlogSubscriptionControl
