import styles from "./LeftSidebar.module.css"
import Subscriptions from "../subscriptions/Subscriptions.jsx"

export default function LeftSidebar({ subscriptions }) {
    return (
        <aside className={styles.wrap}>
            <Subscriptions
                items={subscriptions}
                onClickItem={(item) => {
                    console.log("구독 클릭:", item.id)
                    // TODO: 해당 기술 블로그 페이지 이동
                }}
            />
        </aside>
    )
}