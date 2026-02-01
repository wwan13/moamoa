# moamoa 테스트 작성 가이드 (agent.md)

이 문서는 moamoa 프로젝트에서 테스트를 일관되게 작성하기 위한 규칙을 정의한다.  
테스트는 **JUnit 5 실행 + MockK mocking + Kotest assertions + Coroutine runTest** 기준으로 작성한다.

---

## 1. 사용 라이브러리

- 테스트 실행: JUnit 5
- Assertion: kotest-assertions-core
- Mocking: MockK
- Coroutine 테스트: kotlinx-coroutines-test (runTest)
- Spring 테스트: spring-boot-starter-test

> Kotest runner / StringSpec / kotest-spring-extension은 사용하지 않는다.

---

## 2. 공통 테스트 베이스 클래스

`:moamoa-support:support-test` 모듈의 `server.test` 패키지에 정의된 아래 베이스 클래스를 **상속해서** 테스트를 작성한다.

```kotlin
@ExtendWith(MockKExtension::class)
abstract class UnitTest

@DataR2dbcTest
@TestEnvironment
abstract class RepositoryTest

@SpringBootTest
@TestEnvironment
abstract class IntegrationTest
```

---

## 3. 테스트 유형별 작성 방법

### 3.1 단위 테스트 (UnitTest)

- Spring Context 사용하지 않는다.
- 서비스 / 도메인 / 유틸 테스트에 사용한다.
- 외부 의존성은 MockK로 대체한다.
- suspend 함수는 반드시 `runTest`로 감싼다.

```kotlin
class SubmissionServiceTest : UnitTest {

    @Test
    fun `제보를 생성하면 저장 후 이벤트를 등록한다`() = runTest {
        // given
        // when
        // then
    }
}
```

---

### 3.2 Repository 테스트 (RepositoryTest)

- R2DBC Repository 동작을 검증한다.
- 실제 DB 동작(쿼리/정렬/페이징/제약조건)을 기준으로 작성한다.
- 커스텀 쿼리, 정렬, 페이징은 반드시 테스트한다.

```kotlin
class EventOutboxRepositoryTest(
    private val repository: EventOutboxRepository
) : RepositoryTest {

    @Test
    fun `미발행 이벤트를 오래된 순서로 조회한다`() {
        // given
        // when
        // then
    }
}
```

---

### 3.3 통합 테스트 (IntegrationTest)

- Service + DB + 트랜잭션 + outbox 포함한 실제 흐름을 검증한다.
- outbox 이벤트 생성 여부를 반드시 검증한다.

```kotlin
class SubmissionIntegrationTest(
    private val submissionService: SubmissionService,
    private val outboxRepository: EventOutboxRepository
) : IntegrationTest {

    @Test
    fun `제보를 생성하면 outbox에 이벤트가 저장된다`() {
        // given
        // when
        // then
    }
}
```

---

## 4. 테스트 작성 공통 규칙

- 테스트 이름은 항상 한글로 작성한다.
- 한 테스트는 하나의 시나리오만 검증한다.
- given / when / then 구조를 유지한다.
- 상태 변화 + 협력 객체 호출(verify)을 함께 검증한다.
- 예외 케이스는 타입과 메시지까지 검증한다.

---

## 5. Fixture 작성 규칙

### 5.1 기본 원칙

- Kotlin named parameter를 사용한다.
- fixture는 **최상위 함수**로 정의한다.
- 기본값은 항상 유효한 값으로 둔다.
- 호출 형태는 `createXxx(...)`로 통일한다.
- 디렉토리: `server.fixture`
- 파일: `XxxFixture.kt` (동일 도메인의 fixture를 한 파일에 모은다)
- 테스트 코드에서는 **도메인 생성은 반드시 fixture로만** 한다. (직접 생성 금지, 중복 생성 방지 및 기본값 재사용)

### 5.2 Fixture 예시

```kotlin
fun createSubmission(
    id: Long = 0L,
    blogTitle: String = "기본 제목",
    blogUrl: String = "https://example.com",
    notificationEnabled: Boolean = true,
    accepted: Boolean = false,
    memberId: Long = 1L
): Submission = Submission(
    id = id,
    blogTitle = blogTitle,
    blogUrl = blogUrl,
    notificationEnabled = notificationEnabled,
    accepted = accepted,
    memberId = memberId
)
```

사용 예시:

```kotlin
val submission = createSubmission(
    blogTitle = "무신사 테크",
    notificationEnabled = false
)
```

### 5.3 Fixture 적용 기준 (필수)

- 테스트에 필요한 값만 override한다. (id, 검증 대상 필드 등)
- 나머지 필드는 fixture 기본값을 그대로 사용한다.
- **직접 생성(생성자 호출/팩토리 직접 구현)을 금지**하고, fixture가 없으면 **먼저 fixture를 추가**한 뒤 테스트를 작성한다.

---

## 6. 시나리오 보강 규칙

시나리오가 없거나 부족하면 다음을 자동으로 추가한다.

- 필수 값 누락
- 존재하지 않는 리소스(없는 id)
- 중복 요청 / 멱등성
- outbox 이벤트 생성 여부
- 경계값(id=0 등)

---

## 7. 네이밍 및 패키지 규칙

- 테스트 클래스명
  - 단위 테스트: `XxxTest`
  - 통합 테스트: `XxxIntegrationTest`
- 패키지 구조는 대상 코드와 최대한 동일하게 유지한다.

---

## 8. 핵심 요약

- JUnit 5 실행
- MockK mocking
- Kotest assertions 사용
- suspend 함수는 runTest로 테스트
- Unit / Repository / Integration 테스트를 명확히 분리
- outbox 기반 이벤트는 반드시 검증
