#!/bin/bash

echo "🔍 502 Bad Gateway 문제 진단 및 수동 배포"
echo "========================================"

AWS_REGION="ap-northeast-2"
ECR_URI="034115074124.dkr.ecr.ap-northeast-2.amazonaws.com"
IMAGE_NAME="interv:latest"
CLUSTER_NAME="interv-cluster"

# 1. 현재 ECS 상태 진단
echo "📊 현재 ECS 배포 상태 확인..."

echo "1. ECS 태스크 상태:"
TASKS=$(aws ecs list-tasks --cluster $CLUSTER_NAME --region $AWS_REGION --query 'taskArns[*]' --output text 2>/dev/null)

if [ -n "$TASKS" ]; then
  for task in $TASKS; do
    echo "Task: $task"
    aws ecs describe-tasks --cluster $CLUSTER_NAME --tasks $task --region $AWS_REGION --query 'tasks[0].[lastStatus,healthStatus,desiredStatus]' --output text
  done
else
  echo "❌ 실행 중인 태스크가 없습니다."
fi

echo ""
echo "2. ECS 서비스 이벤트 (최근 오류):"
aws ecs describe-services --cluster $CLUSTER_NAME --services interv-service --region $AWS_REGION --query 'services[0].events[:5].[createdAt,message]' --output table 2>/dev/null

# 2. Target Group 상태 확인
echo ""
echo "🎯 Load Balancer Target Group 상태 확인..."

TARGET_GROUPS=$(aws elbv2 describe-target-groups --region $AWS_REGION --query 'TargetGroups[?contains(TargetGroupName, `interv`)][TargetGroupArn,TargetGroupName]' --output text)

if [ -n "$TARGET_GROUPS" ]; then
  echo "Target Groups:"
  echo "$TARGET_GROUPS"
  
  TARGET_GROUP_ARN=$(echo "$TARGET_GROUPS" | awk '{print $1}')
  echo ""
  echo "Target Health 상태:"
  aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN --region $AWS_REGION --query 'TargetHealthDescriptions[*].[Target.Id,Target.Port,TargetHealth.State,TargetHealth.Reason]' --output table
else
  echo "❌ Target Group을 찾을 수 없습니다."
fi

# 3. 컨테이너 로그 확인
echo ""
echo "📋 컨테이너 로그 확인..."

if [ -n "$TASKS" ]; then
  echo "최근 CloudWatch 로그 (최근 20줄):"
  aws logs tail /ecs/interv --since 30m --region $AWS_REGION | head -20
else
  echo "실행 중인 태스크가 없어 로그를 확인할 수 없습니다."
fi

# 4. 수동 배포를 위한 인스턴스 정보 수집
echo ""
echo "🖥️ 수동 배포를 위한 EC2 인스턴스 정보..."

INSTANCES=$(aws autoscaling describe-auto-scaling-groups --region $AWS_REGION --query 'AutoScalingGroups[*].Instances[*].[InstanceId,LifecycleState]' --output text | grep InService | awk '{print $1}')

if [ -n "$INSTANCES" ]; then
  echo "사용 가능한 인스턴스들:"
  for instance_id in $INSTANCES; do
    PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $instance_id --region $AWS_REGION --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
    PRIVATE_IP=$(aws ec2 describe-instances --instance-ids $instance_id --region $AWS_REGION --query 'Reservations[0].Instances[0].PrivateIpAddress' --output text)
    
    echo "  Instance ID: $instance_id"
    echo "  Public IP: $PUBLIC_IP"
    echo "  Private IP: $PRIVATE_IP"
    echo ""
  done
  
  # 첫 번째 인스턴스 선택
  SELECTED_INSTANCE=$(echo $INSTANCES | awk '{print $1}')
  INSTANCE_IP=$(aws ec2 describe-instances --instance-ids $SELECTED_INSTANCE --region $AWS_REGION --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
  
  echo "🚀 수동 배포용 인스턴스: $SELECTED_INSTANCE ($INSTANCE_IP)"
else
  echo "❌ 사용 가능한 인스턴스를 찾을 수 없습니다."
  exit 1
fi

# 5. 환경 변수 준비
echo ""
echo "🔧 수동 배포를 위한 환경 변수 준비..."

# SSM Parameter에서 값 가져오기
echo "SSM Parameter에서 환경 변수 수집 중..."

DB_USERNAME=$(aws ssm get-parameter --name "/interv/db/username" --with-decryption --region $AWS_REGION --query 'Parameter.Value' --output text 2>/dev/null)
DB_PASSWORD=$(aws ssm get-parameter --name "/interv/db/password" --with-decryption --region $AWS_REGION --query 'Parameter.Value' --output text 2>/dev/null)
COGNITO_USER_POOL_ID=$(aws ssm get-parameter --name "/interv/cognito/user-pool-id" --region $AWS_REGION --query 'Parameter.Value' --output text 2>/dev/null)
COGNITO_CLIENT_ID=$(aws ssm get-parameter --name "/interv/cognito/client-id" --region $AWS_REGION --query 'Parameter.Value' --output text 2>/dev/null)
COGNITO_CLIENT_SECRET=$(aws ssm get-parameter --name "/interv/cognito/client-secret" --with-decryption --region $AWS_REGION --query 'Parameter.Value' --output text 2>/dev/null)

echo "환경 변수 수집 결과:"
echo "  DB_USERNAME: ${DB_USERNAME:0:3}***"
echo "  DB_PASSWORD: ${DB_PASSWORD:0:3}***"
echo "  COGNITO_USER_POOL_ID: $COGNITO_USER_POOL_ID"
echo "  COGNITO_CLIENT_ID: $COGNITO_CLIENT_ID"
echo "  COGNITO_CLIENT_SECRET: ${COGNITO_CLIENT_SECRET:0:5}***"

# 6. 수동 Docker 실행 스크립트 생성
echo ""
echo "📝 수동 Docker 실행 스크립트 생성..."

cat > manual_deploy.sh << EOF
#!/bin/bash
# 수동 Docker 배포 스크립트

echo "🚀 수동 Docker 배포 시작..."

# ECR 로그인
echo "1. ECR 로그인..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_URI

# 기존 컨테이너 정리
echo "2. 기존 컨테이너 정리..."
docker stop interv-manual 2>/dev/null || true
docker rm interv-manual 2>/dev/null || true

# 최신 이미지 pull
echo "3. 최신 이미지 다운로드..."
docker pull $ECR_URI/$IMAGE_NAME

# 컨테이너 실행 (포트 8080)
echo "4. 컨테이너 실행..."
docker run -d \\
  --name interv-manual \\
  -p 8080:8080 \\
  -e SERVER_PORT=8080 \\
  -e AWS_REGION=$AWS_REGION \\
  -e DB_PORT=3306 \\
  -e SPRING_PROFILES_ACTIVE=prod \\
  -e COGNITO_LOGOUT_REDIRECT_URI="https://interv.swote.dev/" \\
  -e JPA_DDL_AUTO=update \\
  -e COGNITO_REDIRECT_URI="https://interv.swote.dev/login/oauth2/code/cognito" \\
  -e DB_NAME=interv \\
  -e SHOW_SQL=false \\
  -e S3_BUCKET_NAME=interv-storage-t8osm0s8 \\
  -e FORMAT_SQL=false \\
  -e DB_HOST=interv-db.cjygi4mssp23.ap-northeast-2.rds.amazonaws.com \\
  -e DB_USERNAME="$DB_USERNAME" \\
  -e DB_PASSWORD="$DB_PASSWORD" \\
  -e COGNITO_USER_POOL_ID="$COGNITO_USER_POOL_ID" \\
  -e COGNITO_CLIENT_ID="$COGNITO_CLIENT_ID" \\
  -e COGNITO_CLIENT_SECRET="$COGNITO_CLIENT_SECRET" \\
  $ECR_URI/$IMAGE_NAME

if [ \$? -eq 0 ]; then
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
  echo "  - 외부: http://$INSTANCE_IP:8080"
  echo "  - Health: http://$INSTANCE_IP:8080/actuator/health"
  echo ""
  echo "🔍 실시간 로그 모니터링:"
  echo "docker logs -f interv-manual"
else
  echo "❌ 컨테이너 시작 실패"
  echo "Docker 로그:"
  docker logs interv-manual 2>/dev/null || echo "로그 없음"
fi
EOF

chmod +x manual_deploy.sh

# 7. 인스턴스 연결 가이드
echo ""
echo "🔗 수동 배포 실행 가이드"
echo "======================"
echo ""
echo "1. 선택된 인스턴스에 연결:"
echo "   Instance ID: $SELECTED_INSTANCE"
echo "   Public IP: $INSTANCE_IP"
echo ""
echo "📝 AWS Session Manager 연결 (권장):"
echo "aws ssm start-session --target $SELECTED_INSTANCE --region $AWS_REGION"
echo ""
echo "🔧 또는 SSH 연결 (키 페어 필요):"
echo "ssh -i your-key.pem ec2-user@$INSTANCE_IP"
echo ""
echo "2. 인스턴스에서 실행할 명령어들:"
echo ""
echo "# AWS CLI 설정 확인"
echo "aws sts get-caller-identity"
echo ""
echo "# Docker 상태 확인"
echo "docker ps"
echo "docker info"
echo ""
echo "# 수동 배포 스크립트 다운로드 및 실행"
echo "curl -o manual_deploy.sh \"데이터\" # 스크립트 내용을 복사해서 파일로 저장"
echo "chmod +x manual_deploy.sh"
echo "./manual_deploy.sh"
echo ""

# 8. 로컬 테스트 방법
echo "🏠 로컬 테스트 방법 (대안)"
echo "========================"
echo ""
echo "인스턴스 접속이 어려운 경우 로컬에서 테스트:"
echo ""
echo "1. 환경변수 파일 생성:"
cat > .env.local << EOF
SERVER_PORT=8080
AWS_REGION=$AWS_REGION
DB_PORT=3306
SPRING_PROFILES_ACTIVE=prod
COGNITO_LOGOUT_REDIRECT_URI=https://interv.swote.dev/
JPA_DDL_AUTO=update
COGNITO_REDIRECT_URI=https://interv.swote.dev/login/oauth2/code/cognito
DB_NAME=interv
SHOW_SQL=false
S3_BUCKET_NAME=interv-storage-t8osm0s8
FORMAT_SQL=false
DB_HOST=interv-db.cjygi4mssp23.ap-northeast-2.rds.amazonaws.com
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
COGNITO_USER_POOL_ID=$COGNITO_USER_POOL_ID
COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID
COGNITO_CLIENT_SECRET=$COGNITO_CLIENT_SECRET
EOF

echo ""
echo "2. 로컬 Docker 실행:"
echo "docker run -d --name interv-local -p 8080:8080 --env-file .env.local $ECR_URI/$IMAGE_NAME"
echo ""
echo "3. 로컬 테스트:"
echo "curl http://localhost:8080/actuator/health"
echo ""

# 9. 디버깅 체크리스트
echo "🐛 502 오류 디버깅 체크리스트"
echo "=========================="
echo ""
echo "✅ 확인할 사항들:"
echo "1. 컨테이너가 실제로 시작되었는가?"
echo "   → docker ps"
echo ""
echo "2. 애플리케이션이 8080 포트에서 리스닝하는가?"
echo "   → curl http://localhost:8080/actuator/health"
echo ""
echo "3. 데이터베이스 연결이 되는가?"
echo "   → 애플리케이션 로그에서 DB 연결 오류 확인"
echo ""
echo "4. 환경변수가 올바른가?"
echo "   → docker exec interv-manual env | grep DB"
echo ""
echo "5. Security Group에서 8080 포트가 열려있는가?"
echo "   → EC2 콘솔에서 보안 그룹 확인"
echo ""
echo "6. Target Group Health Check 설정이 올바른가?"
echo "   → /actuator/health 경로로 설정되어 있는지 확인"
echo ""
echo "🚀 다음 단계:"
echo "1. 위의 가이드대로 인스턴스에 접속"
echo "2. manual_deploy.sh 실행"
echo "3. 로그 확인 및 문제 해결"
echo "4. 문제 해결 후 ECS 서비스 재배포"

echo ""
echo "💾 생성된 파일들:"
echo "  - manual_deploy.sh: 인스턴스에서 실행할 수동 배포 스크립트"
echo "  - .env.local: 로컬 테스트용 환경변수 파일"