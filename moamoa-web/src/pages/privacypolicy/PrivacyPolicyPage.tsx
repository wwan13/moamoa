import policy from "./content/PrivacyPolicy.md?raw"
import styles from "./PrivacyPolicyPage.module.css"
import Markdown from "../../components/markdown/Markdown.tsx";

const PrivacyPolicyPage = () => {
    return (
        <div className={styles.body}>
            <Markdown>{policy}</Markdown>
        </div>
    )
}

export default PrivacyPolicyPage