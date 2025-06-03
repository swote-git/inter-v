#!/bin/bash
# 수동 Docker 배포 스크립트

echo "🚀 수동 Docker 배포 시작..."

# ECR 로그인
echo "1. ECR 로그인..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 034115074124.dkr.ecr.ap-northeast-2.amazonaws.com

# 기존 컨테이너 정리
echo "2. 기존 컨테이너 정리..."
docker stop interv-manual 2>/dev/null || true
docker rm interv-manual 2>/dev/null || true

# 최신 이미지 pull
echo "3. 최신 이미지 다운로드..."
docker pull 034115074124.dkr.ecr.ap-northeast-2.amazonaws.com/interv:latest

# 컨테이너 실행 (포트 8080)
echo "4. 컨테이너 실행..."
docker run -d \
  --name interv-manual \
  -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e AWS_REGION=ap-northeast-2 \
  -e DB_PORT=3306 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e COGNITO_LOGOUT_REDIRECT_URI="https://interv.swote.dev/" \
  -e JPA_DDL_AUTO=update \
  -e COGNITO_REDIRECT_URI="https://interv.swote.dev/login/oauth2/code/cognito" \
  -e DB_NAME=interv \
  -e SHOW_SQL=false \
  -e S3_BUCKET_NAME=interv-storage-t8osm0s8 \
  -e FORMAT_SQL=false \
  -e DB_HOST=interv-db.cjygi4mssp23.ap-northeast-2.rds.amazonaws.com \
  -e DB_USERNAME="admin" \
  -e DB_PASSWORD="interv2025!" \
  -e COGNITO_USER_POOL_ID="ap-northeast-2_N2siYmXOA" \
  -e COGNITO_CLIENT_ID="7cjnd36iuf0g8tl3ar1to5tpff" \
  -e COGNITO_CLIENT_SECRET="1sutf22r149noinrc9u931auqj5v1uiid5tp555iinv7dgimchlr" \
  034115074124.dkr.ecr.ap-northeast-2.amazonaws.com/interv:latest

if [ $? -eq 0 ]; then
  echo "✅ 컨테이너 시작 성공!"
  echo ""
  echo "🔍 컨테이너 상태 확인:"
  docker ps | grep interv-manual
  
  echo ""
  echo "⏳ 애플리케이션 시작 대기 중... (30초)"
  sleep 30
  
  echo ""
  echo "🏥 Health Check 테스트:"
  curl -f http://localhost:8080/actuator/health || echo "Health check 실패"
  
  echo ""
  echo "📋 컨테이너 로그 (최근 20줄):"
  docker logs --tail 20 interv-manual
  
  echo ""
  echo "🌐 접속 정보:"
  echo "  - 로컬: http://localhost:8080"
  echo "  - 외부: http://None:8080"
  echo "  - Health: http://None:8080/actuator/health"
  echo ""
  echo "🔍 실시간 로그 모니터링:"
  echo "docker logs -f interv-manual"
else
  echo "❌ 컨테이너 시작 실패"
  echo "Docker 로그:"
  docker logs interv-manual 2>/dev/null || echo "로그 없음"
fi
