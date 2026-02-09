# Frontend Convention

이 문서는 `moamoa` 저장소의 모든 프론트엔드 개발 공통 기준이다.

## 적용 범위
- 적용 대상: `moamoa-web`, `moamoa-admin/admin-web` 및 향후 추가되는 프론트 프로젝트
- 우선순위: 이 문서 > 앱별 컨벤션 문서

## 필수 규칙
1. 항상 타입 안정성을 유지하고 주요 값/함수/API 입출력 타입을 명시한다.
2. 함수 선언은 항상 `const fnName = (...) => { ... }` 형태를 사용한다.

## 타입 시스템 규칙
- `any` 사용 금지. 필요한 경우 `unknown` + 타입 가드를 사용한다.
- API 요청/응답 타입은 `src/api/*.api.*`에서 선언한다.
- 컴포넌트 props와 훅 반환 타입을 명시한다.
- 비동기 함수는 반환 타입(`Promise<T>`)을 명시한다.
- `as` 단언은 최소화하고 런타임 검증을 우선한다.

## 아키텍처/레이어 규칙
- 레이어 분리: `api -> queries -> pages/components`
- `api`: 네트워크 호출/DTO/직렬화
- `queries`: React Query 훅, 캐시 키, invalidate 정책
- `pages`: 화면 상태, 이벤트 핸들링, 쿼리 조합
- `components`: 프레젠테이션 중심, 도메인 호출 최소화
- 인증 상태 변경은 `AuthContext` 계층에서만 처리한다.

## 네트워크/에러 처리 규칙
- API 호출은 `src/api/client.*`의 `http` 래퍼를 통해서만 수행한다.
- 토큰 만료/재발급/로그인 재요구 흐름을 우회하지 않는다.
- 사용자 피드백은 전역 알림 체계(`showGlobalAlert`, `showGlobalConfirm`, `showToast`)를 우선 사용한다.

## React Query 규칙
- query key는 도메인 prefix를 사용한다.
- 목록 조건(페이지/검색/필터)을 query key에 포함한다.
- mutation 성공 시 관련 query key를 invalidate 한다.
- `QueryClient` 기본 옵션(`retry`, `staleTime` 등)을 프로젝트 전체에서 일관되게 유지한다.

## 네이밍/코드 스타일 규칙
- 파일명: 도메인 기반 네이밍 유지 (`post.api.ts`, `post.queries.ts`)
- 훅은 `useXxx`, 조회 함수는 `fetchXxx`, 변경 함수는 `update/create/deleteXxx` 패턴 권장
- 기본 export 지양, named export 우선
- 새 코드에서 `function foo(){}` 선언 추가 금지

## 마이그레이션 규칙 (JS -> TS)
1. 파일 확장자를 `.js/.jsx -> .ts/.tsx`로 변경한다.
2. `api` 타입 정의를 먼저 고정한다.
3. `queries` 훅 시그니처와 query key 타입을 정리한다.
4. 페이지/컴포넌트 props 타입을 선언한다.
5. 마지막에 전역 인증/에러 흐름 타입을 점검한다.

## 금지 사항
- `// @ts-ignore`를 남긴 채 머지 금지
- 타입 없는 외부 응답을 UI에 직접 바인딩 금지
- 레이어를 건너뛰는 직접 호출(`pages` -> raw fetch) 금지
