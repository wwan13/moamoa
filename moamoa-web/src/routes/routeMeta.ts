import { matchPath } from "react-router-dom"

const SITE_URL = "https://moamoa.dev"
const SITE_NAME = "moamoa"
const DEFAULT_OG_IMAGE = `${SITE_URL}/moamoa_main_logo.png`

type MetaRule = {
  path: string
  title: string
  description: string
  robots: "index,follow" | "noindex,nofollow"
}

type RouteMeta = {
  title: string
  description: string
  robots: "index,follow" | "noindex,nofollow"
  canonicalUrl: string
  ogType: "website"
  ogImage: string
  siteName: string
}

const DEFAULT_META: RouteMeta = {
  title: "moamoa | 기술 블로그 모아보기",
  description: "기술 블로그의 최신 글을 moamoa에서 한 번에 모아보세요.",
  robots: "noindex,nofollow",
  canonicalUrl: `${SITE_URL}/`,
  ogType: "website",
  ogImage: DEFAULT_OG_IMAGE,
  siteName: SITE_NAME,
}

const META_RULES: MetaRule[] = [
  {
    path: "/",
    title: "moamoa | 기술 블로그 모아보기",
    description: "기술 블로그의 최신 글을 moamoa에서 한 번에 모아보세요.",
    robots: "index,follow",
  },
  {
    path: "/blogs",
    title: "블로그 목록 | moamoa",
    description: "관심 있는 기술 블로그를 검색하고 구독해 보세요.",
    robots: "index,follow",
  },
  {
    path: "/blog/:techBlogId",
    title: "블로그 상세 | moamoa",
    description: "선택한 기술 블로그의 최신 글을 확인하세요.",
    robots: "index,follow",
  },
  {
    path: "/notice",
    title: "공지사항 | moamoa",
    description: "moamoa 서비스 공지사항을 확인하세요.",
    robots: "index,follow",
  },
  {
    path: "/privacy",
    title: "개인정보 처리방침 | moamoa",
    description: "moamoa 개인정보 처리방침입니다.",
    robots: "index,follow",
  },
  {
    path: "/subscription",
    title: "내 구독 | moamoa",
    description: "구독한 기술 블로그를 관리합니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/my",
    title: "마이페이지 | moamoa",
    description: "회원 정보와 활동을 관리합니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/submission",
    title: "블로그 제보 | moamoa",
    description: "새로운 기술 블로그를 제보하세요.",
    robots: "noindex,nofollow",
  },
  {
    path: "/password",
    title: "비밀번호 변경 | moamoa",
    description: "비밀번호를 변경합니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/unjoin",
    title: "회원 탈퇴 | moamoa",
    description: "회원 탈퇴를 진행합니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/signup",
    title: "회원가입 | moamoa",
    description: "moamoa 회원가입 페이지입니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/find/password",
    title: "비밀번호 찾기 | moamoa",
    description: "비밀번호를 재설정합니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/oauth2",
    title: "로그인 처리 중 | moamoa",
    description: "소셜 로그인 처리 페이지입니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/oauth2/email",
    title: "이메일 입력 | moamoa",
    description: "소셜 로그인 이메일 입력 페이지입니다.",
    robots: "noindex,nofollow",
  },
  {
    path: "/post/:postId",
    title: "게시글 이동 | moamoa",
    description: "원문 게시글로 이동합니다.",
    robots: "noindex,nofollow",
  },
]

const resolveCanonicalPath = (pattern: string, params: Record<string, string | undefined>) => {
  return pattern.replace(/:([A-Za-z0-9_]+)/g, (_, key: string) => {
    const value = params[key]
    if (!value) return ""
    return encodeURIComponent(value)
  })
}

export const resolveRouteMeta = (pathname: string): RouteMeta => {
  for (const rule of META_RULES) {
    const matched = matchPath({ path: rule.path, end: true }, pathname)
    if (!matched) continue

    const canonicalPath = resolveCanonicalPath(rule.path, matched.params)
    return {
      title: rule.title,
      description: rule.description,
      robots: rule.robots,
      canonicalUrl: `${SITE_URL}${canonicalPath || "/"}`,
      ogType: "website",
      ogImage: DEFAULT_OG_IMAGE,
      siteName: SITE_NAME,
    }
  }

  return {
    ...DEFAULT_META,
    canonicalUrl: `${SITE_URL}${pathname || "/"}`,
  }
}
