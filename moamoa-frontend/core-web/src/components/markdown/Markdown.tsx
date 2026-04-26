import ReactMarkdown from "react-markdown"
import { markdownComponents } from "./elements"
import styles from "./Markdown.module.css"

type Props = {
  children: string
}

const Markdown = ({ children }: Props) => {
  return (
    <article className={styles.root}>
      <ReactMarkdown components={markdownComponents}>{children}</ReactMarkdown>
    </article>
  )
}

export default Markdown
