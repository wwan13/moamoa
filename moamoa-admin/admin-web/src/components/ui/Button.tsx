import styles from "./Button.module.css"
import type { ButtonHTMLAttributes } from "react"

type ButtonProps = {
    variant?: "primary" | "outline"
    className?: string
} & ButtonHTMLAttributes<HTMLButtonElement>

const Button = ({
    variant = "primary",
    className = "",
    ...props
}: ButtonProps) => {
    return <button className={`${styles.base} ${styles[variant]} ${className}`} {...props} />
}

export default Button
