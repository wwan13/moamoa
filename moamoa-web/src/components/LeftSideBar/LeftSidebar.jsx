import styles from "./LeftSidebar.module.css"
import Subscriptions from "../subscriptions/Subscriptions.jsx"
import {useNavigate} from "react-router-dom";

export default function LeftSidebar({ subscriptions }) {
    const navigate = useNavigate()
    return (
        <aside className={styles.wrap}>
            <Subscriptions
                items={subscriptions}
                onClickItem={(item) => {
                    navigate(`/${item.key}`)
                }}
            />
        </aside>
    )
}