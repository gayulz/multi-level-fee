# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**Tree-based Multi-tier Settlement Engine** - 트리 기반 다단계 정산 엔진

레거시 시스템(Java 1.7 모놀리식)에서 정산 로직을 분리하여 현대적 기술 스택으로 재구축하는 MSA Pilot 프로젝트입니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.2.x |
| Build | Gradle (Groovy) |
| ORM | Spring Data JPA |
| Query | QueryDSL 5.0 |
| Messaging | RabbitMQ (Direct Exchange) |
| Frontend | Thymeleaf + Bootstrap 5 |
| Test | @SpringBootTest (통합 테스트) |

## 아키텍처

**Strangler Fig Pattern** 적용:
```
Legacy Client → Legacy System → RabbitMQ → Settlement Engine → DB
```

### 패키지 구조 (예정)
```
src/main/java/com/example/settlement/
├── SettlementApplication.java
├── config/                    # RabbitMqConfig, QueryDslConfig
├── domain/
│   ├── entity/               # SettlementNode (Self-Reference Tree)
│   └── repository/           # JPA + QueryDSL Custom Repository
├── dto/                       # Java Record (불변 DTO)
├── service/                   # 재귀 정산 알고리즘
├── messaging/                 # RabbitMQ Consumer/Producer
└── exception/                 # Custom Exception + GlobalExceptionHandler
```

### 프론트엔드 구조 (예정)
```
src/main/resources/
├── templates/
│   ├── layout/               # 공통 레이아웃, header, sidebar, footer
│   └── pages/                # dashboard, nodes/, settlement/
└── static/
    ├── css/style.css
    └── js/main.js
```

## 개발 순서

**Frontend First** 전략: 정적 페이지(Mock 데이터)를 먼저 완성한 후 백엔드를 구현하여 연동

1. Phase 1: 환경 구성 (JDK, Docker/RabbitMQ, Git)
2. Phase 2: 프론트엔드 정적 페이지 (Thymeleaf + Mock 데이터)
3. Phase 3: 설정 (Gradle, Profile, RabbitMQ Config, QueryDSL)
4. Phase 4: 도메인 (Entity, DTO, Repository)
5. Phase 5: 비즈니스 (정산 서비스, 재귀 알고리즘)
6. Phase 6: 인터페이스 (RabbitMQ Consumer/Producer)
7. Phase 7: 마무리 및 문서화

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "SettlementNodeRepositoryTest"

# 애플리케이션 실행 (local 프로파일)
./gradlew bootRun --args='--spring.profiles.active=local'

# RabbitMQ 컨테이너 실행
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

## 핵심 도메인 개념

### SettlementNode (정산 노드)
- Self-Reference Tree 구조 (`parent`, `children`)
- 수수료율(`feeRate`)을 통한 수수료 계산
- 비즈니스 메서드: `calculateFee(amount)`, `addChild(node)`, `isRoot()`, `isLeaf()`

### 정산 알고리즘
- DFS(깊이 우선 탐색)로 트리 순회
- 각 노드에서 `amount * feeRate` 계산 (Floor, 소수점 절삭)
- 낙전(Dust) 보정: 전체 수수료 합과 원금의 차이를 루트 노드에 할당

### RabbitMQ 설정
- Exchange: `settlement.direct` (Direct Exchange)
- Queue: `settlement.queue`
- Routing Key: `settlement.req`

## QueryDSL 설정

프로젝트 생성 후 `build.gradle`에 수동 추가 필요:

```groovy
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
annotationProcessor "jakarta.persistence:jakarta.persistence-api"
annotationProcessor "jakarta.annotation:jakarta.annotation-api"
```

Q클래스 생성:
```bash
./gradlew compileJava
# build/generated/querydsl 경로에 Q클래스 생성됨
```

## 테스트 전략

- **단위 테스트 지양**: Mockito 대신 `@SpringBootTest` 기반 통합 테스트 선호
- **실제 DB 연동**: H2 인메모리 DB로 Repository 검증
- **N+1 문제 검증**: QueryDSL `fetchJoin`으로 해결 확인

## 참고 문서

- `ROADMAP.md`: 단계별 Task 정의 및 체크리스트 (shrimp-task-manager용)
- `docs/PRD.md`: Product Requirement Document (상세 요구사항)
