# =====================================================================
# [Stage 1] 빌드 스테이지 - Gradle로 JAR 파일 생성
# 서버에 Java가 없어도 Docker 컨테이너 내부에서 빌드 수행
# =====================================================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 의존성 캐시 레이어 분리 (소스 변경 시 의존성 재다운로드 방지)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x ./gradlew

# 의존성 먼저 다운로드 (캐시 활용)
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# =====================================================================
# [Stage 2] 실행 스테이지 - JRE만 포함한 경량 이미지로 실행
# =====================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Stage 1에서 생성된 JAR만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 포트 개방
EXPOSE 8080

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx256m -Xms128m"

# 실행 명령어
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
