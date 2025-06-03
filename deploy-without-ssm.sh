#!/bin/bash

echo "🚀 Parameter Store 없이 배포 시작"

# 환경 변수 확인
if [ -z "$DB_PASSWORD" ] || [ -z "$COGNITO_USER_POOL_ID" ] || [ -z "$COGNITO_CLIENT_ID" ] || [ -z "$COGNITO_CLIENT_SECRET" ]; then
  echo "❌ 필수 환경 변수를 설정해주세요:"
  echo "export DB_PASSWORD='your_password'"
  echo "export COGNITO_USER_POOL_ID='ap-northeast-2_xxxxxxxxx'"
  echo "export COGNITO_CLIENT_ID='your_client_id'"
  echo "export COGNITO_CLIENT_SECRET='your_client_secret'"
  exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION="ap-northeast-2"
ECR_URI="$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/interv"

# ECR 로그인
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_URI

# ECR 레포지토리 생성
aws ecr create-repository --repository-name interv --region $AWS_REGION 2>/dev/null || echo "ECR 레포지토리 이미 존재"

# Docker 빌드 및 푸시
docker build -t interv .
docker tag interv:latest $ECR_URI:latest
docker push $ECR_URI:latest

# Task Definition 생성 (환경 변수 직접 포함)
cat > task-definition-no-ssm.json << EOL
{
  "family": "interv-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::$ACCOUNT_ID:role/ecsTaskExecutionRole-interv",
  "taskRoleArn": "arn:aws:iam::$ACCOUNT_ID:role/ecsTaskRole-interv",
  "containerDefinitions": [
    {
      "name": "interv-container",
      "image": "$ECR_URI:latest",
      "portMappings": [{"containerPort": 8080, "protocol": "tcp"}],
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "DB_HOST", "value": "interv-db.cjygi4mssp23.ap-northeast-2.rds.amazonaws.com"},
        {"name": "DB_PORT", "value": "3306"},
        {"name": "DB_NAME", "value": "interv"},
        {"name": "DB_USERNAME", "value": "admin"},
        {"name": "DB_PASSWORD", "value": "$DB_PASSWORD"},
        {"name": "AWS_REGION", "value": "ap-northeast-2"},
        {"name": "S3_BUCKET_NAME", "value": "interv-storage-t8osm0s8"},
        {"name": "COGNITO_USER_POOL_ID", "value": "$COGNITO_USER_POOL_ID"},
        {"name": "COGNITO_CLIENT_ID", "value": "$COGNITO_CLIENT_ID"},
        {"name": "COGNITO_CLIENT_SECRET", "value": "$COGNITO_CLIENT_SECRET"},
        {"name": "COGNITO_REDIRECT_URI", "value": "https://interv.swote.dev/login/oauth2/code/cognito"},
        {"name": "COGNITO_LOGOUT_REDIRECT_URI", "value": "https://interv.swote.dev/"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/interv",
          "awslogs-region": "ap-northeast-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 120
      },
      "essential": true
    }
  ]
}
EOL

echo "✅ Task Definition 생성 완료"
echo "이제 ECS 클러스터와 서비스를 생성하고 배포하세요."
