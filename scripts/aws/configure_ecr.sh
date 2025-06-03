#!/bin/bash

echo "🧪 새로 추가된 권한 테스트 중..."

# SSM 권한 테스트
echo "🔐 Parameter Store 권한 테스트..."
aws ssm describe-parameters --region ap-northeast-2 --max-items 1 >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ SSM 권한 정상"
else
  echo "❌ SSM 권한 문제"
  exit 1
fi

# ECS 권한 테스트  
echo "🐳 ECS 권한 테스트..."
aws ecs describe-clusters --region ap-northeast-2 >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ ECS 권한 정상"
else
  echo "❌ ECS 권한 문제"
  exit 1
fi

# ECR 권한 테스트
echo "📦 ECR 권한 테스트..."
aws ecr describe-repositories --region ap-northeast-2 >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ ECR 권한 정상"
else
  echo "❌ ECR 권한 문제"
  exit 1
fi

echo ""
echo "🎉 모든 권한이 정상적으로 작동합니다!"
echo ""

# 환경 변수 확인
echo "📋 필수 환경 변수 확인 중..."
required_vars=("DB_PASSWORD" "COGNITO_USER_POOL_ID" "COGNITO_CLIENT_ID" "COGNITO_CLIENT_SECRET")
missing_vars=()

for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    missing_vars+=("$var")
  else
    echo "✅ $var 설정됨"
  fi
done

if [ ${#missing_vars[@]} -gt 0 ]; then
  echo ""
  echo "❌ 다음 환경 변수들을 설정해주세요:"
  for var in "${missing_vars[@]}"; do
    echo "export $var='your_value'"
  done
  echo ""
  echo "설정 후 다시 실행하세요: ./test_and_setup_parameters.sh"
  exit 1
fi

echo ""
echo "🔐 Parameter Store 설정 시작..."

AWS_REGION=${AWS_REGION:-"ap-northeast-2"}
LLM_API_URL=${LLM_API_URL:-"http://localhost:8000"}
LLM_API_KEY=${LLM_API_KEY:-"dummy-api-key"}

# Parameter Store에 값들 저장
echo "📝 Parameter 생성 중..."

# 데이터베이스 설정
aws ssm put-parameter \
  --name "/interv/db/username" \
  --value "admin" \
  --type "String" \
  --overwrite \
  --region $AWS_REGION
echo "✅ DB Username 설정 완료"

aws ssm put-parameter \
  --name "/interv/db/password" \
  --value "$DB_PASSWORD" \
  --type "SecureString" \
  --overwrite \
  --region $AWS_REGION
echo "✅ DB Password 설정 완료"

# Cognito 설정
aws ssm put-parameter \
  --name "/interv/cognito/user-pool-id" \
  --value "$COGNITO_USER_POOL_ID" \
  --type "String" \
  --overwrite \
  --region $AWS_REGION
echo "✅ Cognito User Pool ID 설정 완료"

aws ssm put-parameter \
  --name "/interv/cognito/client-id" \
  --value "$COGNITO_CLIENT_ID" \
  --type "String" \
  --overwrite \
  --region $AWS_REGION
echo "✅ Cognito Client ID 설정 완료"

aws ssm put-parameter \
  --name "/interv/cognito/client-secret" \
  --value "$COGNITO_CLIENT_SECRET" \
  --type "SecureString" \
  --overwrite \
  --region $AWS_REGION
echo "✅ Cognito Client Secret 설정 완료"

# LLM API 설정 (선택사항)
if [ "$LLM_API_URL" != "http://localhost:8000" ]; then
  aws ssm put-parameter \
    --name "/interv/llm/api-url" \
    --value "$LLM_API_URL" \
    --type "String" \
    --overwrite \
    --region $AWS_REGION
  echo "✅ LLM API URL 설정 완료"
fi

if [ "$LLM_API_KEY" != "dummy-api-key" ]; then
  aws ssm put-parameter \
    --name "/interv/llm/api-key" \
    --value "$LLM_API_KEY" \
    --type "SecureString" \
    --overwrite \
    --region $AWS_REGION
  echo "✅ LLM API Key 설정 완료"
fi

echo ""
echo "🔍 Parameter Store 설정 확인..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 설정된 파라미터들 확인
parameters=(
  "/interv/db/username"
  "/interv/db/password" 
  "/interv/cognito/user-pool-id"
  "/interv/cognito/client-id"
  "/interv/cognito/client-secret"
)

for param in "${parameters[@]}"; do
  result=$(aws ssm get-parameter --name "$param" --region $AWS_REGION --query 'Parameter.Name' --output text 2>/dev/null)
  if [ "$result" = "$param" ]; then
    echo "✅ $param"
  else
    echo "❌ $param (설정 실패)"
  fi
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# ECR 레포지토리 준비
echo "🐳 ECR 레포지토리 준비 중..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
PROJECT_NAME="interv"

aws ecr describe-repositories --repository-names $PROJECT_NAME --region $AWS_REGION >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "📦 ECR 레포지토리 생성 중..."
  aws ecr create-repository --repository-name $PROJECT_NAME --region $AWS_REGION
  echo "✅ ECR 레포지토리 생성 완료"
else
  echo "✅ ECR 레포지토리 이미 존재"
fi

# CloudWatch 로그 그룹 생성
echo "📊 CloudWatch 로그 그룹 생성 중..."
aws logs create-log-group --log-group-name "/ecs/interv" --region $AWS_REGION 2>/dev/null || echo "✅ 로그 그룹 이미 존재"

echo ""
echo "🎉 모든 설정이 완료되었습니다!"
echo ""
echo "🚀 이제 배포를 시작할 수 있습니다:"
echo "1. ECR 로그인: aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
echo "2. Docker 빌드: docker build -t $PROJECT_NAME ."
echo "3. 이미지 태그: docker tag $PROJECT_NAME:latest $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$PROJECT_NAME:latest"
echo "4. 이미지 푸시: docker push $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$PROJECT_NAME:latest"
echo ""
echo "또는 통합 배포 스크립트 실행: ./deploy_to_ecs.sh"