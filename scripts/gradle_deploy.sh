#!/bin/bash
# scripts/local_gradle_deploy.sh - 로컬에서 Gradle 빌드 및 AWS 배포

set -e

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 설정
APP_NAME="interv"
AWS_REGION="ap-northeast-2"

echo -e "${BLUE}🚀 InterV 로컬 빌드 및 배포 시작...${NC}"
echo "======================================="

# 0. 사전 조건 확인
echo -e "${BLUE}📋 사전 조건 확인...${NC}"

# AWS CLI 확인
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI가 설치되지 않았습니다.${NC}"
    exit 1
fi

# Java 확인
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java가 설치되지 않았습니다.${NC}"
    exit 1
fi

# Gradle wrapper 확인
if [ ! -f "BE/inter-v/gradlew" ]; then
    echo -e "${RED}❌ Gradle wrapper를 찾을 수 없습니다.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 사전 조건 확인 완료${NC}"

# 1. S3 버킷 이름 가져오기 (Terraform output 또는 직접 찾기)
echo -e "${BLUE}🪣 S3 버킷 확인...${NC}"

S3_BUCKET=""
if [ -d "infrastructure" ] && [ -f "infrastructure/terraform.tfstate" ]; then
    cd infrastructure/
    S3_BUCKET=$(terraform output -raw s3_bucket_name 2>/dev/null || echo "")
    cd ..
fi

if [ -z "$S3_BUCKET" ]; then
    echo -e "${YELLOW}⚠️ Terraform output에서 S3 버킷을 찾을 수 없습니다. AWS에서 직접 찾는 중...${NC}"
    S3_BUCKET=$(aws s3api list-buckets --query "Buckets[?starts_with(Name, '${APP_NAME}-storage')].Name" --output text | head -1)
fi

if [ -z "$S3_BUCKET" ] || [ "$S3_BUCKET" = "None" ]; then
    echo -e "${RED}❌ S3 버킷을 찾을 수 없습니다. 인프라가 배포되어 있는지 확인해주세요.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ S3 버킷 확인: $S3_BUCKET${NC}"

# 2. Gradle 빌드
echo -e "${BLUE}📦 Gradle 빌드 시작...${NC}"
cd BE/inter-v

# gradlew 실행 권한 부여
chmod +x ./gradlew

echo "  ├─ Clean & Build..."
./gradlew clean build -x test

# JAR 파일 찾기
JAR_FILE=$(find build/libs -name "*.jar" | grep -v plain | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}❌ 빌드된 JAR 파일을 찾을 수 없습니다.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 빌드 완료: $JAR_FILE${NC}"

# 파일 크기 확인
FILE_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
echo -e "${BLUE}📦 JAR 파일 크기: $FILE_SIZE${NC}"

cd ../..

# 3. S3에 JAR 파일 업로드
echo -e "${BLUE}📤 S3에 애플리케이션 업로드...${NC}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
S3_KEY_TIMESTAMPED="releases/$TIMESTAMP/$APP_NAME.jar"
S3_KEY_LATEST="releases/latest/$APP_NAME.jar"

echo "  ├─ 타임스탬프 버전 업로드: $S3_KEY_TIMESTAMPED"
aws s3 cp "BE/inter-v/$JAR_FILE" "s3://$S3_BUCKET/$S3_KEY_TIMESTAMPED" \
    --metadata "build-time=$TIMESTAMP,build-source=local"

echo "  ├─ 최신 버전 업로드: $S3_KEY_LATEST"
aws s3 cp "BE/inter-v/$JAR_FILE" "s3://$S3_BUCKET/$S3_KEY_LATEST" \
    --metadata "build-time=$TIMESTAMP,build-source=local"

echo -e "${GREEN}✅ S3 업로드 완료${NC}"

# 4. AWS CLI 버전 및 Auto Scaling Group 확인
echo -e "${BLUE}🔄 AWS CLI 및 Auto Scaling Group 확인...${NC}"

# AWS CLI 버전 확인
AWS_CLI_VERSION=$(aws --version 2>&1 | head -1)
echo "  ├─ AWS CLI 버전: $AWS_CLI_VERSION"

ASG_NAME="${APP_NAME}-asg"

# Auto Scaling Group 존재 확인
echo "  ├─ Auto Scaling Group 확인 중: $ASG_NAME"
ASG_EXISTS=$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "$ASG_NAME" \
    --query 'length(AutoScalingGroups)' \
    --output text 2>/dev/null || echo "0")

if [ "$ASG_EXISTS" = "0" ]; then
    echo -e "${RED}❌ Auto Scaling Group '$ASG_NAME'을 찾을 수 없습니다.${NC}"
    echo -e "${YELLOW}💡 인프라가 완전히 배포되지 않았거나 ASG 이름이 다를 수 있습니다.${NC}"
    
    # 모든 ASG 나열
    echo -e "${BLUE}📋 현재 존재하는 Auto Scaling Groups:${NC}"
    aws autoscaling describe-auto-scaling-groups \
        --query 'AutoScalingGroups[].AutoScalingGroupName' \
        --output table 2>/dev/null || echo "  └─ ASG 목록 조회 실패"
    
    # 수동 배포 옵션 제공
    echo ""
    echo -e "${YELLOW}🔧 대안 방법:${NC}"
    echo "1. EC2 콘솔에서 수동으로 인스턴스 종료 (새 인스턴스가 자동 생성됨)"
    echo "2. Launch Template을 수동으로 업데이트"
    echo "3. 인프라 재배포: cd infrastructure && terraform apply"
    exit 1
fi

echo -e "${GREEN}✅ Auto Scaling Group 확인: $ASG_NAME${NC}"

# 5. 인스턴스 갱신 시작 (여러 방법 시도)
echo -e "${BLUE}🔄 인스턴스 갱신 시작...${NC}"

# 방법 1: start-instance-refresh (AWS CLI v2)
echo "  ├─ Instance Refresh 시도 중..."
REFRESH_ID=""

# AWS CLI v2 명령어 시도
if aws autoscaling start-instance-refresh \
    --auto-scaling-group-name "$ASG_NAME" \
    --preferences MinHealthyPercentage=50,InstanceWarmup=300 \
    --region "$AWS_REGION" >/dev/null 2>&1; then
    
    REFRESH_ID=$(aws autoscaling start-instance-refresh \
        --auto-scaling-group-name "$ASG_NAME" \
        --preferences MinHealthyPercentage=50,InstanceWarmup=300 \
        --region "$AWS_REGION" \
        --query 'InstanceRefreshId' \
        --output text 2>/dev/null)
    
    if [ -n "$REFRESH_ID" ] && [ "$REFRESH_ID" != "None" ]; then
        echo -e "${GREEN}✅ Instance Refresh 시작됨: $REFRESH_ID${NC}"
    else
        echo -e "${YELLOW}⚠️ Instance Refresh ID를 가져올 수 없습니다${NC}"
        REFRESH_ID=""
    fi
else
    echo -e "${YELLOW}⚠️ Instance Refresh 실패 - 대안 방법 시도${NC}"
fi

# 방법 2: 대안 - 인스턴스 직접 교체
if [ -z "$REFRESH_ID" ]; then
    echo "  ├─ 대안: 기존 인스턴스 직접 교체..."
    
    # 현재 인스턴스 목록 가져오기
    INSTANCE_IDS=$(aws autoscaling describe-auto-scaling-groups \
        --auto-scaling-group-names "$ASG_NAME" \
        --query 'AutoScalingGroups[0].Instances[].InstanceId' \
        --output text 2>/dev/null)
    
    if [ -n "$INSTANCE_IDS" ]; then
        echo "  ├─ 발견된 인스턴스: $INSTANCE_IDS"
        
        # 인스턴스 하나씩 교체 (Rolling Update)
        for INSTANCE_ID in $INSTANCE_IDS; do
            echo "  ├─ 인스턴스 교체 중: $INSTANCE_ID"
            
            # 인스턴스를 Unhealthy로 표시하여 교체 유도
            aws autoscaling set-instance-health \
                --instance-id "$INSTANCE_ID" \
                --health-status Unhealthy \
                --region "$AWS_REGION" 2>/dev/null || true
            
            echo "  ├─ 새 인스턴스 생성 대기 (60초)..."
            sleep 60
            
            # 하나씩 교체하므로 첫 번째만 교체하고 중단
            break
        done
        
        echo -e "${GREEN}✅ 인스턴스 교체 시작됨${NC}"
    else
        echo -e "${RED}❌ 교체할 인스턴스를 찾을 수 없습니다${NC}"
    fi
fi

# 6. 갱신 상태 모니터링
echo -e "${BLUE}⏳ 인스턴스 갱신 진행 상황 모니터링...${NC}"

echo "  ├─ 갱신이 시작될 때까지 대기 중..."
sleep 30

# 진행 상황을 주기적으로 확인
for i in {1..30}; do
    REFRESH_STATUS=$(aws autoscaling describe-instance-refreshes \
        --auto-scaling-group-name "$ASG_NAME" \
        --instance-refresh-ids "$REFRESH_ID" \
        --query 'InstanceRefreshes[0].Status' \
        --output text)
    
    PERCENTAGE=$(aws autoscaling describe-instance-refreshes \
        --auto-scaling-group-name "$ASG_NAME" \
        --instance-refresh-ids "$REFRESH_ID" \
        --query 'InstanceRefreshes[0].PercentageComplete' \
        --output text 2>/dev/null || echo "0")
    
    echo "  ├─ 상태: $REFRESH_STATUS, 진행률: ${PERCENTAGE}% ($i/30)"
    
    case $REFRESH_STATUS in
        "Successful")
            echo -e "${GREEN}✅ 인스턴스 갱신 완료!${NC}"
            break
            ;;
        "Failed"|"Cancelled")
            echo -e "${RED}❌ 인스턴스 갱신 실패: $REFRESH_STATUS${NC}"
            exit 1
            ;;
        "InProgress"|"Pending")
            if [ $i -eq 30 ]; then
                echo -e "${YELLOW}⚠️ 갱신이 아직 진행 중입니다. 백그라운드에서 계속됩니다.${NC}"
            else
                sleep 30
            fi
            ;;
    esac
done

# 7. 애플리케이션 상태 확인
echo -e "${BLUE}🏥 애플리케이션 상태 확인...${NC}"

HEALTH_URL="https://interv.swote.dev/actuator/health"
echo "  ├─ Health Check URL: $HEALTH_URL"

# 애플리케이션 시작 대기
echo "  ├─ 애플리케이션 시작 대기 (60초)..."
sleep 60

# Health Check 시도
HEALTH_SUCCESS=false
for i in {1..12}; do
    echo "  ├─ Health Check 시도 $i/12..."
    
    if curl -f -s "$HEALTH_URL" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 애플리케이션이 정상적으로 실행 중입니다!${NC}"
        HEALTH_SUCCESS=true
        
        # 상세 Health 정보 출력
        echo "  └─ Health 상태:"
        curl -s "$HEALTH_URL" | python3 -m json.tool 2>/dev/null || echo "    (Health 응답 확인됨)"
        break
    fi
    
    if [ $i -lt 12 ]; then
        echo "    └─ 대기 중... (30초 후 재시도)"
        sleep 30
    fi
done

if [ "$HEALTH_SUCCESS" = false ]; then
    echo -e "${YELLOW}⚠️ Health Check에 실패했습니다.${NC}"
    echo -e "${BLUE}💡 다음을 확인해보세요:${NC}"
    echo "  • 애플리케이션이 아직 시작 중일 수 있습니다 (추가 대기 필요)"
    echo "  • EC2 인스턴스 로그: AWS Console에서 확인"
    echo "  • Security Group 설정 확인"
    echo "  • ALB Target Group 상태 확인"
fi

# 8. 배포 결과 요약
echo ""
echo -e "${GREEN}🎉 로컬 빌드 및 배포 완료!${NC}"
echo "================================="
echo ""
echo -e "${BLUE}📋 배포 정보:${NC}"
echo "  • 빌드 시간: $TIMESTAMP"
echo "  • JAR 파일: $JAR_FILE ($FILE_SIZE)"
echo "  • S3 버킷: $S3_BUCKET"
echo "  • 인스턴스 갱신 ID: $REFRESH_ID"
echo ""
echo -e "${BLUE}🌐 접속 정보:${NC}"
echo "  • 애플리케이션: https://interv.swote.dev"
echo "  • Health Check: https://interv.swote.dev/actuator/health"
echo "  • Swagger UI: https://interv.swote.dev/swagger-ui.html"
echo ""
echo -e "${BLUE}📊 모니터링:${NC}"
echo "  • AWS Console에서 Auto Scaling Group 상태 확인"
echo "  • CloudWatch Logs에서 애플리케이션 로그 확인"
echo "  • ALB Target Group에서 인스턴스 상태 확인"
echo ""

if [ "$HEALTH_SUCCESS" = true ]; then
    echo -e "${GREEN}✅ 배포가 성공적으로 완료되었습니다!${NC}"
else
    echo -e "${YELLOW}⚠️ 배포는 완료되었지만 Health Check를 확인해주세요.${NC}"
fi

echo ""
echo -e "${BLUE}💡 다음 단계:${NC}"
echo "1. GitHub Actions workflow 수정 (Maven → Gradle)"
echo "2. 애플리케이션 로그 모니터링"
echo "3. 성능 및 보안 설정 최적화"