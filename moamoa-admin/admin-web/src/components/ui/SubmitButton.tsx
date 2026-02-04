import styles from './SubmitButton.module.css'

type SubmitButtonProps = {
    label: string
    onClick: () => void
}

const SubmitButton = ({
                          label,
                          onClick = () => {},
                      }: SubmitButtonProps) => {
    return (
        <button
            type="submit"
            onClick={onClick}
            className={styles.button}
        >
            {label}
        </button>
    )
}

export default SubmitButton