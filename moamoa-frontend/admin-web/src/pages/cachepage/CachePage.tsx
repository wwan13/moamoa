import { useEffect, useState } from "react"
import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import Button from "../../components/ui/Button.tsx"
import styles from "./CachePage.module.css"
import { showGlobalAlert, showGlobalConfirm, showToast } from "../../api/client"
import {
  useAdminCachesQuery,
  useEvictAdminCacheMutation,
  useEvictAllAdminCachesMutation,
} from "../../queries/cache.queries"

const CachePage = () => {
  const [evictingCacheKey, setEvictingCacheKey] = useState<string | null>(null)

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  const { data, isLoading } = useAdminCachesQuery()
  const evictCacheMutation = useEvictAdminCacheMutation()
  const evictAllMutation = useEvictAllAdminCachesMutation()

  const caches = data ?? []
  const evictableCaches = caches.filter((cache) => cache.evictable)
  const unsupportedCaches = caches.filter((cache) => !cache.evictable)

  const handleEvict = async (cacheKey: string, cacheName: string) => {
    const confirmed = await showGlobalConfirm({
      title: "캐시 비우기",
      message: `${cacheName} 캐시를 비우시겠어요?`,
      confirmText: "비우기",
    })
    if (!confirmed) return

    setEvictingCacheKey(cacheKey)

    try {
      const result = await evictCacheMutation.mutateAsync({ cacheKey })
      showToast(`${result.name} 캐시를 비웠습니다.`, { type: "success" })
    } catch {
      await showGlobalAlert("캐시 evict에 실패했습니다.")
    } finally {
      setEvictingCacheKey(null)
    }
  }

  const handleEvictAll = async () => {
    const confirmed = await showGlobalConfirm({
      title: "전체 캐시 비우기",
      message: `지원되는 캐시 ${evictableCaches.length}개를 모두 비우시겠어요?`,
      confirmText: "전체 비우기",
    })
    if (!confirmed) return

    try {
      const result = await evictAllMutation.mutateAsync()
      showToast(`${result.evicted.length}개 캐시를 비웠습니다.`, {
        type: "success",
      })
    } catch {
      await showGlobalAlert("전체 캐시 evict에 실패했습니다.")
    }
  }

  return (
    <div className={styles.wrap}>
      <PageTitle value="캐시 관리" />

      <section className={styles.summary}>
        <div>
          <p className={styles.summaryLabel}>운영자 evict 대상</p>
          <h3 className={styles.summaryValue}>
            {evictableCaches.length.toLocaleString("ko-KR")}개
          </h3>
          <p className={styles.summaryDescription}>
            prefix, exact key, versioned prefix 전략을 구분해서 관리합니다.
          </p>
        </div>

        <Button
          type="button"
          disabled={
            evictAllMutation.isPending || evictCacheMutation.isPending || isLoading
          }
          onClick={() => {
            void handleEvictAll()
          }}
        >
          전체 Evict
        </Button>
      </section>

      {isLoading && <div className={styles.empty}>캐시 목록을 불러오는 중입니다.</div>}

      {!isLoading && caches.length === 0 && (
        <div className={styles.empty}>등록된 캐시 정보가 없습니다.</div>
      )}

      {!isLoading && caches.length > 0 && (
        <section className={styles.grid}>
          {caches.map((cache) => (
            <article key={cache.key} className={styles.card}>
              <div className={styles.cardTop}>
                <div>
                  <div className={styles.badges}>
                    <span className={styles.badge}>{cache.target}</span>
                    <span className={styles.badge}>{formatStrategy(cache.evictionStrategy)}</span>
                  </div>
                  <h4 className={styles.cardTitle}>{cache.name}</h4>
                </div>

                <Button
                  type="button"
                  variant={cache.evictable ? "primary" : "outline"}
                  disabled={
                    !cache.evictable ||
                    evictAllMutation.isPending ||
                    (evictingCacheKey !== null && evictingCacheKey !== cache.key)
                  }
                  onClick={() => {
                    void handleEvict(cache.key, cache.name)
                  }}
                >
                  {evictingCacheKey === cache.key ? "처리 중..." : "Evict"}
                </Button>
              </div>

              <p className={styles.description}>{cache.description}</p>

              {cache.evictable ? (
                <p className={styles.strategyDescription}>
                  {describeStrategy(cache.evictionStrategy)}
                </p>
              ) : (
                <p className={styles.warning}>
                  {cache.unsupportedReason ?? "현재 운영자 evict를 지원하지 않습니다."}
                </p>
              )}
            </article>
          ))}
        </section>
      )}

      {!isLoading && unsupportedCaches.length > 0 && (
        <p className={styles.footnote}>
          일부 캐시는 저장소가 둘 이상이라 운영자 화면에서 안전한 전체 evict를 막아두었습니다.
        </p>
      )}
    </div>
  )
}

function formatStrategy(strategy: string) {
  switch (strategy) {
    case "exact_key":
      return "Exact Key"
    case "versioned_prefix":
      return "Versioned Prefix"
    case "prefix":
      return "Prefix"
    default:
      return "Unsupported"
  }
}

function describeStrategy(strategy: string) {
  switch (strategy) {
    case "exact_key":
      return "단일 key를 직접 제거합니다."
    case "versioned_prefix":
      return "버전 key와 과거 엔트리까지 prefix 기준으로 함께 제거합니다."
    case "prefix":
      return "같은 prefix를 쓰는 엔트리를 한 번에 제거합니다."
    default:
      return "운영자 evict를 지원하지 않습니다."
  }
}

export default CachePage
