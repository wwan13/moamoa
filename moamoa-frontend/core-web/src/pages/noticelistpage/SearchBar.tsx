import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined"
import CancelRoundedIcon from "@mui/icons-material/CancelRounded"
import type { ChangeEvent, KeyboardEvent, RefObject } from "react"
import styles from "./SearchBar.module.css"

type SearchBarProps = {
  query: string
  hasInputQuery: boolean
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
  onKeyDown?: (event: KeyboardEvent<HTMLInputElement>) => void
  onSearch: () => void
  onClear: () => void
  placeholder?: string
  disabled?: boolean
  className?: string
  inputRef?: RefObject<HTMLInputElement | null>
}

const SearchBar = ({
  query,
  hasInputQuery,
  onChange,
  onKeyDown,
  onSearch,
  onClear,
  placeholder = "제목, 내용을 검색해 보세요",
  disabled = false,
  className,
  inputRef,
}: SearchBarProps) => {
  return (
    <div className={[styles.searchForm, className].filter(Boolean).join(" ")}>
      <input
        id="search"
        ref={inputRef}
        className={styles.search}
        type="text"
        value={query}
        onChange={onChange}
        onKeyDown={onKeyDown}
        placeholder={placeholder}
        disabled={disabled}
      />
      <button
        type="button"
        className={styles.searchButton}
        onClick={hasInputQuery ? onClear : onSearch}
        aria-label={hasInputQuery ? "검색어 지우기" : "검색"}
        disabled={disabled}
      >
        {hasInputQuery ? (
          <CancelRoundedIcon sx={{ fontSize: 20, color: "#BCC0C6" }} />
        ) : (
          <SearchOutlinedIcon sx={{ fontSize: 20, color: "#252525" }} />
        )}
      </button>
    </div>
  )
}

export default SearchBar
