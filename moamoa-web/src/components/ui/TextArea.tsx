import type { TextareaHTMLAttributes } from "react"
import styles from "./TextArea.module.css"

type TextAreaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  hasError?: boolean
}

const TextArea = ({ hasError = false, className = "", ...props }: TextAreaProps) => {
  const textAreaClassName = [styles.textarea, hasError ? styles.error : "", className]
    .filter(Boolean)
    .join(" ")

  return <textarea className={textAreaClassName} {...props} />
}

export default TextArea
