import styles from './Button.module.css';

const Button = ({
    variant = 'primary',
    fullWidth = true,
    className = '',
    ...props
}) => {
    const isWidthControlledVariant = variant === 'primary' || variant === 'border';
    const widthClassName = isWidthControlledVariant
        ? fullWidth
            ? styles.widthFull
            : styles.widthAutoPad16
        : '';

    const buttonClassName = [styles.base, styles[variant], widthClassName, className]
        .filter(Boolean)
        .join(' ');

    return <button className={buttonClassName} {...props} />
}

export default Button
