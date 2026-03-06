# GitHub Actions에서 빌드된 JAR 파일을 받아 실행하는 Runtime 전용 Dockerfile
# (Gradle 빌드는 GitHub Actions에서 수행)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# GitHub Actions에서 빌드 후 서버로 전송된 JAR 파일을 복사
COPY build/libs/*.jar app.jar

# 컨테이너 포트 개방
EXPOSE 8080

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx256m -Xms128m"

# 실행 명령어
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
