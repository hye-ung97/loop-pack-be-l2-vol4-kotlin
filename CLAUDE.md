# CLAUDE.md

Claude Code(및 기타 AI 코딩 도우미)가 본 프로젝트에서 작업할 때 참조해야 하는 가이드 문서입니다.

## 프로젝트 개요
- **이름** : `loopers-kotlin-spring-template` (group: `com.loopers`)
- **형태** : Gradle 멀티 모듈 기반의 Spring Boot 백엔드 프로젝트
- **목적** : 커머스 도메인을 다루는 백엔드 학습/실습 템플릿 (TDD 기반 점진적 구현)

## 기술 스택 및 버전
| 분류 | 항목 | 버전 |
| --- | --- | --- |
| Language | Kotlin | 2.0.20 |
| Runtime | JDK (toolchain) | 21 |
| Framework | Spring Boot | 3.4.4 |
| BOM | Spring Cloud | 2024.0.1 |
| Build | Gradle (Kotlin DSL) | wrapper 동봉 |
| ORM | Spring Data JPA + Hibernate | Spring Boot 동봉 |
| Query | QueryDSL (jakarta) | kapt 기반 |
| DB | MySQL | 컨테이너/`mysql-connector-j` |
| Cache | Redis (Spring Data Redis) | Spring Boot 동봉 |
| Messaging | Spring Kafka | Spring Boot 동봉 |
| Batch | Spring Batch | Spring Boot 동봉 |
| Security | spring-security-crypto (BCrypt) | Spring Boot 동봉 |
| API Docs | springdoc-openapi (webmvc-ui) | 2.7.0 |
| Serialize | jackson-module-kotlin / jsr310 | Spring Boot 동봉 |
| Lint | ktlint (Plugin 12.1.2 / Engine 1.0.1) | INTELLIJ_IDEA style |
| Coverage | Jacoco | Gradle plugin |
| Test (unit) | JUnit 5, kotlin-test-junit5 | platform 1.x |
| Test (mock) | springmockk 4.0.2, mockito 5.14.0, mockito-kotlin 5.4.0 | |
| Test (data) | Instancio JUnit | 5.0.2 |
| Test (env) | Testcontainers (mysql, redis, kafka) | Spring Boot 동봉 |
| Build env | JVM timezone : `Asia/Seoul`, profile : `test` (테스트 시) | |

> 버전을 추가/변경할 때는 `gradle.properties` 의 `*Version` 항목을 단일 소스로 사용합니다. 모듈별 `build.gradle.kts` 에서 직접 버전을 하드코딩하지 않습니다.

## 모듈 구조
멀티 모듈 위계는 `apps → (modules | supports)` 단방향 의존만 허용합니다.

```
Root
├── apps/                 # 실행 가능한 SpringBootApplication (BootJar=ON, Jar=OFF)
│   ├── commerce-api       # 사용자/주문 등 외부 REST API 서버 (현재 주요 작업 모듈)
│   ├── commerce-batch     # Spring Batch 기반 배치 잡 실행기
│   └── commerce-streamer  # Kafka 컨슈머/스트리머
├── modules/              # 도메인 비종속, 재사용 가능한 인프라 설정
│   ├── jpa                # Spring Data JPA + QueryDSL + MySQL
│   ├── redis              # Spring Data Redis
│   └── kafka              # Spring Kafka
└── supports/             # 부가 기능 add-on
    ├── jackson            # Object Mapper 설정
    ├── logging            # 로깅 설정 (slack appender 포함 가능)
    └── monitoring         # actuator/prometheus 연동
```

### `apps/commerce-api` 패키지 레이어
```
com.loopers
├── CommerceApiApplication.kt
├── config/         # @Configuration (예: PasswordConfig - BCryptPasswordEncoder)
├── interfaces/     # 외부 인터페이스 (REST Controller, ApiSpec, Dto, ApiResponse, ApiControllerAdvice)
├── application/    # 유즈케이스 (XxxFacade, XxxInfo) - 도메인 조립 및 표현 계층 변환
├── domain/         # 핵심 도메인 (Model, Service, Repository 인터페이스, BaseEntity)
├── infrastructure/ # 도메인 Repository 구현 (JpaRepository + RepositoryImpl)
└── support/        # 공용 에러 (ErrorType, CoreException) 등
```
- **의존 방향** : `interfaces → application → domain ← infrastructure`. `domain` 은 다른 레이어에 의존하지 않습니다.
- **DTO 경계** : `interfaces` 레이어의 `XxxV1Dto` 는 외부 노출용. `application` 은 `XxxInfo` 를 반환하고 컨트롤러가 응답 DTO 로 매핑합니다.
- **에러 처리** : 도메인/애플리케이션에서는 `CoreException(ErrorType, message)` 만 던지고, `ApiControllerAdvice` 가 일괄 변환합니다.

## 환경 셋업
```shell
# pre-commit hook (ktlint) 설치
make init

# 로컬 인프라 (DB 등) 기동
docker-compose -f ./docker/infra-compose.yml up

# 로컬 모니터링 (prometheus + grafana, http://localhost:3000)
docker-compose -f ./docker/monitoring-compose.yml up

# 빌드 / 테스트
./gradlew build
./gradlew :apps:commerce-api:test
./gradlew ktlintCheck
```

## 도메인 & 객체 설계 전략

### 1. Aggregate 와 도메인 모델
- **Aggregate Root 만 외부에서 참조**합니다. 내부 엔티티(`OrderItem` 등)는 Root 를 통해서만 접근합니다.
- Aggregate 간은 **ID 참조**만 사용합니다 (`Product → brandId`). JPA 연관관계로 직접 객체 참조하지 않습니다.
- 한 트랜잭션은 한 Aggregate 변경을 원칙으로 하되, **즉시 정합성이 필요한 두-Aggregate 묶음**(예: Like + Product.likeCount, Order + Product.stock)은 의도적으로 **Facade 가 같은 TX 로 조율**합니다.
- 도메인 객체는 **비즈니스 규칙을 캡슐화**합니다. 필드는 `protected set` 으로 외부 직접 변경을 막고, 도메인 메서드로만 상태를 바꿉니다.

### 2. Entity / VO / Domain Service 구분
- **Entity (`XxxModel`)** : 고유 ID + 연속성 + 상태 변화. `BaseEntity` 상속 (예외: `Like` 는 hard delete 라 미상속).
- **Value Object** : 식별자 없이 "값 자체"가 의미. 불변(`data class` / `value class`). 도메인 규칙 포함 (예: `Stock`, `Money`).
- **Domain Service** : 단일 도메인 객체에 담기 어려운 협력 로직만. **상태 없는 POJO 처럼 유지** — 가급적 Repository 의존을 피해 단위 테스트 용이성을 보존합니다. Repository 의존이 꼭 필요하면 구조가 어색하지 않은지 먼저 점검합니다.

### 3. Service vs Facade (Application Layer)
- **ApplicationService** : 단일 도메인 흐름 조율 + 트랜잭션 경계. 본인 도메인의 비즈 로직 수행에 집중하고, 다른 도메인의 Repository 에 직접 접근하지 않습니다.
- **Facade** : 여러 도메인의 ApplicationService(혹은 Repository) 를 조립해 유즈케이스 흐름을 완성합니다. 도메인 결합이 필요한 흐름(예: 상품 상세 = Product + Brand + Like)은 Facade 에 둡니다.
- **Controller → Application(Facade/Service)** 경유는 강제입니다. Controller 가 Domain Service 를 직접 호출하지 않습니다.

### 4. 검증 이중화
- **DTO 검증** (`@Valid` + `@NotBlank` / `@Min` 등) : 요청의 **형식**(null, 길이, 범위) 을 인터페이스 진입부에서 1차 검증.
- **도메인 검증** (`init` 블록 / 도메인 메서드 / VO 생성자) : 비즈니스 **불변규칙** 을 도메인 안에서 보장.
- 둘은 책임이 다르므로 **중복으로 보이더라도 둘 다 작성**합니다.

## 아키텍처 & 패키지 구성 전략

### 1. 레이어드 + DIP
- `interfaces → application → domain ← infrastructure` 단방향 의존을 유지합니다.
- Repository 는 **인터페이스를 `domain` 에**, 구현체(`XxxRepositoryImpl`) 를 `infrastructure` 에 둡니다.
- `*Impl` 접미사는 **Repository 에만 한정**합니다 (JPA 기술 격리 목적). 일반 Service 는 구현체가 하나뿐이면 인터페이스를 만들지 않습니다.

### 2. DIP 의 실용주의 적용
- 도메인 모델에 JPA 어노테이션(`@Entity`, `@Table`, `@Column`) 사용을 허용합니다. ORM 교체 가능성이 거의 없고, 추가 매핑 레이어가 복잡도만 키우기 때문입니다.
- 단, **도메인 모델은 Spring Data Repository 인터페이스를 직접 상속하지 않습니다**. 도메인 Repository 인터페이스는 순수 추상화만 갖고, Impl 이 `XxxJpaRepository` 를 위임합니다.

### 3. 패키지 구성
- **계층 > 도메인** 구조를 유지합니다. (`interfaces/api/{domain}`, `application/{domain}`, `domain/{domain}`, `infrastructure/{domain}`)
- 같은 도메인 코드를 4개 레이어에 흩뿌리는 비용은 있지만, 레이어 경계와 의존방향을 한눈에 보기 좋습니다.

### 4. DTO 경계 (재강조)
- `interfaces/api/{domain}/XxxV1Dto` : 외부 노출용 요청/응답.
- `application/{domain}/XxxInfo` : 유즈케이스가 반환하는 표현용 결과.
- `domain/{domain}/XxxModel` : 영속 도메인 객체.
- 레이어 경계를 넘을 때 DTO/Info 매핑은 Controller / Facade 가 책임집니다.

## 개발 규칙

### 진행 Workflow - 증강 코딩
- **대원칙** : 방향성 및 주요 의사 결정은 개발자에게 **제안만** 합니다. 최종 승인된 사항을 기반으로만 작업을 수행합니다.
- **중간 결과 보고** : AI 가 반복 동작을 하거나 요청하지 않은 기능을 추가, 테스트 삭제/우회를 시도할 경우 개발자가 즉시 개입할 수 있도록 단계별 변경 사항을 명시합니다.
- **설계 주도권 유지** : 임의 판단으로 구조/명세를 바꾸지 않습니다. 대안은 제안 가능하나, 적용은 개발자 승인 후에만 진행합니다.
- **기존 패턴 준수** : 신규 기능은 기존 레이어 구조(`interfaces/application/domain/infrastructure`)와 명명 규칙(`XxxV1Controller`, `XxxFacade`, `XxxService`, `XxxModel`, `XxxJpaRepository`, `XxxRepositoryImpl`)을 따릅니다.

### 개발 Workflow - TDD (Red → Green → Refactor)
- 모든 테스트는 **3A 원칙**으로 작성합니다 (Arrange - Act - Assert).
- 커밋은 페이즈 단위로 분리합니다. 최근 커밋 메시지 컨벤션을 참고하세요.
  - `test: ... 실패 테스트 작성 (Red)`
  - `feat: ... 최소 구현 (Green)`
  - `refactor: ... 코드 정리 (Refactor)`
- **커밋 메시지에 Claude / AI 어시스턴트 표기를 포함하지 않습니다.** `Co-Authored-By: Claude ...`, `🤖 Generated with [Claude Code]`, 그 외 AI 자동 서명 푸터를 모두 제거하고, 사람이 작성한 형태의 메시지만 남깁니다. PR 본문에도 동일하게 적용합니다.

#### 1. Red Phase — 실패하는 테스트 먼저
- 요구사항을 만족하는 동작/엣지 케이스를 **먼저** 표현합니다.
- 단위 테스트는 `domain` 계층(Service/Model)부터, 통합 테스트는 `interfaces` 또는 `application` 계층에 둡니다.
- 의존성은 우선 `springmockk` (`@MockkBean` / `mockk()`) 를 기본으로 사용하고, 데이터가 필요한 경우 `instancio` 를 활용합니다.
- DB/Redis/Kafka 가 실제로 필요한 통합 테스트는 `testFixtures` (`modules:jpa`, `modules:redis`) 의 Testcontainers 베이스를 활용합니다.

#### 2. Green Phase — 통과시키는 **최소** 구현
- Red 의 테스트만 통과시킵니다. **오버 엔지니어링 금지** (추측성 API, 가상 케이스 핸들링 추가 금지).
- 검증 규칙은 도메인 모델/도메인 서비스에 두고, 컨트롤러는 입력 파싱·응답 매핑까지만 책임집니다.

#### 3. Refactor Phase — 정리/품질 개선
- 불필요한 `private` 헬퍼 지양, 객체지향적으로 응집도 ↑.
- 미사용 import 제거, `ktlintFormat` 수렴.
- 성능/가독성 개선 시 모든 테스트가 통과해야 합니다.
- 외부 동작이 변하면 안 됩니다 (테스트 추가 변경이 필요하면 별도 Red 사이클로).

## 주의사항

### 1. Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터로 시연용 응답을 만들어 두지 않습니다.
- null-unsafe 한 코드 작성 금지. **Kotlin 의 nullable 타입(`?`)/`requireNotNull`/`?:` 를 활용**하고, 플랫폼 타입을 그대로 노출하지 않습니다. (Java 상호운용 경계에서는 `@param:`/`@get:NotNull` 이나 검증 코드를 명시)
- 운영 코드에 `println` / 디버그용 출력 금지. 필요한 경우 `slf4j` 로깅을 사용합니다.
- 테스트를 삭제하거나 `@Disabled` 로 우회하여 빌드를 통과시키지 않습니다. 실패의 원인을 분석하고 코드(또는 테스트)를 올바르게 수정합니다.
- `git commit --no-verify`, `ktlint` 무시 등으로 hook 우회 금지.
- **커밋/PR 메시지에 Claude·AI 어시스턴트 서명을 절대 남기지 않습니다** (`Co-Authored-By: Claude ...`, `🤖 Generated with [Claude Code]` 등 일체 제외).
- DB 마이그레이션/스키마, 인프라 설정을 임의로 변경하지 않습니다 (요청 시 제안만).

### 2. Recommendation
- 실제 API 동작을 확인하는 **E2E 테스트** 를 권장합니다 (`@SpringBootTest` + Testcontainers).
- 재사용 가능한 객체/함수 설계 (도메인 모델 안에 불변 규칙을 응집).
- 성능 최적화 시 트레이드오프(메모리/락/N+1)를 코멘트가 아닌 PR 본문으로 설명합니다.
- API 가 완성되면 `http/<app-name>/<feature>-v{n}.http` 에 요청 샘플을 추가합니다 (예: `http/commerce-api/example-v1.http`).
- 도메인 검증은 도메인 모델 `init` 블록 또는 도메인 서비스에 위치시킵니다.

### 3. Priority
1. **실제 동작하는 해결책**만 고려합니다. 가짜 통과(`assertTrue(true)` 류)는 즉시 제거합니다.
2. **null-safety / thread-safety** 를 먼저 고려합니다. JPA 엔티티는 `protected set` 등 캡슐화를 유지합니다.
3. **테스트 가능한 구조**로 설계합니다. (생성자 주입, 인터페이스 기반 Repository, 부수효과 격리)
4. **기존 코드 패턴을 분석**한 후 일관성을 유지합니다. (레이어/명명/예외 처리)
5. 위 4가지가 충족된 이후에만 성능/유연성/확장성 개선을 검토합니다.

## 참고 위치
- 커밋 메시지 컨벤션 : `git log --oneline -20` 의 최근 이력
- 코드 스타일 : `.editorconfig`, `ktlint`(`./gradlew ktlintFormat`)
- 환경별 설정 : `apps/<app-name>/src/main/resources/application.yml`
- HTTP 요청 샘플 : `http/<app-name>/*.http`
- CI/CD 워크플로 : `.github/`
