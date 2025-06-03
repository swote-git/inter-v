# Multi-stage build for Spring Boot with Gradle
FROM openjdk:17-jdk-slim as builder

# 작업 디렉토리 설정
WORKDIR /build

# Gradle 관련 파일들 먼저 복사 (캐시 최적화)
COPY BE/inter-v/gradle ./gradle
COPY BE/inter-v/gradlew ./
COPY BE/inter-v/build.gradle* ./
COPY BE/inter-v/settings.gradle* ./

# 의존성 다운로드 (캐시 레이어)
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY BE/inter-v/src ./src

# 애플리케이션 빌드
RUN ./gradlew clean build -x test --no-daemon

# 실행 단계
FROM openjdk:17-jdk-slim

# 메타데이터
LABEL maintainer="inter-v-team"
LABEL description="inter-v Application"

# 애플리케이션 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 패키지 설치 (헬스체크용)
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 파일 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# 소유권 변경
RUN chown -R appuser:appuser /app

# 애플리케이션 사용자로 전환
USER appuser

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
