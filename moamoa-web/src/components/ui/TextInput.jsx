import styles from "./TextInput.module.css"

export default function TextInput({
                                      label,
                                      error,          // 에러 메시지(짧게)
                                      success,        // ✅ 추가: 성공 메시지(예: "인증 완료")
                                      labelRight,     // ✅ 추가: 라벨 오른쪽(예: 03:00)
                                      right,
                                      ...props
                                  }) {
    const message = error || success
    const isError = Boolean(error)

    return (
        <div className={styles.field}>
            <div className={styles.labelRow}>
                {/* ✅ 왼쪽: 라벨 + 메시지(에러/성공) */}
                <div className={styles.labelLeft}>
                    <span className={styles.labelText}>{label}</span>

                    {message && (
                        <span
                            className={`${styles.labelMessage} ${
                                isError ? styles.labelError : styles.labelSuccess
                            }`}
                        >
            {message}
          </span>
                    )}
                </div>

                {labelRight && <span className={styles.labelRight}>{labelRight}</span>}
            </div>

            <div className={`${styles.control} ${isError ? styles.controlError : ""}`}>
                <input className={styles.input} {...props} />
                {right && <div className={styles.right}>{right}</div>}
            </div>
        </div>
    )
}