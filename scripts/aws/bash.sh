#!/bin/bash

echo "🚀 ECS + EC2 프리티어 완전 배포"
echo "================================"

AWS_REGION="ap-northeast-2"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
PROJECT_NAME="interv"
ECR_URI="$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$PROJECT_NAME"

echo "📋 배포 정보:"
echo "  - Account ID: $ACCOUNT_ID"
echo "  - Region: $AWS_REGION"
echo "  - ECR URI: $ECR_URI"

# 1. 현재 Auto Scaling Group 상태 확인
echo ""
echo "📊 Auto Scaling Group 상태 확인..."

ASG_LIST=$(aws autoscaling describe-auto-scaling-groups --region $AWS_REGION --query 'AutoScalingGroups[*].[AutoScalingGroupName,MinSize,MaxSize,DesiredCapacity,Instances[0].InstanceType]' --output table)
echo "$ASG_LIST"

# 프리티어 ASG 찾기
ASG_NAME=$(aws autoscaling describe-auto-scaling-groups --region $AWS_REGION --query 'AutoScalingGroups[?contains(Instances[0].InstanceType, `t2.micro`) || contains(Instances[0].InstanceType, `t3.micro`)].AutoScalingGroupName' --output text | head -1)

if [ -z "$ASG_NAME" ]; then
  echo "❌ 프리티어 Auto Scaling Group을 찾을 수 없습니다."
  echo "기존 ASG 이름을 입력하세요:"
  read -r ASG_NAME
fi

echo "✅ 사용할 ASG: $ASG_NAME"

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
  echo "✅ ECR 레포지토리 생성 완료"
else
  echo "✅ ECR 레포지토리 이미 존재"
fi

# 4. WSL 환경 및 프로젝트 디렉토리 확인
echo ""
echo "🖥️ WSL 환경 확인..."

if grep -q Microsoft /proc/version; then
  echo "✅ WSL 환경에서 실행 중"
  WSL_ENV=true
else
  echo "⚠️ WSL 환경이 아닌 것 같습니다."
  WSL_ENV=false
fi

echo ""
echo "📂 프로젝트 디렉토리 확인..."

# WSL에서 파일 시스템 위치 확인
PWD_PATH=$(pwd)
if [[ "$PWD_PATH" =~ ^/mnt/[a-z]/ ]]; then
  echo "⚠️ Windows 파일 시스템에서 실행 중: $PWD_PATH"
  echo "💡 Docker 빌드 성능을 위해 WSL 파일 시스템 사용을 권장합니다."
else
  echo "✅ WSL 파일 시스템에서 실행 중: $PWD_PATH"
fi

# 다양한 프로젝트 구조 지원
POSSIBLE_DIRS=(
  "BE/interv"
  "interv"
  "backend/interv"
  "server/interv"
  "."
)

BUILD_DIR=""
for dir in "${POSSIBLE_DIRS[@]}"; do
  if [ -f "$dir/Dockerfile" ]; then
    BUILD_DIR="$dir"
    echo "✅ Dockerfile 발견: $BUILD_DIR/Dockerfile"
    break
  fi
done

if [ -z "$BUILD_DIR" ]; then
  echo "❌ Dockerfile을 찾을 수 없습니다."
  echo ""
  echo "현재 디렉토리 구조:"
  find . -maxdepth 3 -name "Dockerfile" -type f 2>/dev/null | head -5
  echo ""
  echo "💡 Dockerfile이 다른 위치에 있다면 해당 디렉토리에서 스크립트를 실행하세요."
  exit 1
fi

echo "✅ 빌드 디렉토리: $BUILD_DIR"

# 5. ECR 로그인 (향상된 오류 처리)
echo ""
echo "🔑 ECR 로그인..."

# ECR 권한 테스트
echo "🧪 ECR 권한 테스트..."
aws ecr describe-repositories --region $AWS_REGION --max-items 1 >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "❌ ECR 권한이 없습니다. IAM 권한을 확인하세요."
  echo ""
  echo "필요한 권한:"
  echo "- ecr:GetAuthorizationToken"
  echo "- ecr:BatchCheckLayerAvailability"
  echo "- ecr:GetDownloadUrlForLayer"
  echo "- ecr:BatchGetImage"
  echo "- ecr:DescribeRepositories"
  echo "- ecr:CreateRepository"
  echo "- ecr:InitiateLayerUpload"
  echo "- ecr:UploadLayerPart"
  echo "- ecr:CompleteLayerUpload"
  echo "- ecr:PutImage"
  exit 1
fi

# ECR 로그인 시도
echo "🔐 ECR 인증 토큰 획득 중..."
ECR_TOKEN=$(aws ecr get-login-password --region $AWS_REGION)
if [ $? -ne 0 ] || [ -z "$ECR_TOKEN" ]; then
  echo "❌ ECR 인증 토큰 획득 실패"
  exit 1
fi

echo "🔑 Docker ECR 로그인 중..."
echo "$ECR_TOKEN" | docker login --username AWS --password-stdin $ECR_URI
if [ $? -ne 0 ]; then
  echo "❌ Docker ECR 로그인 실패"
  echo ""
  echo "🔧 문제 해결 방법:"
  echo "1. Docker가 실행 중인지 확인: docker ps"
  echo "2. AWS 권한 확인: aws sts get-caller-identity"
  echo "3. ECR 레포지토리 존재 확인: aws ecr describe-repositories --region $AWS_REGION"
  exit 1
fi

echo "✅ ECR 로그인 성공"

# 6. WSL 환경에서 Docker 이미지 빌드 및 푸시
echo ""
echo "🏗️ WSL 환경에서 Docker 이미지 빌드..."

# 현재 작업 디렉토리 저장
ORIGINAL_DIR=$(pwd)
cd "$BUILD_DIR"

echo "현재 작업 디렉토리: $(pwd)"
echo ""
echo "📄 Dockerfile 미리보기:"
echo "─────────────────────────────────────"
head -10 Dockerfile
echo "─────────────────────────────────────"

# WSL에서 Docker 빌드 시 추가 옵션
DOCKER_BUILDKIT=${DOCKER_BUILDKIT:-1}
export DOCKER_BUILDKIT

echo ""
echo "🔨 Docker 빌드 시작... (WSL 환경에서는 시간이 조금 더 걸릴 수 있습니다)"

# Docker 빌드 실행 (WSL 최적화)
docker build -t $PROJECT_NAME . --progress=plain
BUILD_RESULT=$?

if [ $BUILD_RESULT -ne 0 ]; then
  echo "❌ Docker 빌드 실패"
  echo ""
  echo "🔧 WSL Docker 빌드 문제 해결:"
  echo "1. 메모리 부족: Docker Desktop → Settings → Resources에서 메모리 증가"
  echo "2. 파일 시스템 권한: 프로젝트를 WSL 파일 시스템으로 이동"
  echo "3. BuildKit 비활성화: DOCKER_BUILDKIT=0 docker build -t $PROJECT_NAME ."
  echo "4. Docker Desktop 재시작"
  cd "$ORIGINAL_DIR"
  exit 1
fi

echo "✅ Docker 빌드 완료"

echo ""
echo "🏷️ 이미지 태깅..."
docker tag $PROJECT_NAME:latest $ECR_URI:latest

echo ""
echo "📤 ECR에 이미지 푸시... (첫 번째 푸시는 시간이 오래 걸릴 수 있습니다)"
docker push $ECR_URI:latest
PUSH_RESULT=$?

if [ $PUSH_RESULT -ne 0 ]; then
  echo "❌ ECR 푸시 실패"
  echo ""
  echo "🔧 ECR 푸시 문제 해결:"
  echo "1. ECR 로그인 재시도: aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_URI"
  echo "2. 네트워크 연결 확인"
  echo "3. 이미지 크기 확인: docker images $PROJECT_NAME"
  cd "$ORIGINAL_DIR"
  exit 1
fi

echo "✅ Docker 이미지 푸시 완료"

# 생성된 이미지 정보 출력
echo ""
echo "📊 생성된 Docker 이미지 정보:"
docker images $PROJECT_NAME --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"

cd "$ORIGINAL_DIR"

# 5. CloudWatch 로그 그룹 생성
echo ""
echo "📊 CloudWatch 로그 그룹 생성..."
aws logs create-log-group --log-group-name "/ecs/interv" --region $AWS_REGION 2>/dev/null || echo "✅ 로그 그룹 이미 존재"

# 6. ECS 클러스터 확인/생성
echo ""
echo "🐳 ECS 클러스터 설정..."

aws ecs describe-clusters --clusters interv-cluster --region $AWS_REGION >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "📝 ECS 클러스터 생성 중..."
  aws ecs create-cluster --cluster-name interv-cluster --region $AWS_REGION
  echo "✅ ECS 클러스터 생성 완료"
else
  echo "✅ ECS 클러스터 이미 존재"
fi

# 7. ECS 인스턴스가 클러스터에 등록되었는지 확인
echo ""
echo "🔍 ECS 인스턴스 등록 상태 확인..."

# ASG의 인스턴스들이 ECS 클러스터에 제대로 등록되었는지 확인
CONTAINER_INSTANCES=$(aws ecs list-container-instances --cluster interv-cluster --region $AWS_REGION --query 'containerInstanceArns' --output text)

if [ -z "$CONTAINER_INSTANCES" ] || [ "$CONTAINER_INSTANCES" = "None" ]; then
  echo "⚠️ ECS 클러스터에 등록된 인스턴스가 없습니다."
  echo "Auto Scaling Group의 인스턴스들이 ECS 클러스터에 조인하도록 설정 중..."
  
  # ASG 인스턴스들을 새로 시작하여 ECS 클러스터에 조인하도록 함
  echo "🔄 Auto Scaling Group 인스턴스 새로고침..."
  
  # Launch Template에 ECS 클러스터 설정이 있는지 확인하고 없으면 추가
  LAUNCH_TEMPLATE=$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names $ASG_NAME \
    --region $AWS_REGION \
    --query 'AutoScalingGroups[0].LaunchTemplate.LaunchTemplateName' \
    --output text)
  
  if [ "$LAUNCH_TEMPLATE" != "None" ] && [ -n "$LAUNCH_TEMPLATE" ]; then
    echo "📝 Launch Template 업데이트 중..."
    
    # User Data에 ECS 클러스터 설정 추가
    cat > user-data-ecs.sh << 'EOF'
#!/bin/bash
echo ECS_CLUSTER=interv-cluster >> /etc/ecs/ecs.config
echo ECS_ENABLE_CONTAINER_METADATA=true >> /etc/ecs/ecs.config
echo ECS_ENABLE_TASK_IAM_ROLE=true >> /etc/ecs/ecs.config
systemctl restart ecs
EOF

    USER_DATA_B64=$(base64 -w 0 user-data-ecs.sh)
    
    # 기존 Launch Template 데이터 가져오기
    TEMPLATE_DATA=$(aws ec2 describe-launch-template-versions \
      --launch-template-name $LAUNCH_TEMPLATE \
      --region $AWS_REGION \
      --query 'LaunchTemplateVersions[0].LaunchTemplateData')
    
    # UserData 업데이트
    echo "$TEMPLATE_DATA" | jq --arg userdata "$USER_DATA_B64" '.UserData = $userdata' > updated_template.json
    
    # 새 버전 생성
    NEW_VERSION=$(aws ec2 create-launch-template-version \
      --launch-template-name $LAUNCH_TEMPLATE \
      --launch-template-data file://updated_template.json \
      --region $AWS_REGION \
      --query 'LaunchTemplateVersion.VersionNumber' \
      --output text)
    
    if [ $? -eq 0 ]; then
      echo "✅ Launch Template 업데이트 완료: v$NEW_VERSION"
      
      # ASG 업데이트
      aws autoscaling update-auto-scaling-group \
        --auto-scaling-group-name $ASG_NAME \
        --launch-template "LaunchTemplateName=$LAUNCH_TEMPLATE,Version=$NEW_VERSION" \
        --region $AWS_REGION
      
      # Instance Refresh 시도 (AWS CLI v2 필요)
      echo "🔄 인스턴스 새로고침 시도 중..."
      
      # AWS CLI 버전 확인
      aws autoscaling help 2>/dev/null | grep -q "start-instance-refresh"
      if [ $? -eq 0 ]; then
        echo "✅ start-instance-refresh 명령어 지원됨"
        
        # JSON 형식으로 preferences 전달
        REFRESH_ID=$(aws autoscaling start-instance-refresh \
          --auto-scaling-group-name "$ASG_NAME" \
          --preferences '{
            "InstanceWarmup": 300,
            "MinHealthyPercentage": 50,
            "SkipMatching": false
          }' \
          --region $AWS_REGION \
          --query 'InstanceRefreshId' \
          --output text 2>/dev/null)
        
        if [ $? -eq 0 ] && [ "$REFRESH_ID" != "None" ] && [ -n "$REFRESH_ID" ]; then
          echo "✅ Instance Refresh 시작됨: $REFRESH_ID"
          echo "📊 진행 상황: aws autoscaling describe-instance-refreshes --auto-scaling-group-name $ASG_NAME --region $AWS_REGION"
          
          # 간단한 상태 확인 (타임아웃 설정)
          echo "⏳ Instance Refresh 진행 상황 확인 중..."
          for i in {1..10}; do
            sleep 30
            REFRESH_STATUS=$(aws autoscaling describe-instance-refreshes \
              --auto-scaling-group-name "$ASG_NAME" \
              --region $AWS_REGION \
              --query 'InstanceRefreshes[0].Status' \
              --output text 2>/dev/null)
            
            echo "상태: $REFRESH_STATUS"
            
            if [ "$REFRESH_STATUS" = "Successful" ]; then
              echo "✅ Instance Refresh 완료!"
              break
            elif [ "$REFRESH_STATUS" = "Failed" ] || [ "$REFRESH_STATUS" = "Cancelled" ]; then
              echo "❌ Instance Refresh 실패: $REFRESH_STATUS"
              echo "💡 수동으로 인스턴스를 교체해주세요."
              break
            fi
          done
          
          if [ $i -eq 10 ]; then
            echo "⏳ Instance Refresh가 진행 중입니다. 백그라운드에서 계속 실행됩니다."
          fi
        else
          echo "❌ Instance Refresh 시작 실패. 수동 교체를 권장합니다."
        fi
      else
        echo "❌ start-instance-refresh 명령어를 지원하지 않습니다 (AWS CLI v1 또는 권한 부족)"
        echo ""
        echo "🔧 수동 인스턴스 교체 방법:"
        echo "1. AWS Console → EC2 → Auto Scaling Groups → $ASG_NAME"
        echo "2. Instance management 탭에서 기존 인스턴스 'Terminate'"
        echo "3. Auto Scaling이 자동으로 새 인스턴스 시작"
        echo ""
        echo "또는 별도 스크립트 실행: ./fix_instance_refresh_issue.sh"
      fi
      
    else
      echo "❌ Launch Template 업데이트 실패"
    fi
    
    rm user-data-ecs.sh updated_template.json 2>/dev/null
  fi
  
  # ECS 인스턴스 등록 재확인
  echo "🔍 ECS 인스턴스 등록 재확인..."
  sleep 30
  CONTAINER_INSTANCES=$(aws ecs list-container-instances --cluster interv-cluster --region $AWS_REGION --query 'containerInstanceArns' --output text)
fi

if [ -n "$CONTAINER_INSTANCES" ] && [ "$CONTAINER_INSTANCES" != "None" ]; then
  echo "✅ ECS 클러스터에 인스턴스 등록 완료"
  aws ecs describe-container-instances --cluster interv-cluster --container-instances $CONTAINER_INSTANCES --region $AWS_REGION --query 'containerInstances[*].[ec2InstanceId,runningTasksCount,pendingTasksCount]' --output table
else
  echo "❌ ECS 클러스터에 인스턴스가 등록되지 않았습니다."
  echo "수동으로 인스턴스에 접속하여 ECS 에이전트 상태를 확인해주세요."
fi

# 8. Task Definition 생성 (EC2 호환)
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

if [ $? -eq 0 ]; then
  echo "✅ Task Definition 등록 완료"
else
  echo "❌ Task Definition 등록 실패"
  exit 1
fi

# 9. Target Group 확인
echo ""
echo "🎯 Target Group 확인..."

TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names interv-tg --region $AWS_REGION --query 'TargetGroups[0].TargetGroupArn' --output text 2>/dev/null)

if [ "$TARGET_GROUP_ARN" = "None" ] || [ -z "$TARGET_GROUP_ARN" ]; then
  echo "❌ Target Group 'interv-tg'를 찾을 수 없습니다."
  echo "먼저 network_setup.sh를 실행하여 ALB와 Target Group을 생성하세요."
  exit 1
fi

echo "✅ Target Group: $TARGET_GROUP_ARN"

# 10. ECS 서비스 생성
echo ""
echo "🚀 ECS 서비스 생성..."

# 기존 서비스 확인
EXISTING_SERVICE=$(aws ecs describe-services \
  --cluster interv-cluster \
  --services interv-service \
  --region $AWS_REGION \
  --query 'services[0].serviceName' \
  --output text 2>/dev/null)

if [ "$EXISTING_SERVICE" = "interv-service" ]; then
  echo "🔄 기존 서비스 업데이트..."
  aws ecs update-service \
    --cluster interv-cluster \
    --service interv-service \
    --task-definition interv-task-ec2 \
    --desired-count 1 \
    --region $AWS_REGION
else
  echo "🆕 새 ECS 서비스 생성..."
  aws ecs create-service \
    --cluster interv-cluster \
    --service-name interv-service \
    --task-definition interv-task-ec2 \
    --desired-count 1 \
    --launch-type EC2 \
    --load-balancers "targetGroupArn=$TARGET_GROUP_ARN,containerName=interv-container,containerPort=8080" \
    --health-check-grace-period-seconds 300 \
    --region $AWS_REGION
fi

if [ $? -eq 0 ]; then
  echo "✅ ECS 서비스 설정 완료"
else
  echo "❌ ECS 서비스 설정 실패"
  exit 1
fi

# 11. 배포 상태 확인
echo ""
echo "⏳ 서비스 안정화 대기 중... (최대 10분)"
echo "진행 상황을 확인하려면 다른 터미널에서 다음 명령어를 실행하세요:"
echo "aws ecs describe-services --cluster interv-cluster --services interv-service --region $AWS_REGION"

# 서비스 안정화 대기 (타임아웃 설정)
timeout 600 aws ecs wait services-stable \
  --cluster interv-cluster \
  --services interv-service \
  --region $AWS_REGION

if [ $? -eq 0 ]; then
  echo "✅ 서비스 안정화 완료!"
else
  echo "⚠️ 서비스 안정화 대기 시간 초과 (10분)"
  echo "수동으로 상태를 확인해주세요."
fi

# 12. 최종 상태 확인
echo ""
echo "📊 최종 배포 상태 확인..."

echo "ECS 서비스 상태:"
aws ecs describe-services --cluster interv-cluster --services interv-service --region $AWS_REGION --query 'services[0].[serviceName,status,runningCount,pendingCount,desiredCount]' --output table

echo ""
echo "실행 중인 태스크:"
aws ecs list-tasks --cluster interv-cluster --service-name interv-service --region $AWS_REGION --query 'taskArns[*]' --output table

echo ""
echo "Target Group 헬스체크:"
aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN --region $AWS_REGION --query 'TargetHealthDescriptions[*].[Target.Id,TargetHealth.State,TargetHealth.Description]' --output table

# 정리
rm task-definition-ec2.json

echo ""
echo "🎉 WSL 환경에서 ECS + EC2 프리티어 배포 완료!"
echo "=============================================="
echo ""
echo "📋 배포 결과:"
echo "  - ECS 클러스터: interv-cluster"
echo "  - ECS 서비스: interv-service"
echo "  - Task Definition: interv-task-ec2"
echo "  - Target Group: $TARGET_GROUP_ARN"
echo "  - 빌드 환경: WSL ($(cat /proc/version | grep -o 'Microsoft\|WSL' | head -1))"
echo ""
echo "🌐 접속 정보:"
echo "  - 애플리케이션: https://interv.swote.dev"
echo "  - 헬스체크: https://interv.swote.dev/actuator/health"
echo ""
echo "📊 모니터링 (브라우저에서 접속):"
echo "  - ECS 콘솔: https://console.aws.amazon.com/ecs/home?region=ap-northeast-2"
echo "  - CloudWatch 로그: https://console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2#logsV2:log-groups/log-group/%2Fecs%2Finterv"
echo "  - EC2 인스턴스: https://console.aws.amazon.com/ec2/v2/home?region=ap-northeast-2#Instances:"
echo ""
echo "💰 예상 월 비용 (프리티어):"
echo "  - EC2 인스턴스: $0 (프리티어)"
echo "  - ALB: ~$18"
echo "  - 기타 AWS 서비스: ~$5-10"
echo "  - 총합: ~$23-28/월"
echo ""
echo "🔧 WSL 환경 관련 팁:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 성공적인 배포를 위한 WSL 모범 사례:"
echo "  1. 프로젝트를 WSL 파일 시스템에 보관 (예: ~/projects/)"
echo "  2. Docker Desktop의 WSL 2 백엔드 사용"
echo "  3. 정기적인 WSL 재시작으로 성능 유지"
echo ""
echo "🚨 문제 발생 시 해결 방법:"
echo "  1. Docker 문제: Docker Desktop 재시작 (Windows)"
echo "  2. 성능 저하: PowerShell에서 'wsl --shutdown' 후 WSL 재시작"
echo "  3. 권한 문제: sudo usermod -aG docker \$USER && newgrp docker"
echo "  4. 빌드 실패: 메모리 부족 시 Docker Desktop 리소스 증가"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"