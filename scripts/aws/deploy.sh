#!/bin/bash

echo "🚀 ECS + EC2 프리티어 배포"
echo "=============================================="

AWS_REGION="ap-northeast-2"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
PROJECT_NAME="interv"
ECR_URI="$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$PROJECT_NAME"
CLUSTER_NAME="interv-cluster"

echo "📋 배포 정보:"
echo "  - Account ID: $ACCOUNT_ID"
echo "  - Region: $AWS_REGION"
echo "  - ECR URI: $ECR_URI"
echo "  - Cluster: $CLUSTER_NAME"

# 함수: 에러 체크
check_error() {
    if [ $1 -ne 0 ]; then
        echo "❌ $2"
        exit 1
    fi
}

# 함수: ECS 클러스터 상태 확인 및 생성
setup_ecs_cluster() {
    echo ""
    echo "🐳 ECS 클러스터 설정..."
    
    # 클러스터 존재 확인
    CLUSTER_STATUS=$(aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].status' --output text 2>/dev/null)
    
    if [ "$CLUSTER_STATUS" = "ACTIVE" ]; then
        echo "✅ ECS 클러스터 이미 존재하고 활성화됨"
    elif [ "$CLUSTER_STATUS" = "None" ] || [ -z "$CLUSTER_STATUS" ]; then
        echo "📝 ECS 클러스터 생성 중..."
        aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION
        check_error $? "ECS 클러스터 생성 실패"
        
        # 클러스터 생성 대기
        echo "⏳ 클러스터 생성 대기 중..."
        sleep 10
        
        # 생성 확인
        CLUSTER_STATUS=$(aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].status' --output text 2>/dev/null)
        if [ "$CLUSTER_STATUS" = "ACTIVE" ]; then
            echo "✅ ECS 클러스터 생성 완료"
        else
            echo "❌ ECS 클러스터 생성 실패: 상태 = $CLUSTER_STATUS"
            exit 1
        fi
    else
        echo "⚠️ ECS 클러스터 상태: $CLUSTER_STATUS"
        echo "클러스터를 다시 생성합니다..."
        aws ecs delete-cluster --cluster $CLUSTER_NAME --region $AWS_REGION 2>/dev/null
        sleep 5
        aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION
        check_error $? "ECS 클러스터 재생성 실패"
        echo "✅ ECS 클러스터 재생성 완료"
    fi
}

# 함수: Container Instance 확인
check_container_instances() {
    echo ""
    echo "🔍 Container Instance 확인..."
    
    CONTAINER_INSTANCES=$(aws ecs list-container-instances --cluster $CLUSTER_NAME --region $AWS_REGION --query 'containerInstanceArns' --output text 2>/dev/null)
    
    if [ -z "$CONTAINER_INSTANCES" ] || [ "$CONTAINER_INSTANCES" = "None" ]; then
        echo "⚠️ 클러스터에 등록된 Container Instance가 없습니다."
        echo ""
        echo "🔧 EC2 인스턴스가 ECS 클러스터에 조인하려면:"
        echo "1. EC2 인스턴스에 ECS Agent가 설치되어 있어야 합니다"
        echo "2. IAM Role에 ECS 권한이 있어야 합니다"
        echo "3. User Data에 ECS_CLUSTER=$CLUSTER_NAME 설정이 있어야 합니다"
        echo ""
        
        # Auto Scaling Group 확인
        echo "📊 Auto Scaling Group 인스턴스 확인..."
        ASG_INSTANCES=$(aws autoscaling describe-auto-scaling-groups --region $AWS_REGION --query 'AutoScalingGroups[*].Instances[*].[InstanceId,LifecycleState]' --output table)
        echo "$ASG_INSTANCES"
        
        return 1
    else
        echo "✅ Container Instance 발견:"
        aws ecs describe-container-instances --cluster $CLUSTER_NAME --container-instances $CONTAINER_INSTANCES --region $AWS_REGION --query 'containerInstances[*].[ec2InstanceId,status,runningTasksCount]' --output table
        return 0
    fi
}

# 1. AWS 권한 확인
echo ""
echo "🔑 AWS 권한 확인..."
aws sts get-caller-identity >/dev/null 2>&1
check_error $? "AWS 인증 실패. aws configure를 확인하세요."
echo "✅ AWS 인증 성공"

# 2. Parameter Store 설정 확인
echo ""
echo "🔍 Parameter Store 설정 확인..."

required_params=(
  "/interv/db/username"
  "/interv/db/password"
  "/interv/cognito/user-pool-id"
  "/interv/cognito/client-id"
  "/interv/cognito/client-secret"
)

all_params_exist=true
for param in "${required_params[@]}"; do
  if aws ssm get-parameter --name "$param" --region $AWS_REGION >/dev/null 2>&1; then
    echo "✅ $param"
  else
    echo "❌ $param (누락)"
    all_params_exist=false
  fi
done

if [ "$all_params_exist" = false ]; then
  echo "❌ Parameter Store 설정이 완료되지 않았습니다."
  echo "필요한 파라미터를 먼저 생성하세요."
  exit 1
fi

echo "✅ Parameter Store 설정 완료"

# 3. ECR 레포지토리 준비
echo ""
echo "🐳 ECR 레포지토리 준비..."

aws ecr describe-repositories --repository-names $PROJECT_NAME --region $AWS_REGION >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "📦 ECR 레포지토리 생성 중..."
  aws ecr create-repository --repository-name $PROJECT_NAME --region $AWS_REGION
  check_error $? "ECR 레포지토리 생성 실패"
  echo "✅ ECR 레포지토리 생성 완료"
else
  echo "✅ ECR 레포지토리 이미 존재"
fi

# 4. ECR 로그인
echo ""
echo "🔑 ECR 로그인..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_URI
check_error $? "ECR 로그인 실패"
echo "✅ ECR 로그인 성공"

# 5. Docker 이미지 빌드 및 푸시
echo ""
echo "🏗️ Docker 이미지 빌드..."

# Dockerfile 위치 확인
if [ ! -f "Dockerfile" ]; then
    echo "❌ Dockerfile을 찾을 수 없습니다. 현재 디렉토리: $(pwd)"
    echo "Dockerfile이 있는 디렉토리에서 스크립트를 실행하세요."
    exit 1
fi

docker build -t $PROJECT_NAME . --no-cache
check_error $? "Docker 빌드 실패"
echo "✅ Docker 빌드 완료"

echo ""
echo "🏷️ 이미지 태깅..."
docker tag $PROJECT_NAME:latest $ECR_URI:latest

echo ""
echo "📤 ECR에 이미지 푸시..."
docker push $ECR_URI:latest
check_error $? "ECR 푸시 실패"
echo "✅ Docker 이미지 푸시 완료"

# 6. CloudWatch 로그 그룹 생성
echo ""
echo "📊 CloudWatch 로그 그룹 생성..."
aws logs create-log-group --log-group-name "/ecs/interv" --region $AWS_REGION 2>/dev/null || echo "✅ 로그 그룹 이미 존재"

# 7. ECS 클러스터 설정
setup_ecs_cluster

# 8. Container Instance 확인
if ! check_container_instances; then
    echo ""
    echo "⚠️ Container Instance가 없어도 서비스는 생성하겠습니다."
    echo "나중에 EC2 인스턴스가 클러스터에 조인하면 태스크가 자동으로 시작됩니다."
    echo ""
    read -p "계속하시겠습니까? (y/N): " continue_deploy
    if [[ ! $continue_deploy =~ ^[Yy]$ ]]; then
        echo "배포를 중단합니다."
        exit 1
    fi
fi

# 9. Task Definition 생성
echo ""
echo "📋 Task Definition 생성..."

cat > task-definition-ec2.json << EOF
{
  "family": "interv-task-ec2",
  "networkMode": "bridge",
  "requiresCompatibilities": ["EC2"],
  "cpu": "512",
  "memory": "900",
  "executionRoleArn": "arn:aws:iam::$ACCOUNT_ID:role/ecsTaskExecutionRole-interv",
  "taskRoleArn": "arn:aws:iam::$ACCOUNT_ID:role/ecsTaskRole-interv",
  "containerDefinitions": [
    {
      "name": "interv-container",
      "image": "$ECR_URI:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 0,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "DB_HOST", "value": "interv-db.cjygi4mssp23.ap-northeast-2.rds.amazonaws.com"},
        {"name": "DB_PORT", "value": "3306"},
        {"name": "DB_NAME", "value": "interv"},
        {"name": "AWS_REGION", "value": "ap-northeast-2"},
        {"name": "S3_BUCKET_NAME", "value": "interv-storage-t8osm0s8"},
        {"name": "COGNITO_REDIRECT_URI", "value": "https://interv.swote.dev/login/oauth2/code/cognito"},
        {"name": "COGNITO_LOGOUT_REDIRECT_URI", "value": "https://interv.swote.dev/"},
        {"name": "SERVER_PORT", "value": "8080"},
        {"name": "JPA_DDL_AUTO", "value": "update"},
        {"name": "SHOW_SQL", "value": "false"},
        {"name": "FORMAT_SQL", "value": "false"}
      ],
      "secrets": [
        {"name": "DB_USERNAME", "valueFrom": "arn:aws:ssm:ap-northeast-2:$ACCOUNT_ID:parameter/interv/db/username"},
        {"name": "DB_PASSWORD", "valueFrom": "arn:aws:ssm:ap-northeast-2:$ACCOUNT_ID:parameter/interv/db/password"},
        {"name": "COGNITO_USER_POOL_ID", "valueFrom": "arn:aws:ssm:ap-northeast-2:$ACCOUNT_ID:parameter/interv/cognito/user-pool-id"},
        {"name": "COGNITO_CLIENT_ID", "valueFrom": "arn:aws:ssm:ap-northeast-2:$ACCOUNT_ID:parameter/interv/cognito/client-id"},
        {"name": "COGNITO_CLIENT_SECRET", "valueFrom": "arn:aws:ssm:ap-northeast-2:$ACCOUNT_ID:parameter/interv/cognito/client-secret"}
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
      "essential": true,
      "memoryReservation": 512
    }
  ]
}
EOF

aws ecs register-task-definition \
  --cli-input-json file://task-definition-ec2.json \
  --region $AWS_REGION

check_error $? "Task Definition 등록 실패"
echo "✅ Task Definition 등록 완료"

# 10. Target Group 확인
echo ""
echo "🎯 Target Group 확인..."

TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names interv-tg --region $AWS_REGION --query 'TargetGroups[0].TargetGroupArn' --output text 2>/dev/null)

if [ "$TARGET_GROUP_ARN" = "None" ] || [ -z "$TARGET_GROUP_ARN" ]; then
  echo "❌ Target Group 'interv-tg'를 찾을 수 없습니다."
  echo "ALB와 Target Group을 먼저 생성해야 합니다."
  echo ""
  echo "Target Group 없이 서비스를 생성하시겠습니까? (ALB 없이 테스트용)"
  read -p "(y/N): " create_without_alb
  if [[ $create_without_alb =~ ^[Yy]$ ]]; then
    TARGET_GROUP_ARN=""
  else
    exit 1
  fi
else
  echo "✅ Target Group: $TARGET_GROUP_ARN"
fi

# 11. ECS 서비스 생성/업데이트
echo ""
echo "🚀 ECS 서비스 생성/업데이트..."

# 기존 서비스 확인
EXISTING_SERVICE=$(aws ecs describe-services \
  --cluster $CLUSTER_NAME \
  --services interv-service \
  --region $AWS_REGION \
  --query 'services[0].serviceName' \
  --output text 2>/dev/null)

if [ "$EXISTING_SERVICE" = "interv-service" ]; then
  echo "🔄 기존 서비스 업데이트..."
  aws ecs update-service \
    --cluster $CLUSTER_NAME \
    --service interv-service \
    --task-definition interv-task-ec2 \
    --desired-count 1 \
    --region $AWS_REGION
  check_error $? "ECS 서비스 업데이트 실패"
else
  echo "🆕 새 ECS 서비스 생성..."
  
  if [ -n "$TARGET_GROUP_ARN" ]; then
    # ALB와 함께 서비스 생성
    aws ecs create-service \
      --cluster $CLUSTER_NAME \
      --service-name interv-service \
      --task-definition interv-task-ec2 \
      --desired-count 1 \
      --launch-type EC2 \
      --load-balancers "targetGroupArn=$TARGET_GROUP_ARN,containerName=interv-container,containerPort=8080" \
      --health-check-grace-period-seconds 300 \
      --region $AWS_REGION
  else
    # ALB 없이 서비스 생성
    aws ecs create-service \
      --cluster $CLUSTER_NAME \
      --service-name interv-service \
      --task-definition interv-task-ec2 \
      --desired-count 1 \
      --launch-type EC2 \
      --region $AWS_REGION
  fi
  
  check_error $? "ECS 서비스 생성 실패"
fi

echo "✅ ECS 서비스 설정 완료"

# 12. 최종 상태 확인
echo ""
echo "📊 최종 배포 상태 확인..."

echo "ECS 클러스터 상태:"
aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].[clusterName,status,registeredContainerInstancesCount,runningTasksCount,pendingTasksCount]' --output table

echo ""
echo "ECS 서비스 상태:"
aws ecs describe-services --cluster $CLUSTER_NAME --services interv-service --region $AWS_REGION --query 'services[0].[serviceName,status,runningCount,pendingCount,desiredCount]' --output table

echo ""
echo "실행 중인 태스크:"
TASK_ARNS=$(aws ecs list-tasks --cluster $CLUSTER_NAME --service-name interv-service --region $AWS_REGION --query 'taskArns[*]' --output text)
if [ -n "$TASK_ARNS" ] && [ "$TASK_ARNS" != "None" ]; then
  aws ecs describe-tasks --cluster $CLUSTER_NAME --tasks $TASK_ARNS --region $AWS_REGION --query 'tasks[*].[taskArn,lastStatus,healthStatus,cpu,memory]' --output table
else
  echo "실행 중인 태스크가 없습니다."
fi

if [ -n "$TARGET_GROUP_ARN" ]; then
  echo ""
  echo "Target Group 헬스체크:"
  aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN --region $AWS_REGION --query 'TargetHealthDescriptions[*].[Target.Id,TargetHealth.State,TargetHealth.Description]' --output table
fi

# 정리
rm task-definition-ec2.json 2>/dev/null

echo ""
echo "🎉 ECS + EC2 배포 완료!"
echo "========================"
echo ""
echo "📋 배포 결과:"
echo "  - ECS 클러스터: $CLUSTER_NAME"
echo "  - ECS 서비스: interv-service"
echo "  - Task Definition: interv-task-ec2"
if [ -n "$TARGET_GROUP_ARN" ]; then
  echo "  - Target Group: $TARGET_GROUP_ARN"
  echo "  - 도메인: https://interv.swote.dev"
fi
echo ""
echo "🔧 다음 단계:"
echo "1. EC2 인스턴스가 ECS 클러스터에 조인되었는지 확인"
echo "2. 태스크가 정상적으로 실행되는지 확인"
echo "3. ALB 헬스체크 통과 확인"
echo "4. 애플리케이션 접속 테스트"
echo ""
echo "📊 모니터링:"
echo "  - ECS 콘솔: https://console.aws.amazon.com/ecs/home?region=ap-northeast-2"
echo "  - CloudWatch 로그: https://console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2#logsV2:log-groups/log-group/%2Fecs%2Finterv"