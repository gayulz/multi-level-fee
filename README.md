# SettleTree - 계층형 다단계 정산 및 승인 시스템

![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-5.0-blue?style=for-the-badge)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![H2](https://img.shields.io/badge/H2-Test_DB-blue?style=for-the-badge)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)

> **🌐 Live Demo**: [https://settletree.p-e.kr/](https://settletree.p-e.kr/)
> **개인 프로젝트이지만 실제 운영 중**인 웹 애플리케이션입니다. Oracle Cloud Infrastructure의 무료 티어에 배포하여 실제 서비스를 제공하고 있습니다.

## 💡 프로젝트 개요

SettleTree는 본사, 지사, 대리점으로 이어지는 **계층형 조직 구조(Hierarchical Organization)**를 기반으로 정산 요청을 처리하고 승인 워크플로우를 전산화한 웹 애플리케이션입니다.

다중 레벨로 구성된 조직에서는 하위 조직의 정산 요청이 상위 조직의 순차적인 검토를 거쳐야 하는 경우가 많습니다. 본 프로젝트는 이러한 비즈니스 요구사항을 분석하여, 각 계층의 역할을 분리하고 수수료가 상위로 분배되는 로직을 전산화하는 데 초점을 맞추었습니다.

### 주요 목적
* **승인 워크플로우의 시스템화**: 조직 레벨(Depth)에 따른 순차적 결재 프로세스 강제
* **정산 분배의 자동화**: 정해진 요율에 따라 발생한 수익을 각 조직 계층에 자동 분할 계산
* **수치 정합성 유지**: 시스템적 오차(소수점 등)로 인한 정산 유실을 방지하는 계산 체계 구현

---

## 🏗 아키텍처 및 구현 특징

### 시스템 흐름도 (System Architecture)
```mermaid
graph TD
    Client(["👤 Client (Web Browser)"])
    Nginx["🌐 Nginx (Reverse Proxy & SSL)"]
    App["🍃 Spring Boot App (SettleTree)"]
    DB[("🐘 PostgreSQL (Production)")]
    MQ{{"🐇 RabbitMQ (Message Broker)"}}

    Client -- "HTTPS" --> Nginx
    Nginx -- "Proxy (8080)" --> App
    App -- "JPA / QueryDSL" --> DB
    App -- "AMQP" --> MQ
    MQ -. "Asynchronous Task" .-> App
```

### 1. 계층형 조직의 조회 및 성능 최적화
* **Self-Referencing 구조**: 하나의 `Organization` 및 `SettlementNode` 테이블이 부모-자식 관계를 맺는 트리 형태로 설계되었습니다.
* **N+1 문제 방지 (QueryDSL Fetch Join)**: 깊이가 있는 트리를 순회할 때 JPA의 지연 로딩(Lazy Loading)으로 인해 쿼리가 기하급수적으로 발생하는 문제를 방지하고자, QueryDSL을 통한 `fetchJoin`을 적용해 계층 정보를 효율적으로 로드합니다.

### 2. 수수료 분배 체계 및 정합성 (낙전 보정 알고리즘)

**조직 계층 구조도 (Organization Hierarchy)**
```mermaid
graph TD
    HQ["🏢 본사 (Level 1)"]
    BranchA["🏙 지사 A (Level 2)"]
    BranchB["🏙 지사 B (Level 2)"]
    AgencyA1["🏪 대리점 A-1 (Level 3)"]
    AgencyA2["🏪 대리점 A-2 (Level 3)"]

    HQ -->|5%| BranchA
    HQ -->|5%| BranchB
    BranchA -->|2%| AgencyA1
    BranchA -->|2%| AgencyA2

    style HQ fill:#ffcccc,stroke:#ff6666,stroke-width:2px
    style BranchA fill:#ccddff,stroke:#6699ff,stroke-width:2px
    style BranchB fill:#ccddff,stroke:#6699ff,stroke-width:2px
```

정산에서 가장 중요한 부분은 "원금과 분배금의 합이 정확히 일치해야 한다"는 점입니다. 수수료를 % 단위로 하위 조직으로 분배하다 보면, 소수점 이하의 금액에서 오차(Dust, 낙전)가 발생합니다.

* **DFS (깊이 우선 탐색)**: 본사에서 대리점 끝단의 노드까지 트리를 탐색하며 수수료율에 따른 금액을 분할합니다.
* **절삭 로직 (`RoundingMode.DOWN`)**: 모든 노드에서는 분배금을 무조건 소수점 아래에서 내림(절삭) 처리합니다.
* **본사 보정**: 탐색 후 남은 1원 단위의 잔액은 트리의 가장 최상단(본사)에 강제 귀속하여 전체 수치 정합성(100%)을 맞춥니다.

### 3. Role/조직 레벨별 접근 제어 (Security)
Spring Security의 URL 차단과 `@PreAuthorize` 어노테이션을 결합하여 권한 검증을 처리합니다.
* **USER, ADMIN, SUPER_ADMIN**: 세 단계의 권한 모델을 사용합니다.
* 일반 사용자는 본인의 건만 볼 수 있고, 중간 관리자(지사/대리점 ADMIN)는 자신보다 하위 조직의 데이터만 승인하고 열람할 수 있도록 격리하였습니다.

### 4. 클라우드 인프라 운용 문제 해결 (OCI Free Tier)
비용 효율적인 배포를 위해 Oracle Cloud Infrastructure (OCI)의 무료 티어(1/8 OCPU, 1GB RAM)에 시스템을 구축하는 과정에서 발생한 메모리 한계(OOM)를 다음과 같이 해결했습니다.

1. **Swap Memory 할당**: 1GB의 제한된 물리 메모리를 우회하기 위해 2GB의 Swap 파일을 `/etc/fstab`에 마운트하여 Spring Boot, DB, Nginx, RabbitMQ의 동시 구동을 보장했습니다.
2. **도커 빌드 최적화**: OCI 상의 CI/CD 과정에서 Gradle 빌드 시 불필요한 `-plain.jar`가 남는 현상을 방지하기 위해 `jar { enabled = false }` 처리를 진행했습니다.
3. **Nginx 기반 HTTPS 프록시**: Certbot을 연동하여 HTTPS 암호화를 구현하고 80포트를 443으로 리다이렉트하는 표준적인 웹 인프라 환경을 구축했습니다.

### 5. 최근 기능 개선 사항

#### 대시보드 하이브리드 조직도 개편
* **수평+수직 트리 전환**: 기존 조직도를 하이브리드 레이아웃으로 개선하여 가독성을 향상시켰습니다.
* **용어 개선**: 사용자 편의성을 위해 "트랜잭션 → 주문", "Tx ID → 주문 ID"로 용어를 변경했습니다.
* **실제 주문 ID 노출**: 대시보드 테이블에서 실제 주문 ID(orderId)를 노출하도록 DTO 필드를 추가했습니다.

#### UI/UX 개선
* **푸터 영역 레이아웃 수정**: 사이드바에 가려지던 푸터 영역을 Flexbox 기반으로 재배치하여 문제를 해결했습니다.
* **정산 내역 링크 수정**: 대시보드에서 정산 내역 바로가기 링크의 404 오류를 수정했습니다(`/settlements` → `/settlement/history`).

#### 보안 강화
* **더미 데이터 초기화 분리**: 평문 비밀번호 설정을 `application.yml`에서 제거하고 `application-local.yml`로 이동하여 운영 환경에서의 보안 리스크를 제거했습니다.
* **프로파일 기반 초기화**: `DataInitializer`는 local/dev 프로파일에서만 실행되도록 제한했습니다.

#### 개발 편의성 향상
* **Live Reload 환경 구성**: Thymeleaf 캐시 비활성화 및 `bootRun sourceResources` 설정을 통해 템플릿/정적 자원의 실시간 리로드를 지원합니다.

#### 조직 관리 기능 고도화
* **하위 조직 관리 개선**: 계층형 조직 구조에서 하위 조직을 더 효율적으로 관리할 수 있도록 기능을 개선했습니다.

---

## 🛠 기술 스택

### Backend
- **Java 17** 
- **Spring Boot 3.3.2**
- **Spring Security 6.x** 
- **Spring Data JPA** 
- **QueryDSL 5.0** 
- **RabbitMQ** (비동기 메시징 지원)
- **MapStruct 1.5.5**

### Database
- **PostgreSQL** (Production 환경)
- **H2** (테스트 및 로컬 메모리 DB)

### Frontend
- **Thymeleaf + Bootstrap 5** (HTML 서버 사이드 렌더링)
- **SUIT 폰트 / 다크모드 대응** 등 일반적인 CSS, JS 기반 UI 구현

### Infrastructure & CI/CD
- **Oracle Cloud Infrastructure (OCI)** 
- **Docker / Docker Compose**
- **Nginx (SSL/HTTPS)**
- **GitHub Actions** 

---

## 🚀 로컬 환경 실행 가이드

```bash
# 1. RabbitMQ 백그라운드 실행 (도커 필요)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management

# 2. 로컬 프로파일(H2, Local RabbitMQ 의존) 지정하여 앱 실행
./gradlew clean build -x test
./gradlew bootRun --args='--spring.profiles.active=local'
```

- **웹 애플리케이션 접속**: http://localhost:8080/
- **H2 DB 접속 경로**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa` (비밀번호 없음)
- **RabbitMQ 관리 도구**: http://localhost:15672 (아이디/비번: guest/guest)

---

## 📝 관리자 참고용 기초 세팅

### 더미 데이터 초기화 (로컬 환경 전용)
로컬 프로파일(`local`) 모드로 부팅 시, 더미 데이터 구성을 위해 아래 계정이 자동 생성됩니다.

**⚠️ 보안 주의사항**:
- 더미 데이터 초기화는 `application-local.yml`에서만 설정되며, **운영 환경에서는 실행되지 않습니다**.
- `DataInitializer`는 `local` 또는 `dev` 프로파일에서만 활성화됩니다.
- 비밀번호는 `application-local.yml`에서 환경 변수로 주입 가능합니다.

**자동 생성 계정**:
- **최고 관리자**: `admin@settletree.com`
- **본사 관리자**: `hq_admin@settletree.com`
- **서울지사 관리자**: `seoul_admin@settletree.com`
- **강남대리점 관리자**: `gangnam_admin@settletree.com`
- **강남대리점 일반사용자**: `gangnam_user@example.com`

### 개발 환경 설정
**Live Reload 지원**:
- Thymeleaf 캐시가 비활성화되어 있어 템플릿 수정 시 재시작 없이 반영됩니다.
- `bootRun`에서 `sourceResources` 설정으로 정적 자원도 실시간 감시됩니다.
- IDE에서 "Build Project" 또는 자동 빌드 활성화 시 즉시 반영됩니다.

---

## 👤 연락처 및 포트폴리오
* **개발자**: 김가율 (gayul.kim)
* **이메일**: gayulz@kakao.com
* **GitHub**: https://github.com/gayulz/multi-level-fee
* **🌐 Live Demo (운영 중)**: https://settletree.p-e.kr/

---

## 🎯 프로젝트 특징 요약

| 특징 | 설명 |
|------|------|
| **핵심 알고리즘** | DFS 재귀 기반 정산 분배 + 낙전 보정 알고리즘 |
| **성능 최적화** | QueryDSL Fetch Join으로 N+1 문제 해결 |
| **보안** | Spring Security 6.x 기반 Role/계층별 접근 제어 |
| **인프라** | OCI Free Tier 1GB RAM 환경에서 안정적 운영 |
| **비동기 메시징** | RabbitMQ 기반 정산 요청 처리 |
| **개발 편의성** | Live Reload 지원, 로컬 환경 더미 데이터 자동 생성 |
| **운영 환경** | Docker + Nginx + HTTPS(Certbot) 완전 자동화 배포 |

---

## 📊 프로젝트 구조

```
src/main/java/com/example/settlement/
├── config/              # 설정 클래스 (Security, QueryDSL, RabbitMQ)
├── controller/          # 웹 컨트롤러
├── domain/
│   ├── entity/         # JPA 엔티티 (Organization, User, SettlementRequest, SettlementNode)
│   └── repository/     # Repository 인터페이스 및 QueryDSL 구현체
├── dto/                # 데이터 전송 객체
├── service/            # 비즈니스 로직 (정산, 승인, 조직 관리)
├── messaging/          # RabbitMQ Producer/Consumer
├── exception/          # 예외 처리
└── web/
    ├── controller/     # 뷰 컨트롤러 (대시보드, 설정)
    └── security/       # 커스텀 UserDetails 구현
```

---

## 🚀 주요 기술적 도전과 해결

### 1. 정산 정합성 문제
**문제**: 소수점 연산으로 인한 금액 유실 (낙전)
**해결**: DFS 트리 순회 + `RoundingMode.DOWN` + 루트 노드 보정 알고리즘

### 2. N+1 쿼리 문제
**문제**: 계층형 조직 조회 시 쿼리 폭발
**해결**: QueryDSL `fetchJoin`으로 필요한 연관 엔티티를 한 번에 로딩

### 3. 제한된 인프라 자원
**문제**: OCI Free Tier 1GB RAM에서 OOM 발생
**해결**: 2GB Swap 메모리 + Docker 최적화 + Gradle 설정 개선

### 4. 보안 취약점
**문제**: 평문 비밀번호가 공유 설정 파일에 노출
**해결**: 프로파일 분리 (`application-local.yml`로 격리)

---

## 📈 향후 개선 계획

- [ ] **정산 승인 워크플로우 강화**: 다단계 승인 체인 구현
- [ ] **실시간 알림**: WebSocket 기반 정산 상태 변경 알림
- [ ] **대시보드 차트**: 조직별/기간별 정산 통계 시각화
- [ ] **API 문서화**: Swagger/OpenAPI 통합
- [ ] **테스트 커버리지 확대**: 통합 테스트 및 E2E 테스트 보강
- [ ] **모니터링**: Prometheus + Grafana 연동

---

## 🏆 학습 성과

이 프로젝트를 통해 다음을 학습하고 구현했습니다:

✅ **계층형 자료구조 설계 및 재귀 알고리즘**
✅ **QueryDSL을 활용한 복잡한 쿼리 최적화**
✅ **Spring Security 기반 다단계 권한 제어**
✅ **메시지 큐(RabbitMQ)를 활용한 비동기 처리**
✅ **제한된 클라우드 자원에서의 실전 배포 및 운영**
✅ **Docker/Nginx/Certbot을 활용한 프로덕션 인프라 구축**
✅ **Thymeleaf + Bootstrap 5를 이용한 SSR 웹 UI 구현**
