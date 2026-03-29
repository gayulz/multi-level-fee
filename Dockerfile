# =====================================================================
# 단일 실행 스테이지 - 이미 빌드된 JAR를 복사하여 경량 이미지로 실행
# GitHub Actions 에서 SCP 가 전달해준 JAR 활용 (서버 내 빌드 타임아웃 방지)
# =====================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# GitHub Actions의 SCP를 통해 서버로 전달된 JAR 파일을 컨테이너 내부로 복사
COPY build/libs/*.jar app.jar

# 컨테이너 포트 개방
EXPOSE 8080

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx256m -Xms128m"

# 실행 명령어
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
