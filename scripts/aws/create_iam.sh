#!/bin/bash

echo "🔍 권한 문제 진단 중..."
echo "=================================="

# 1. 현재 인라인 정책 확인
echo "📋 현재 인라인 정책 확인:"
INLINE_POLICIES=$(aws iam list-user-policies --user-name interv-deploy-user --output text --query 'PolicyNames')

if [ -z "$INLINE_POLICIES" ]; then
  echo "❌ 인라인 정책이 없습니다."
  NEED_TO_ADD_POLICY=true
else
  echo "✅ 인라인 정책 목록: $INLINE_POLICIES"
  
  # 특정 정책이 있는지 확인
  if echo "$INLINE_POLICIES" | grep -q "InterV-Additional-Permissions"; then
    echo "✅ InterV-Additional-Permissions 정책 존재"
    NEED_TO_ADD_POLICY=false
  else
    echo "❌ InterV-Additional-Permissions 정책이 없습니다."
    NEED_TO_ADD_POLICY=true
  fi
fi

# 2. 인라인 정책 추가 (필요한 경우)
if [ "$NEED_TO_ADD_POLICY" = true ]; then
  echo ""
  echo "📝 인라인 정책 추가 중..."
  
  cat > interv-comprehensive-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SSMFullAccess",
      "Effect": "Allow",
      "Action": "ssm:*",
      "Resource": "*"
    },
    {
      "Sid": "ECSFullAccess",
      "Effect": "Allow", 
      "Action": "ecs:*",
      "Resource": "*"
    },
    {
      "Sid": "ECRFullAccess",
      "Effect": "Allow",
      "Action": "ecr:*", 
      "Resource": "*"
    },
    {
      "Sid": "CloudWatchLogsFullAccess",
      "Effect": "Allow",
      "Action": "logs:*",
      "Resource": "*"
    },
    {
      "Sid": "CognitoFullAccess",
      "Effect": "Allow",
      "Action": "cognito-idp:*",
      "Resource": "*"
    },
    {
      "Sid": "STSAccess",
      "Effect": "Allow",
      "Action": [
        "sts:GetCallerIdentity"
      ],
      "Resource": "*"
    }
  ]
}
EOF

  # 인라인 정책 추가
  aws iam put-user-policy \
    --user-name interv-deploy-user \
    --policy-name InterV-Comprehensive-Policy \
    --policy-document file://interv-comprehensive-policy.json

  if [ $? -eq 0 ]; then
    echo "✅ 인라인 정책 추가 성공!"
  else
    echo "❌ 인라인 정책 추가 실패"
    rm interv-comprehensive-policy.json
    exit 1
  fi
  
  rm interv-comprehensive-policy.json
  
  echo "⏳ 권한 적용 대기 중... (30초)"
  sleep 30
else
  echo "⏳ 기존 정책 확인 후 권한 적용 대기 중... (10초)"
  sleep 10
fi

echo ""
echo "🧪 권한 테스트 시작..."
echo "=================================="

# 3. 권한 테스트
echo "🔐 SSM 권한 테스트..."
aws ssm describe-parameters --region ap-northeast-2 --max-items 1 >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ SSM 권한 정상"
  SSM_OK=true
else
  echo "❌ SSM 권한 여전히 문제"
  SSM_OK=false
fi

echo "🐳 ECS 권한 테스트..."
aws ecs describe-clusters --region ap-northeast-2 >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ ECS 권한 정상"
  ECS_OK=true
else
  echo "❌ ECS 권한 문제"
  ECS_OK=false
fi

echo "📦 ECR 권한 테스트..."
aws ecr describe-repositories --region ap-northeast-2 >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ ECR 권한 정상"
  ECR_OK=true
else
  echo "❌ ECR 권한 문제"
  ECR_OK=false
fi

echo ""
echo "📊 권한 테스트 결과:"
echo "=================================="
echo "SSM: $([ "$SSM_OK" = true ] && echo "✅ 정상" || echo "❌ 문제")"
echo "ECS: $([ "$ECS_OK" = true ] && echo "✅ 정상" || echo "❌ 문제")"
echo "ECR: $([ "$ECR_OK" = true ] && echo "✅ 정상" || echo "❌ 문제")"

# 4. 문제가 계속되는 경우 대안 제시
if [ "$SSM_OK" = false ]; then
  echo ""
  echo "🚨 SSM 권한 문제가 계속됩니다."
  echo ""
  echo "🔧 대안 1: AWS 콘솔에서 수동 설정"
  echo "─────────────────────────────────────"
  echo "1. AWS Console → Systems Manager → Parameter Store"
  echo "2. 다음 파라미터들을 수동으로 생성:"
  echo "   - /interv/db/username (String): admin"
  echo "   - /interv/db/password (SecureString): YOUR_DB_PASSWORD"
  echo "   - /interv/cognito/user-pool-id (String): YOUR_COGNITO_USER_POOL_ID"
  echo "   - /interv/cognito/client-id (String): YOUR_COGNITO_CLIENT_ID"
  echo "   - /interv/cognito/client-secret (SecureString): YOUR_COGNITO_CLIENT_SECRET"
  echo ""
  echo "🔧 대안 2: Parameter Store 없이 배포"
  echo "─────────────────────────────────────"
  echo "환경 변수를 Task Definition에 직접 설정하여 배포"
  echo ""
  
  # Parameter Store 없이 배포하는 간단한 스크립트 생성
  cat > deploy-without-ssm.sh << 'EOF'
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
EOF

  chmod +x deploy-without-ssm.sh
  echo "✅ 대안 배포 스크립트 생성: ./deploy-without-ssm.sh"
  
else
  echo ""
  echo "🎉 모든 권한이 정상적으로 작동합니다!"
  echo "이제 Parameter Store 설정을 진행할 수 있습니다."
fi

echo ""
echo "📋 현재 상태 요약:"
echo "=================================="
echo "관리형 정책: 10개 (최대 한계)"
echo "인라인 정책: $(aws iam list-user-policies --user-name interv-deploy-user --query 'length(PolicyNames)' --output text)개"
echo ""
echo "다음 명령어로 인라인 정책 내용 확인:"
echo "aws iam get-user-policy --user-name interv-deploy-user --policy-name InterV-Comprehensive-Policy"