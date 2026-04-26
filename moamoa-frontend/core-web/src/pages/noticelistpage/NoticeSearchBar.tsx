import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined"
import CancelRoundedIcon from "@mui/icons-material/CancelRounded"
import type { ChangeEvent, KeyboardEvent } from "react"
import styles from "./NoticeSearchBar.module.css"

type NoticeSearchBarProps = {
  query: string
  hasInputQuery: boolean
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
  onKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void
  onSearch: () => void
  onClear: () => void
}

const NoticeSearchBar = ({
  query,
  hasInputQuery,
  onChange,
  onKeyDown,
  onSearch,
  onClear,
}: NoticeSearchBarProps) => {
  return (
    <div className={styles.searchForm}>
      <input
        id="search"
        className={styles.search}
        type="text"
        value={query}
        onChange={onChange}
        onKeyDown={onKeyDown}
        placeholder="제목, 내용을 검색해 보세요"
      />
      <button
        type="button"
        className={styles.searchButton}
        onClick={hasInputQuery ? onClear : onSearch}
        aria-label={hasInputQuery ? "검색어 지우기" : "검색"}
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

export default NoticeSearchBar
