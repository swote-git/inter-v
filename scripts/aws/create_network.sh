#!/bin/bash

echo "🔍 프로젝트 구조 및 Dockerfile 분석"
echo "==================================="

# 1. 현재 프로젝트 구조 확인
echo "📂 현재 디렉토리 구조:"
echo "현재 위치: $(pwd)"
echo ""

# Dockerfile 위치 확인
if [ -f "Dockerfile" ]; then
  echo "✅ Root에 Dockerfile 발견"
else
  echo "❌ Root에 Dockerfile이 없습니다."
  exit 1
fi

# BE 프로젝트 구조 확인
echo ""
echo "🔍 BE 프로젝트 구조 확인:"

if [ -d "BE/inter-v" ]; then
  echo "✅ BE/inter-v/ 디렉토리 발견"
  BE_PATH="BE/inter-v"
elif [ -d "inter-v" ]; then
  echo "✅ inter-v/ 디렉토리 발견"
  BE_PATH="inter-v"
else
  echo "❌ BE 프로젝트 디렉토리를 찾을 수 없습니다."
  echo ""
  echo "현재 디렉토리 내용:"
  ls -la
  exit 1
fi

echo "BE 프로젝트 경로: $BE_PATH"

# 2. BE 프로젝트 내용 확인
echo ""
echo "📋 BE 프로젝트 내용 확인:"
echo "─────────────────────────────────────"
if [ -d "$BE_PATH" ]; then
  ls -la "$BE_PATH/"
  echo ""
  
  # Gradle 또는 Maven 확인
  if [ -f "$BE_PATH/build.gradle" ] || [ -f "$BE_PATH/build.gradle.kts" ]; then
    echo "✅ Gradle 프로젝트 발견"
    BUILD_TOOL="gradle"
    BUILD_FILE=$(find "$BE_PATH" -name "build.gradle*" | head -1)
    echo "빌드 파일: $BUILD_FILE"
  elif [ -f "$BE_PATH/pom.xml" ]; then
    echo "✅ Maven 프로젝트 발견"
    BUILD_TOOL="maven"
    BUILD_FILE="$BE_PATH/pom.xml"
    echo "빌드 파일: $BUILD_FILE"
  else
    echo "❌ 빌드 파일을 찾을 수 없습니다."
    echo "확인된 파일들:"
    find "$BE_PATH" -name "*.gradle" -o -name "pom.xml" -o -name "*.java" | head -10
    exit 1
  fi
  
  # Java 소스 확인
  if [ -d "$BE_PATH/src" ]; then
    echo "✅ src 디렉토리 발견"
    echo "Java 파일 수: $(find "$BE_PATH/src" -name "*.java" | wc -l)"
  fi
  
  # Application 클래스 찾기
  MAIN_CLASS=$(find "$BE_PATH/src" -name "*Application.java" 2>/dev/null | head -1)
  if [ -n "$MAIN_CLASS" ]; then
    echo "✅ Main Application 클래스: $MAIN_CLASS"
  fi
  
else
  echo "❌ BE 프로젝트 디렉토리 접근 불가"
  exit 1
fi

# 3. 현재 Dockerfile 내용 확인
echo ""
echo "📄 현재 Dockerfile 내용:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat Dockerfile
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 4. 문제점 분석
echo ""
echo "🔍 Dockerfile 문제점 분석:"

# COPY 명령어 확인
COPY_COMMANDS=$(grep -n "COPY\|ADD" Dockerfile)
echo "현재 COPY/ADD 명령어:"
echo "$COPY_COMMANDS"

echo ""
echo "예상 문제점:"
echo "1. COPY 명령어가 올바른 BE 프로젝트 경로를 참조하지 않음"
echo "2. WORKDIR이 잘못 설정되어 있을 수 있음"
echo "3. 빌드 명령어가 올바른 디렉토리에서 실행되지 않음"

# 5. 수정된 Dockerfile 생성
echo ""
echo "🔧 수정된 Dockerfile 생성 중..."

# 백업 생성
cp Dockerfile Dockerfile.backup
echo "✅ 기존 Dockerfile을 Dockerfile.backup으로 백업했습니다."

# 빌드 도구에 따른 Dockerfile 생성
if [ "$BUILD_TOOL" = "gradle" ]; then
  cat > Dockerfile.new << EOF
# Multi-stage build for Spring Boot with Gradle
FROM openjdk:17-jdk-slim as builder

# 작업 디렉토리 설정
WORKDIR /build

# Gradle 관련 파일들 먼저 복사 (캐시 최적화)
COPY $BE_PATH/gradle ./gradle
COPY $BE_PATH/gradlew ./
COPY $BE_PATH/build.gradle* ./
COPY $BE_PATH/settings.gradle* ./

# 의존성 다운로드 (캐시 레이어)
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY $BE_PATH/src ./src

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
RUN apt-get update && \\
    apt-get install -y curl && \\
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
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \\
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EOF

elif [ "$BUILD_TOOL" = "maven" ]; then
  cat > Dockerfile.new << EOF
# Multi-stage build for Spring Boot with Maven
FROM openjdk:17-jdk-slim as builder

# 작업 디렉토리 설정
WORKDIR /build

# Maven 관련 파일들 먼저 복사 (캐시 최적화)
COPY $BE_PATH/pom.xml ./
COPY $BE_PATH/.mvn ./.mvn
COPY $BE_PATH/mvnw ./

# 의존성 다운로드 (캐시 레이어)
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# 소스 코드 복사
COPY $BE_PATH/src ./src

# 애플리케이션 빌드
RUN ./mvnw clean package -DskipTests -B

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
RUN apt-get update && \\
    apt-get install -y curl && \\
    rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 파일 복사
COPY --from=builder /build/target/*.jar app.jar

# 소유권 변경
RUN chown -R appuser:appuser /app

# 애플리케이션 사용자로 전환
USER appuser

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --inter-val=30s --timeout=10s --start-period=40s --retries=3 \\
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EOF
fi

# 6. .dockerignore 파일 생성
echo ""
echo "📝 .dockerignore 파일 생성..."

cat > .dockerignore << 'EOF'
# Git
.git
.gitignore

# Build outputs (다른 경로의 빌드 파일들 제외)
**/build/
**/target/
!inter-v/BE/inter-v/build/
!inter-v/BE/inter-v/target/

# IDE files
.idea/
.vscode/
*.iml
*.ipr
*.iws

# OS files
.DS_Store
Thumbs.db

# Logs
*.log
logs/

# Temporary files
*.tmp
*.temp

# Node modules (프론트엔드가 있는 경우)
node_modules/

# 기존 Docker 관련 파일들
Dockerfile.backup
README.md
.env
.env.*

# AWS 관련 설정 파일들
.aws/
*.pem
EOF

# 7. 변경사항 비교
echo ""
echo "🔄 Dockerfile 변경사항 비교:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📄 새로운 Dockerfile 내용:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat Dockerfile.new
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 8. 사용자 확인 및 적용
echo ""
echo "🤔 새로운 Dockerfile을 적용하시겠습니까?"
echo "변경사항:"
echo "  - BE 프로젝트 경로: $BE_PATH"
echo "  - 빌드 도구: $BUILD_TOOL"
echo "  - Multi-stage 빌드 적용"
echo "  - 보안 개선 (non-root 사용자)"
echo "  - 헬스체크 추가"
echo "  - .dockerignore 최적화"
echo ""
echo "적용하시겠습니까? (y/n):"
read -r apply_changes

if [[ "$apply_changes" =~ ^[Yy]$ ]]; then
  mv Dockerfile.new Dockerfile
  echo "✅ 새로운 Dockerfile이 적용되었습니다!"
  echo "✅ .dockerignore 파일이 생성되었습니다!"
  echo ""
  echo "📋 적용된 개선사항:"
  echo "  - ✅ 올바른 BE 프로젝트 경로 ($BE_PATH)"
  echo "  - ✅ Multi-stage 빌드로 이미지 크기 최적화"
  echo "  - ✅ 캐시 최적화로 빌드 속도 향상"
  echo "  - ✅ 보안 강화 (non-root 실행)"
  echo "  - ✅ 헬스체크 기능 추가"
  echo "  - ✅ .dockerignore로 불필요한 파일 제외"
else
  rm Dockerfile.new
  echo "❌ 변경사항이 적용되지 않았습니다."
  echo "기존 Dockerfile을 그대로 사용합니다."
fi

# 9. 테스트 빌드 제안
echo ""
echo "🧪 Dockerfile 테스트 빌드를 실행하시겠습니까? (y/n):"
read -r test_build

if [[ "$test_build" =~ ^[Yy]$ ]]; then
  echo "🏗️ 테스트 빌드 시작..."
  docker build -t inter-v-test .
  
  if [ $? -eq 0 ]; then
    echo "✅ 테스트 빌드 성공!"
    echo "이미지 크기: $(docker images inter-v-test --format '{{.Size}}')"
    
    # 이미지 정리 옵션
    echo ""
    echo "테스트 이미지를 삭제하시겠습니까? (y/n):"
    read -r cleanup
    if [[ "$cleanup" =~ ^[Yy]$ ]]; then
      docker rmi inter-v-test
      echo "✅ 테스트 이미지 삭제 완료"
    fi
  else
    echo "❌ 테스트 빌드 실패"
    echo "Dockerfile을 다시 확인해주세요."
  fi
fi

echo ""
echo "🎉 Dockerfile 수정 작업 완료!"
echo "=========================="
echo ""
echo "📁 생성된 파일들:"
echo "  - Dockerfile (수정됨)"
echo "  - Dockerfile.backup (백업)"
echo "  - .dockerignore (새로 생성)"
echo ""
echo "🚀 다음 단계:"
echo "  이제 수정된 Dockerfile로 배포를 진행할 수 있습니다:"
echo "  ./complete_ecs_ec2_deployment.sh"