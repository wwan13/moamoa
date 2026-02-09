---
name: web-frontend-architecture
description: "moamoa-web 또는 moamoa-admin/admin-web 프론트엔드 구조를 유지하며 기능을 추가/수정할 때 사용. React + Vite + React Query 공통 레이어(api/queries/auth/routes/pages/components), 인증 토큰 처리, 전역 모달/토스트 이벤트, 파일 배치 규칙을 프로젝트별(JS/TS)로 맞춰 적용할 때 이 스킬을 사용."
---

# Web Frontend Architecture

`moamoa-web`(user web)과 `moamoa-admin/admin-web`(admin web)의 공통 구조를 유지한다.

## 적용 대상 먼저 선택
- `moamoa-admin/admin-web`: TypeScript (`.ts/.tsx`) 기반 관리자 웹.
- `moamoa-web`: JavaScript (`.js/.jsx`) 기반 사용자 웹.
- 구현 시 대상 앱의 확장자/네이밍/레이아웃 구조를 그대로 따른다.
- 모든 프론트 작업의 최상위 컨벤션은 `/Users/taewan/dev/moamoa/.codex/agents/guides/frontend_convention.md`를 먼저 따른다.

## 공통 아키텍처 맵
- 진입점: `src/main.(ts|js)x`
- 앱 셸/전역 UI 이벤트 바인딩: `src/app/App.(ts|js)x`
- 라우팅: `src/routes/AppRoutes.(ts|js)x`
- 인증 컨텍스트: `src/auth/AuthContext.(ts|js)x`
- HTTP 클라이언트: `src/api/client.(ts|js)`
- 도메인 API: `src/api/*.api.(ts|js)`
- React Query 훅: `src/queries/*.queries.(ts|js)`
- 페이지: `src/pages/*`
- 공통 UI/레이아웃/모달: `src/components/*` 또는 `src/layouts/*`

## 공통 레이어 규칙
- `api` 레이어에서 HTTP 요청, DTO 타입(또는 JSDoc 타입), 응답 fallback 처리.
- `queries` 레이어에서 `useQuery`, `useMutation`, `invalidateQueries` 정의.
- `pages` 레이어에서 화면 상태/핸들러/쿼리 훅 조합.
- `components`는 프레젠테이션 우선, 도메인 호출 최소화.
- 인증 상태 변경(로그인/로그아웃/세션 키)은 `AuthContext`를 통해서만 처리.

## 공통 인증/네트워크 규칙
- API 호출은 반드시 `http` 래퍼(`src/api/client.*`)를 통해 수행.
- `Authorization` 헤더, `TOKEN_EXPIRED` 재발급, `LOGIN_AGAIN` 흐름을 우회하지 않는다.
- 401 처리 시 등록된 전역 핸들러(`setOnLoginRequired`, `setOnLogout`) 흐름을 유지한다.
- 사용자 피드백은 전역 함수(`showGlobalAlert`, `showGlobalConfirm`, `showToast`)를 우선 사용한다.

## 프로젝트별 차이 규칙
- admin-web:
1. 보호 라우트는 `AppLayout` 하위에 둔다.
2. `/login`은 레이아웃 바깥 라우트로 둔다.
3. 메뉴 변경 시 `src/components/layout/Sidebar.tsx`와 `AppRoutes.tsx`를 함께 수정한다.
- moamoa-web:
1. 레이아웃을 `DefaultLayout`/`BlankLayout`으로 분리해 라우트를 배치한다.
2. 인증 UI는 라우트 페이지 대신 `AuthContext`의 모달 상태(`authModal`)를 우선 활용한다.
3. 전역 검색 모달 이벤트(`setOnOpenSearch`, `setOnCloseSearch`) 흐름을 깨지 않는다.
4. TypeScript 마이그레이션 시 `/Users/taewan/dev/moamoa/.codex/agents/guides/moamoa-web-ts-migration-convention.md`를 기준 규칙으로 적용한다.

## 파일 배치 규칙
- 새 도메인 추가 순서:
1. `src/api/<domain>.api.(ts|js)`에 요청 함수 추가.
2. `src/queries/<domain>.queries.(ts|js)`에 query/mutation 훅 추가.
3. `src/pages/<Domain>Page/*`에 화면 상태 + 훅 조합 구현.
4. 필요 시 `src/components/<domain|ui>/` 또는 `src/layouts/`로 UI 분리.
5. `src/routes/AppRoutes.(ts|js)x`에 라우트 연결.

## React Query 규칙
- query key는 도메인 prefix를 사용한다.
- mutation 성공 후 관련 key를 `invalidateQueries`로 무효화한다.
- 목록 조건(페이지/검색/필터)은 query key에 포함한다.
- 기본 옵션(`retry`, `staleTime`, `refetchOnWindowFocus`)은 `main.*`의 `QueryClient` 설정을 따른다.

## TS 마이그레이션 컨벤션
- 저장소 공통 기준: `/Users/taewan/dev/moamoa/.codex/agents/guides/frontend_convention.md`
- `moamoa-web`를 TS로 바꿀 때는 `/Users/taewan/dev/moamoa/.codex/agents/guides/moamoa-web-ts-migration-convention.md`를 앱별 보완 기준으로 함께 사용한다.
- 필수 규칙:
1. 타입 안정성을 유지하고 주요 값/함수/API 응답 타입을 명시한다.
2. 함수 선언은 항상 `const fnName = (...) => { ... }` 형태를 사용한다.
- 추가 권장 규칙:
1. `any` 대신 `unknown` + 타입 가드를 사용한다.
2. 레이어는 `api -> queries -> pages/components` 순서를 유지한다.
3. API 호출은 `src/api/client.*`의 `http` 래퍼를 통해서만 수행한다.

## 구현 체크리스트
1. 대상 앱(`moamoa-web` 또는 `admin-web`)을 먼저 고정했는가?
2. 기능 위치를 `api → queries → pages/components`로 분리했는가?
3. API 호출이 `http` 래퍼를 통과하는가?
4. 인증 실패/토큰 만료 전역 처리 흐름을 깨지 않는가?
5. query key와 invalidate 범위가 일관적인가?
6. 대상 앱의 라우트/레이아웃 규칙을 지켰는가?
