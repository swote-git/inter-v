#!/bin/bash

set -e

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

APP_NAME="interv"
AWS_REGION="ap-northeast-2"

echo -e "${RED}🗑️ AWS 리소스 완전 초기화 시작...${NC}"
echo -e "${YELLOW}⚠️ 이 작업은 모든 InterV 관련 AWS 리소스를 삭제합니다!${NC}"
echo -e "${YELLOW}⚠️ 계속하시겠습니까? (yes/no)${NC}"
read -p "" confirm

if [ "$confirm" != "yes" ]; then
    echo "취소되었습니다."
    exit 0
fi

echo -e "${BLUE}🔍 현재 리소스 상태 확인 중...${NC}"

# 1. Auto Scaling Group 삭제 (가장 먼저 - 다른 리소스 의존성)
echo -e "${BLUE}🔄 Auto Scaling Group 삭제 중...${NC}"
ASG_NAME="${APP_NAME}-asg"
if aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG_NAME" >/dev/null 2>&1; then
    echo "  ├─ ASG 발견: $ASG_NAME"
    echo "  ├─ 인스턴스 개수를 0으로 설정..."
    aws autoscaling update-auto-scaling-group \
        --auto-scaling-group-name "$ASG_NAME" \
        --min-size 0 \
        --desired-capacity 0 \
        --max-size 0
    
    echo "  ├─ 인스턴스 종료 대기 (60초)..."
    sleep 60
    
    echo "  ├─ ASG 삭제..."
    aws autoscaling delete-auto-scaling-group \
        --auto-scaling-group-name "$ASG_NAME" \
        --force-delete
    
    echo "  └─ ASG 삭제 완료 대기 (30초)..."
    sleep 30
else
    echo "  └─ ASG 없음"
fi

# 2. Launch Template 삭제
echo -e "${BLUE}🚀 Launch Template 삭제 중...${NC}"
LT_NAMES=("${APP_NAME}-lt" "${APP_NAME}-lt-*")
for LT_PATTERN in "${LT_NAMES[@]}"; do
    LT_IDS=$(aws ec2 describe-launch-templates \
        --query "LaunchTemplates[?starts_with(LaunchTemplateName, '$APP_NAME-lt')].LaunchTemplateId" \
        --output text 2>/dev/null || echo "")
    
    if [ -n "$LT_IDS" ]; then
        for LT_ID in $LT_IDS; do
            echo "  ├─ Launch Template 삭제: $LT_ID"
            aws ec2 delete-launch-template --launch-template-id "$LT_ID" || true
        done
    fi
done
echo "  └─ Launch Template 삭제 완료"

# 3. Load Balancer Listeners 삭제 (먼저)
echo -e "${BLUE}⚖️ Load Balancer 삭제 중...${NC}"
ALB_ARN=$(aws elbv2 describe-load-balancers \
    --names "${APP_NAME}-alb" \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text 2>/dev/null || echo "None")

if [ "$ALB_ARN" != "None" ] && [ "$ALB_ARN" != "null" ]; then
    echo "  ├─ ALB 발견: $ALB_ARN"
    
    # Listeners 삭제
    echo "  ├─ Listeners 삭제 중..."
    LISTENER_ARNS=$(aws elbv2 describe-listeners \
        --load-balancer-arn "$ALB_ARN" \
        --query 'Listeners[].ListenerArn' \
        --output text 2>/dev/null || echo "")
    
    for LISTENER_ARN in $LISTENER_ARNS; do
        if [ "$LISTENER_ARN" != "None" ]; then
            echo "    ├─ Listener 삭제: $LISTENER_ARN"
            aws elbv2 delete-listener --listener-arn "$LISTENER_ARN" || true
        fi
    done
    
    # ALB 삭제
    echo "  ├─ ALB 삭제..."
    aws elbv2 delete-load-balancer --load-balancer-arn "$ALB_ARN"
    
    echo "  └─ ALB 삭제 완료 대기 (60초)..."
    sleep 60
else
    echo "  └─ ALB 없음"
fi

# 4. Target Group 삭제
echo -e "${BLUE}🎯 Target Group 삭제 중...${NC}"
TG_ARN=$(aws elbv2 describe-target-groups \
    --names "${APP_NAME}-tg" \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text 2>/dev/null || echo "None")

if [ "$TG_ARN" != "None" ] && [ "$TG_ARN" != "null" ]; then
    echo "  ├─ Target Group 발견: $TG_ARN"
    aws elbv2 delete-target-group --target-group-arn "$TG_ARN"
    echo "  └─ Target Group 삭제 완료"
else
    echo "  └─ Target Group 없음"
fi

# 5. RDS 인스턴스 삭제 (오래 걸림)
echo -e "${BLUE}🗄️ RDS 인스턴스 삭제 중...${NC}"
DB_IDENTIFIER="${APP_NAME}-db"
if aws rds describe-db-instances --db-instance-identifier "$DB_IDENTIFIER" >/dev/null 2>&1; then
    echo "  ├─ RDS 인스턴스 발견: $DB_IDENTIFIER"
    echo "  ├─ RDS 삭제 시작 (5-10분 소요 예상)..."
    aws rds delete-db-instance \
        --db-instance-identifier "$DB_IDENTIFIER" \
        --skip-final-snapshot \
        --delete-automated-backups
    
    echo "  └─ RDS 삭제 진행 중... (백그라운드에서 계속됨)"
else
    echo "  └─ RDS 인스턴스 없음"
fi

# 6. DB Subnet Group 삭제 (RDS가 완전히 삭제된 후)
echo -e "${BLUE}🗄️ DB Subnet Group 삭제 대기 중...${NC}"
DB_SUBNET_GROUP="${APP_NAME}-db-subnet-group"

# RDS 완전 삭제 대기
if aws rds describe-db-instances --db-instance-identifier "$DB_IDENTIFIER" >/dev/null 2>&1; then
    echo "  ├─ RDS 완전 삭제 대기 중... (최대 10분)"
    aws rds wait db-instance-deleted --db-instance-identifier "$DB_IDENTIFIER" --cli-read-timeout 600 || true
fi

# DB Subnet Group 삭제
if aws rds describe-db-subnet-groups --db-subnet-group-name "$DB_SUBNET_GROUP" >/dev/null 2>&1; then
    echo "  ├─ DB Subnet Group 삭제: $DB_SUBNET_GROUP"
    aws rds delete-db-subnet-group --db-subnet-group-name "$DB_SUBNET_GROUP"
    echo "  └─ DB Subnet Group 삭제 완료"
else
    echo "  └─ DB Subnet Group 없음"
fi

# 7. IAM 리소스 삭제
echo -e "${BLUE}👤 IAM 리소스 삭제 중...${NC}"

# Instance Profile 삭제
INSTANCE_PROFILE="${APP_NAME}-ec2-profile"
if aws iam get-instance-profile --instance-profile-name "$INSTANCE_PROFILE" >/dev/null 2>&1; then
    echo "  ├─ Instance Profile에서 Role 제거..."
    aws iam remove-role-from-instance-profile \
        --instance-profile-name "$INSTANCE_PROFILE" \
        --role-name "${APP_NAME}-ec2-role" || true
    
    echo "  ├─ Instance Profile 삭제: $INSTANCE_PROFILE"
    aws iam delete-instance-profile --instance-profile-name "$INSTANCE_PROFILE"
else
    echo "  ├─ Instance Profile 없음"
fi

# Role Policy 삭제
ROLE_NAME="${APP_NAME}-ec2-role"
POLICY_NAME="${APP_NAME}-ec2-policy"
if aws iam get-role-policy --role-name "$ROLE_NAME" --policy-name "$POLICY_NAME" >/dev/null 2>&1; then
    echo "  ├─ Role Policy 삭제: $POLICY_NAME"
    aws iam delete-role-policy --role-name "$ROLE_NAME" --policy-name "$POLICY_NAME"
else
    echo "  ├─ Role Policy 없음"
fi

# IAM Role 삭제
if aws iam get-role --role-name "$ROLE_NAME" >/dev/null 2>&1; then
    echo "  ├─ IAM Role 삭제: $ROLE_NAME"
    aws iam delete-role --role-name "$ROLE_NAME"
else
    echo "  ├─ IAM Role 없음"
fi

echo "  └─ IAM 리소스 삭제 완료"

# 8. Elastic IP 삭제
echo -e "${BLUE}🌐 Elastic IP 삭제 중...${NC}"
EIP_IDS=$(aws ec2 describe-addresses \
    --query 'Addresses[?AssociationId==null].AllocationId' \
    --output text)

if [ -n "$EIP_IDS" ]; then
    for EIP_ID in $EIP_IDS; do
        if [ "$EIP_ID" != "None" ]; then
            echo "  ├─ EIP 삭제: $EIP_ID"
            aws ec2 release-address --allocation-id "$EIP_ID" || true
        fi
    done
    echo "  └─ 모든 미사용 EIP 삭제 완료"
else
    echo "  └─ 삭제할 EIP 없음"
fi

# 9. Route53 레코드 삭제 (선택사항)
echo -e "${BLUE}🌍 Route53 레코드 확인 중...${NC}"
DOMAIN_NAME="interv.swote.dev"
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
    --query "HostedZones[?Name=='swote.dev.'].Id" \
    --output text | cut -d'/' -f3)

if [ -n "$HOSTED_ZONE_ID" ] && [ "$HOSTED_ZONE_ID" != "None" ]; then
    echo "  ├─ Hosted Zone 발견: $HOSTED_ZONE_ID"
    
    # A 레코드 삭제 (ALB 연결)
    RECORD_SET=$(aws route53 list-resource-record-sets \
        --hosted-zone-id "$HOSTED_ZONE_ID" \
        --query "ResourceRecordSets[?Name=='$DOMAIN_NAME.' && Type=='A']" \
        --output json)
    
    if [ "$RECORD_SET" != "[]" ]; then
        echo "  ├─ A 레코드 삭제: $DOMAIN_NAME"
        # 실제 삭제는 수동으로 확인 후 진행하는 것이 안전
        echo "  └─ Route53 레코드는 수동으로 확인해주세요"
    else
        echo "  └─ 삭제할 A 레코드 없음"
    fi
else
    echo "  └─ Hosted Zone 없음"
fi

# 10. S3 버킷 정리 (내용물만 삭제, 버킷은 유지)
echo -e "${BLUE}🪣 S3 버킷 정리 중...${NC}"
S3_BUCKETS=$(aws s3api list-buckets \
    --query "Buckets[?starts_with(Name, '$APP_NAME-storage')].Name" \
    --output text)

if [ -n "$S3_BUCKETS" ]; then
    for BUCKET in $S3_BUCKETS; do
        if [ "$BUCKET" != "None" ]; then
            echo "  ├─ S3 버킷 내용물 삭제: $BUCKET"
            aws s3 rm "s3://$BUCKET" --recursive || true
            echo "  ├─ 버킷 자체는 유지 (데이터 보존)"
        fi
    done
    echo "  └─ S3 정리 완료"
else
    echo "  └─ 관련 S3 버킷 없음"
fi

# 11. Terraform State 초기화
echo -e "${BLUE}🏗️ Terraform State 초기화 중...${NC}"
if [ -d "infrastructure" ]; then
    cd infrastructure/
    
    if [ -f "terraform.tfstate" ]; then
        echo "  ├─ 기존 terraform.tfstate 백업..."
        cp terraform.tfstate terraform.tfstate.backup.$(date +%Y%m%d_%H%M%S)
        rm -f terraform.tfstate terraform.tfstate.backup
    fi
    
    if [ -d ".terraform" ]; then
        echo "  ├─ .terraform 디렉토리 삭제..."
        rm -rf .terraform
    fi
    
    echo "  └─ Terraform 초기화..."
    terraform init
    
    cd ..
else
    echo "  └─ infrastructure 디렉토리 없음"
fi

echo ""
echo -e "${GREEN}🎉 AWS 리소스 완전 초기화 완료!${NC}"
echo ""
echo -e "${BLUE}📋 정리된 리소스:${NC}"
echo "  ✅ Auto Scaling Group & EC2 인스턴스들"
echo "  ✅ Launch Template"
echo "  ✅ Application Load Balancer & Target Group"
echo "  ✅ RDS 인스턴스 & DB Subnet Group"
echo "  ✅ IAM Role, Policy, Instance Profile"
echo "  ✅ 미사용 Elastic IP"
echo "  ✅ S3 버킷 내용물"
echo "  ✅ Terraform State"
echo ""
echo -e "${BLUE}🚀 다음 단계:${NC}"
echo "1. GitHub Actions 실행 또는 로컬에서 terraform apply"
echo "2. 모든 리소스가 처음부터 깨끗하게 생성됩니다"
echo ""
echo -e "${YELLOW}💡 참고:${NC}"
echo "• Route53 레코드는 수동으로 확인해주세요"
echo "• S3 버킷은 데이터 보존을 위해 유지했습니다"
echo "• RDS 삭제는 백그라운드에서 계속 진행됩니다"