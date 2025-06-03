#!/bin/bash

# 환경 변수 설정
export AWS_REGION=ap-northeast-2
export PROJECT_NAME=interv

# AWS 계정 정보 수집
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "✅ AWS Account ID: $ACCOUNT_ID"

# VPC 정보 수집 (ALB가 있는 VPC를 찾기)
export VPC_ID=$(aws elbv2 describe-load-balancers \
  --names interv-alb \
  --query 'LoadBalancers[0].VpcId' \
  --output text \
  --region $AWS_REGION 2>/dev/null)

if [ "$VPC_ID" = "None" ] || [ -z "$VPC_ID" ]; then
  echo "❌ ALB를 찾을 수 없습니다. 수동으로 VPC ID를 설정해주세요:"
  echo "export VPC_ID=vpc-xxxxxxxxx"
else
  echo "✅ VPC ID: $VPC_ID"
fi

# 서브넷 정보 수집 (private 서브넷 우선)
export SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'Subnets[?MapPublicIpOnLaunch==`false`].SubnetId | join(`,`, @)' \
  --output text \
  --region $AWS_REGION)

if [ -z "$SUBNET_IDS" ]; then
  # Private 서브넷이 없으면 Public 서브넷 사용
  export SUBNET_IDS=$(aws ec2 describe-subnets \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query 'Subnets[].SubnetId | join(`,`, @)' \
    --output text \
    --region $AWS_REGION)
fi

echo "✅ Subnet IDs: $SUBNET_IDS"

# 기존 Target Group 정보 수집
export TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
  --names interv-tg \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text \
  --region $AWS_REGION 2>/dev/null)

if [ "$TARGET_GROUP_ARN" = "None" ] || [ -z "$TARGET_GROUP_ARN" ]; then
  echo "⚠️  Target Group이 없습니다. 새로 생성하겠습니다."
else
  echo "✅ Target Group ARN: $TARGET_GROUP_ARN"
fi

# 환경 변수 저장
cat > .env << EOF
AWS_REGION=$AWS_REGION
ACCOUNT_ID=$ACCOUNT_ID
VPC_ID=$VPC_ID
SUBNET_IDS=$SUBNET_IDS
TARGET_GROUP_ARN=$TARGET_GROUP_ARN
PROJECT_NAME=$PROJECT_NAME
EOF

echo "✅ 환경 정보가 .env 파일에 저장되었습니다."
echo ""
echo "다음 정보들을 입력해주세요:"
echo "export DB_PASSWORD='YOUR_ACTUAL_DB_PASSWORD'"
echo "export COGNITO_USER_POOL_ID='YOUR_COGNITO_USER_POOL_ID'"
echo "export COGNITO_CLIENT_ID='YOUR_COGNITO_CLIENT_ID'"
echo "export COGNITO_CLIENT_SECRET='YOUR_COGNITO_CLIENT_SECRET'"
echo "export LLM_API_URL='YOUR_LLM_API_URL'"
echo "export LLM_API_KEY='YOUR_LLM_API_KEY'"