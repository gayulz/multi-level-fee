# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어 및 커뮤니케이션 규칙

- **기본 응답 언어**: 한국어
- **코드 주석**: 한국어로 작성
- **커밋 메시지**: 한국어로 작성
- **문서화**: 한국어로 작성
- **변수명/함수명**: 영어 (코드 표준 준수)

## 프로젝트 개요

**SettleTree** - 계층형 조직 기반 다단계 정산 승인 시스템

레거시 시스템(Java 1.7 모놀리식)에서 정산 로직을 분리하여 현대적 기술 스택으로 재구축하는 MSA Pilot 프로젝트입니다.

### 핵심 특징

- **조직 계층 구조**: 본사 → 지사 → 대리점의 3단계 트리 구조
- **단계별 승인 프로세스**: 조직 레벨에 따른 다단계 정산 승인 워크플로우
- **권한 기반 접근 제어**: Spring Security 기반 3단계 권한 체계 (SUPER_ADMIN, ADMIN, USER)
- **글래스모피즘 디자인**: Liquid Glass 효과와 그라데이션 포인트 컬러 적용

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.3.2 |
| Build | Gradle 8.8 (Groovy) |
| Security | Spring Security 6.x |
| ORM | Spring Data JPA |
| Query | QueryDSL 5.0 |
| Messaging | RabbitMQ (Direct Exchange) |
| Database | PostgreSQL (Production), H2 (Test) |
| Frontend | Thymeleaf + Bootstrap 5 + Thymeleaf Layout Dialect |
| Design | SUIT 폰트, 글래스모피즘 |
| DTO Mapping | MapStruct 1.5.5 |
| Validation | Spring Validation |
| Test | @SpringBootTest (통합 테스트), @MockBean (RabbitMQ 격리) |

## 아키텍처

### 시스템 구조

**Strangler Fig Pattern** 적용:
```
Legacy Client → Legacy System → RabbitMQ → Settlement Engine → DB
```

### 도메인 계층 구조

```
Organization (조직 트리)
├── Headquarters (본사, level=1)
│   ├── Branch (지사, level=2)
│   │   └── Agency (대리점, level=3)
│   └── Branch (지사, level=2)
│       └── Agency (대리점, level=3)

User (사용자)
├── ROLE_SUPER_ADMIN (시스템 최고 관리자)
├── ROLE_ADMIN (조직별 관리자)
└── ROLE_USER (일반 사용자)

SettlementRequest (정산 요청)
└── 승인 단계: Agency Admin → Branch Admin → HQ Admin → COMPLETED
```

### 패키지 구조

```
src/main/java/com/example/settlement/
├── SettlementApplication.java
├── config/
│   ├── SecurityConfig.java           # Spring Security 설정
│   ├── RabbitMqConfig.java
│   └── QueryDslConfig.java
├── domain/
│   ├── entity/
│   │   ├── Organization.java         # 조직 (Self-Reference Tree)
│   │   ├── User.java                 # 사용자
│   │   ├── SettlementRequest.java    # 정산 요청
│   │   └── SettlementNode.java       # 정산 노드 (기존)
│   └── repository/
│       ├── OrganizationRepository.java
│       ├── UserRepository.java
│       ├── SettlementRequestRepository.java
│       └── SettlementNodeRepository.java
├── dto/
│   ├── request/
│   │   ├── SignupRequest.java
│   │   ├── SettlementRequestDto.java
│   │   └── ApprovalRequest.java
│   └── response/
│       ├── UserResponse.java
│       ├── SettlementResponse.java
│       └── DashboardResponse.java
├── service/
│   ├── OrganizationService.java      # 조직 관리
│   ├── UserService.java              # 사용자 관리
│   ├── SettlementService.java        # 정산 로직
│   ├── ApprovalService.java          # 승인 프로세스
│   └── AuthService.java              # 인증/인가
├── web/
│   ├── controller/
│   │   ├── AuthController.java       # 로그인, 회원가입
│   │   ├── DashboardController.java
│   │   ├── NodeController.java
│   │   ├── SettlementController.java
│   │   └── AdminController.java      # 권한 관리
│   └── security/
│       ├── CustomUserDetails.java
│       └── CustomUserDetailsService.java
├── messaging/
│   ├── RabbitMqConsumer.java
│   └── RabbitMqProducer.java
└── exception/
    ├── CustomException.java
    └── GlobalExceptionHandler.java
```

### 프론트엔드 구조

```
src/main/resources/
├── templates/
│   ├── auth/
│   │   ├── login.html                # 로그인
│   │   ├── signup.html               # 회원가입 (이메일 인증)
│   │   └── welcome.html              # 웰컴 페이지
│   ├── layout/
│   │   ├── base.html                 # 공통 레이아웃
│   │   ├── header.html
│   │   ├── sidebar.html              # 권한별 메뉴 표시
│   │   └── footer.html
│   └── pages/
│       ├── dashboard.html            # 대시보드
│       ├── nodes/
│       │   ├── list.html             # 노드 현황
│       │   └── detail.html
│       ├── settlement/
│       │   ├── request.html          # 정산 요청 (사용자별 다른 화면)
│       │   ├── history.html          # 정산 내역
│       │   └── detail.html
│       ├── admin/
│       │   └── user-management.html  # 권한 설정
│       └── settings/
│           └── profile.html          # 개인정보 수정
└── static/
    ├── css/
    │   ├── welcome.css               # 웰컴 페이지 스타일 (글래스모피즘)
    │   ├── common.css                # 공통 스타일
    │   └── components/
    │       ├── glass-panel.css       # 글래스 패널 컴포넌트
    │       ├── gradient-button.css   # 그라데이션 버튼
    │       └── sidebar.css
    ├── js/
    │   ├── main.js
    │   ├── dashboard.js
    │   └── settlement.js
    └── fonts/
        └── SUIT/                     # SUIT 폰트 파일
```

## 개발 순서

**사용자/조직/권한 중심 개발 전략**:

1. **Phase 1**: 환경 구성 (JDK, Docker/PostgreSQL/RabbitMQ, Git)
2. **Phase 2**: DB 설계 및 엔티티 개발 (Organization, User, SettlementRequest)
3. **Phase 3**: Spring Security 설정 및 인증/인가 구현
4. **Phase 4**: 웰컴/로그인/회원가입 페이지 (글래스모피즘 디자인)
5. **Phase 5**: 조직 관리 및 승인 프로세스 개발
6. **Phase 6**: 정산 요청/승인 워크플로우 구현
7. **Phase 7**: 권한별 대시보드 및 관리 페이지
8. **Phase 8**: RabbitMQ 메시징 연동
9. **Phase 9**: 테스트 및 문서화

## 빌드 및 실행

### 환경 프로파일

| 프로파일 | 설명 | DB | RabbitMQ |
|---------|------|----|----|
| `local` | 로컬 개발 환경 | H2 | localhost:5672 |
| `test` | 테스트 환경 | H2 (in-memory) | Mock (@MockBean) |
| `prod` | 운영 환경 | PostgreSQL | 외부 서버 |

### 빌드 및 실행 명령어

```bash
# 빌드 (테스트 제외)
./gradlew clean build -x test

# 테스트 실행 (RabbitMQ Mock으로 격리)
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "OrganizationRepositoryTest"

# QueryDSL Q클래스 생성
./gradlew compileJava
# build/generated/querydsl 경로에 Q클래스 생성됨

# 애플리케이션 실행 (local 프로파일)
./gradlew bootRun --args='--spring.profiles.active=local'

# 애플리케이션 실행 (prod 프로파일)
./gradlew bootRun --args='--spring.profiles.active=prod'

# Docker Compose로 전체 환경 실행 (PostgreSQL + RabbitMQ)
docker-compose up -d

# RabbitMQ Management UI
# http://localhost:15672 (guest/guest)

# H2 Console (local 프로파일 실행 시)
# http://localhost:8080/h2-console
```

## 핵심 도메인 개념

### 핵심 알고리즘: 정산 분배 로직

**DFS(깊이 우선 탐색) 기반 하향식 분배**:
1. 원금에서 각 노드의 수수료율(%)을 우선 취득
2. 잔여 금액을 하위 노드 개수(N)로 균등 분할 (1/N)
3. 소수점은 `RoundingMode.FLOOR`로 내림 처리
4. 순회 완료 후 남은 낙전(Dust)을 루트 노드에 귀속

**예시**: 10,000원 / 본사(10%) → 지사2(각 5%) → 대리점4(각 3%)
- 본사: 1,000원 + 낙전 8,294원 = **9,294원**
- 지사: 각 225원 (총 450원)
- 대리점: 각 64원 (총 256원)
- **합계: 10,000원 (100% 정합성 보장)**

### Organization (조직)
- Self-Reference Tree 구조 (`parent`, `children`)
- 조직 유형: `HEADQUARTERS`, `BRANCH`, `AGENCY`
- 레벨: 1(본사), 2(지사), 3(대리점)
- 비즈니스 메서드: `addChildOrg()`, `getAncestors()`, `isRoot()`, `getLevel()`

### User (사용자)
- 소속 조직(`org_id`) 연결
- 권한: `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_USER`
- 가입 승인 워크플로우: PENDING → APPROVED/REJECTED
- 비즈니스 메서드: `hasRole()`, `canApprove(SettlementRequest)`, `isInOrganization(org)`

### SettlementRequest (정산 요청)
- 요청자 조직 레벨에 따른 승인 단계 결정
- 승인 상태: `PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`
- 승인 단계 추적: `current_approval_level` (1~3)
- 각 단계별 승인자 및 승인일시 기록

### 승인 프로세스 로직

**Case 1: 대리점(level=3) 일반 사용자**
```
요청 → 대리점 관리자 승인 → 지사 관리자 승인 → 본사 관리자 승인 → 완료
```

**Case 2: 지사(level=2) 일반 사용자**
```
요청 → 지사 관리자 승인 → 본사 관리자 승인 → 완료
```

**Case 3: 본사(level=1) 일반 사용자**
```
요청 → 본사 관리자 승인 → 완료
```

### SettlementNode (정산 노드 - 기존)
- Organization과 1:1 매핑 관계
- 수수료율(`feeRate`)을 통한 수수료 계산
- DFS(깊이 우선 탐색)로 트리 순회 및 낙전(Dust) 보정

## 디자인 시스템

### 컬러 팔레트

```css
/* 베이스 컬러 */
--base-dark: #0a0a0a;
--base-navy: #19065a;

/* 포인트 컬러 */
--point-pink: #f05cfa;
--point-orange: #d76750;

/* 그라데이션 */
--gradient-primary: linear-gradient(135deg, #19065a, #f05cfa);
--gradient-secondary: linear-gradient(135deg, #d76750, #f05cfa);

/* 글래스모피즘 */
--glass-bg: rgba(255, 255, 255, 0.1);
--glass-border: rgba(255, 255, 255, 0.2);
--glass-backdrop-blur: blur(10px);
```

### 타이포그래피

- **기본 폰트**: SUIT (전역 적용)
- **폰트 크기**: 16px (기본), 제목은 24px~48px

### 주요 UI 컴포넌트

**글래스 패널**:
```css
.glass-panel {
	background: var(--glass-bg);
	border: 1px solid var(--glass-border);
	border-radius: 16px;
	backdrop-filter: var(--glass-backdrop-blur);
	box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}
```

**그라데이션 버튼**:
```css
.btn-gradient {
	background: var(--gradient-primary);
	border: none;
	border-radius: 8px;
	transition: transform 0.2s, box-shadow 0.2s;
}

.btn-gradient:hover {
	transform: translateY(-2px);
	box-shadow: 0 8px 24px rgba(240, 92, 250, 0.4);
}
```

## 권한별 페이지 접근 제어

### Dashboard
- **ROLE_USER**: 본인 정산 내역만 조회
- **ROLE_ADMIN**: 소속 조직 + 하위 조직 정산 내역 조회, 활성 유저 통계
- **ROLE_SUPER_ADMIN**: 전체 조직 정산 내역 조회, 전체 활성 유저 통계

### 노드 현황
- **ROLE_USER**: 접근 불가
- **ROLE_ADMIN**: 소속 조직 트리 조회만 가능 (읽기 전용)
- **ROLE_SUPER_ADMIN**: 전체 조직 트리 조회/생성/수정/삭제

### 정산 요청
- **ROLE_USER**: 신규 정산 요청 등록, 본인 요청만 조회
- **ROLE_ADMIN**:
	- 소속 조직의 하위 사용자 요청 목록 조회
	- 승인/반려 처리 (해당 레벨의 승인 권한)
- **ROLE_SUPER_ADMIN**: 전체 정산 요청 조회 및 최종 승인

### 정산 내역
- **ROLE_USER**: 본인 요청 내역만 조회
- **ROLE_ADMIN**: 소속 조직 + 하위 조직 내역 조회, 필터링 및 검색
- **ROLE_SUPER_ADMIN**: 전체 내역 조회, 고급 필터링 및 통계

### 권한 설정 (신규)
- **ROLE_USER**: 접근 불가
- **ROLE_ADMIN**: 소속 조직 회원 목록 조회 (읽기 전용)
- **ROLE_SUPER_ADMIN**: 전체 회원 조회/권한 변경/삭제

### 설정
- **전체 권한**: 본인 개인정보 수정 (이메일 변경 불가)

## RabbitMQ 설정

- Exchange: `settlement.direct` (Direct Exchange)
- Queue: `settlement.queue`
- Routing Key: `settlement.req`

## QueryDSL 설정

프로젝트 생성 후 `build.gradle`에 수동 추가:

```groovy
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
```

Q클래스 생성:
```bash
./gradlew compileJava
# build/generated/querydsl 경로에 Q클래스 생성됨
```

## MapStruct 설정

DTO 변환을 위한 MapStruct 설정:

```groovy
implementation 'org.mapstruct:mapstruct:1.5.5.Final'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
```

**주의**: Lombok과 함께 사용 시 `lombok-mapstruct-binding` 필수

## 테스트 전략

- **통합 테스트 선호**: Mockito 대신 `@SpringBootTest` 기반 통합 테스트 선호
- **실제 DB 연동**: H2 인메모리 DB로 Repository 검증
- **외부 의존성 격리**: RabbitMQ는 `@MockBean`으로 격리하여 빠른 빌드 (5초 이내)
  - `RabbitTemplate`, `RabbitListener` Mock 처리
  - `Mockito.verify()`로 메시지 발송 여부만 검증
  - CI/CD 환경에서 외부 인프라 없이 테스트 가능
- **N+1 문제 검증**: QueryDSL `fetchJoin`으로 해결 확인
- **보안 테스트**: `@WithMockUser`로 권한별 접근 제어 검증

## 회원가입 프로세스

### 1. 회원가입 폼 작성
- 이메일 주소 (ID 역할)
- 비밀번호 (특수문자 1개 이상, 영어 대문자 1개 이상, 최소 8자리)
- 연락처 (핸드폰 번호)
- 소속 (드롭다운: DB에 등록된 모든 조직 표시)
- 개인정보 동의 체크박스 (필수)

### 2. 이메일 인증
- 회원가입 버튼 클릭 시 인증 메일 발송
- 5분 제한시간 내 인증 링크 클릭
- 인증 완료 후 가입 요청 상태로 전환

### 3. 관리자 승인
- 해당 조직의 관리자에게 승인 요청 알림
- 관리자가 "권한 설정" 페이지에서 승인/반려 처리
- 승인 시 계정 활성화 및 로그인 가능

### 4. 로그인
- 이메일 + 비밀번호로 로그인
- Spring Security 세션 기반 인증
- 권한에 따라 사이드바 메뉴 차등 표시

## 보안 고려사항

- **비밀번호 암호화**: BCryptPasswordEncoder 사용
- **XSS 방어**: 특수문자 필터링 (비밀번호 입력 시 XSS 공격 가능 특문 제외)
- **CSRF 보호**: Spring Security CSRF 토큰 적용
- **SQL Injection 방어**: JPA Prepared Statement 사용
- **권한 검증**: @PreAuthorize 어노테이션으로 메서드 레벨 보안

## 주요 트러블슈팅

### 1. N+1 문제 해결 (QueryDSL Fetch Join)
- **문제**: 계층 깊이만큼 SELECT 쿼리 반복 발생
- **해결**: `leftJoin().fetchJoin()`으로 한 번에 로드 (쿼리 1회)

### 2. 소수점 낙전 누수 방지
- **문제**: 1/N 분배 시 소수점 오차로 전체 합계 불일치
- **해결**: `BigDecimal` + `RoundingMode.FLOOR` + 낙전 보정 알고리즘

### 3. RabbitMQ 테스트 격리
- **문제**: CI 환경에서 RabbitMQ 커넥션 타임아웃으로 빌드 블로킹
- **해결**: `@MockBean`으로 격리, 빌드 시간 30초 → 5초 단축

### 4. 다계층 권한 제어
- **문제**: 본사/지사/대리점별 권한 구분 필요
- **해결**: Spring Security + `@PreAuthorize` 2단계 방어

## 참고 문서

- `docs/DATABASE_DESIGN.md`: DB 스키마 및 ERD
- `docs/AUTH_DESIGN.md`: 권한 체계 및 Spring Security 설정
- `docs/ARCHITECTURE.md`: 시스템 아키텍처 및 비즈니스 로직
- `docs/PRD.md`: Product Requirement Document (상세 요구사항)
- `docs/ROADMAP.md`: 단계별 Task 정의 및 체크리스트
- `.claude/rules/`: 코딩 스타일, 아키텍처, 보안 등 상세 규칙

## 코딩 컨벤션

- **들여쓰기**: 탭(Tab) 1번 사용
- **주석**: 한국어로 작성
- **커밋 메시지**: 한국어, AI 작성 표시 금지
- **변수명**: camelCase (예: `userName`, `orgId`)
- **클래스명**: PascalCase (예: `UserService`, `SettlementRequest`)
- **상수명**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`)

## 금지 사항

- Deprecated 라이브러리/API 제안 금지
- 설명 없이 코드만 제공 금지
- 존재하지 않는 클래스/메서드 가정 금지
- 요청 없는 대규모 아키텍처 변경 금지
- 코드 파일에 이모지 사용 금지
- 커밋 메시지에 `Co-Authored-By: Claude` 등 AI 표시 금지
- 들여쓰기는 탭(Tab) 1번만 사용 (스페이스 금지)
