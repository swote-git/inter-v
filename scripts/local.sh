#!/bin/bash
# 긴급 Import 스크립트 - 로컬에서 확실하게 처리

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${RED}🚨 긴급 Import 모드 - 기존 리소스 강제 Import${NC}"
echo "=================================================="

cd infrastructure/

# 환경변수 설정
export TF_VAR_db_password="interv2025!"
export TF_VAR_key_pair_name="interv-keypair"
export TF_VAR_aws_region="ap-northeast-2"
export TF_VAR_app_name="interv"
export TF_VAR_domain_name="interv.swote.dev"

echo -e "${BLUE}🏗️ Terraform 초기화...${NC}"
terraform init

echo ""
echo -e "${YELLOW}⚠️ 현재 Terraform State 상태 확인:${NC}"
terraform state list | head -10 || echo "State가 비어있거나 오류"

echo ""
echo -e "${RED}🔥 강제 Import 시작 (에러 무시하고 계속 진행)${NC}"
echo ""

# 각 리소스별로 강제 import (에러가 나도 계속 진행)
force_import() {
    local name="$1"
    local terraform_resource="$2"
    local aws_resource_id="$3"
    
    echo -e "${BLUE}📥 $name Import 시도...${NC}"
    
    # 이미 state에 있으면 스킫
    if terraform state show "$terraform_resource" >/dev/null 2>&1; then
        echo -e "${GREEN}  ✅ 이미 존재: $name${NC}"
        return 0
    fi
    
    # Import 시도 (에러 무시)
    if terraform import "$terraform_resource" "$aws_resource_id" 2>/dev/null; then
        echo -e "${GREEN}  ✅ Import 성공: $name${NC}"
    else
        echo -e "${YELLOW}  ⚠️ Import 실패: $name (계속 진행)${NC}"
    fi
}

# 1. 핵심 문제 리소스들만 집중 Import
echo -e "${RED}🎯 문제가 된 핵심 리소스들 Import:${NC}"

force_import "DB Subnet Group" \
    "aws_db_subnet_group.main" \
    "interv-db-subnet-group"

force_import "IAM Role" \
    "aws_iam_role.ec2_role" \
    "interv-ec2-role"

# Load Balancer ARN 확인 후 Import
echo -e "${BLUE}🔍 Load Balancer ARN 확인 중...${NC}"
ALB_ARN=$(aws elbv2 describe-load-balancers --names interv-alb --query 'LoadBalancers[0].LoadBalancerArn' --output text 2>/dev/null || echo "None")
if [ "$ALB_ARN" != "None" ] && [ "$ALB_ARN" != "null" ]; then
    echo -e "${YELLOW}  발견된 ALB ARN: $ALB_ARN${NC}"
    force_import "Application Load Balancer" \
        "aws_lb.main" \
        "$ALB_ARN"
else
    echo -e "${RED}  ❌ Load Balancer를 찾을 수 없습니다!${NC}"
fi

# Target Group ARN 확인 후 Import  
echo -e "${BLUE}🔍 Target Group ARN 확인 중...${NC}"
TG_ARN=$(aws elbv2 describe-target-groups --names interv-tg --query 'TargetGroups[0].TargetGroupArn' --output text 2>/dev/null || echo "None")
if [ "$TG_ARN" != "None" ] && [ "$TG_ARN" != "null" ]; then
    echo -e "${YELLOW}  발견된 TG ARN: $TG_ARN${NC}"
    force_import "Target Group" \
        "aws_lb_target_group.app" \
        "$TG_ARN"
else
    echo -e "${RED}  ❌ Target Group을 찾을 수 없습니다!${NC}"
fi

# 나머지 중요 리소스들도 Import
echo ""
echo -e "${BLUE}📦 추가 리소스 Import:${NC}"

force_import "IAM Role Policy" \
    "aws_iam_role_policy.ec2_policy" \
    "interv-ec2-role:interv-ec2-policy"

force_import "IAM Instance Profile" \
    "aws_iam_instance_profile.ec2_profile" \
    "interv-ec2-profile"

force_import "Auto Scaling Group" \
    "aws_autoscaling_group.app" \
    "interv-asg"

# Launch Template
LT_ID=$(aws ec2 describe-launch-templates --launch-template-names interv-lt --query 'LaunchTemplates[0].LaunchTemplateId' --output text 2>/dev/null || echo "None")
if [ "$LT_ID" != "None" ] && [ "$LT_ID" != "null" ]; then
    force_import "Launch Template" \
        "aws_launch_template.app" \
        "$LT_ID"
fi

force_import "RDS Instance" \
    "aws_db_instance.main" \
    "interv-db"

# S3 Bucket
S3_BUCKET=$(aws s3api list-buckets --query "Buckets[?starts_with(Name, 'interv-storage-')].Name" --output text 2>/dev/null || echo "")
if [ -n "$S3_BUCKET" ] && [ "$S3_BUCKET" != "None" ]; then
    force_import "S3 Bucket" \
        "aws_s3_bucket.app_storage" \
        "$S3_BUCKET"
fi

echo ""
echo -e "${GREEN}✅ Import 과정 완료!${NC}"
echo ""

# State 확인
echo -e "${BLUE}📊 현재 Terraform State:${NC}"
terraform state list | wc -l | xargs echo "Total resources in state:"

echo ""
echo -e "${BLUE}🧪 Plan 테스트...${NC}"

# Plan 실행하여 상태 확인
if terraform plan -detailed-exitcode -out=tfplan; then
    PLAN_EXIT_CODE=$?
    case $PLAN_EXIT_CODE in
        0)
            echo -e "${GREEN}✅ 변경사항 없음 - Perfect!${NC}"
            ;;
        2)
            echo -e "${YELLOW}⚠️ 일부 변경사항 있음 - 정상 상황${NC}"
            echo "변경사항을 확인해보세요:"
            terraform show tfplan | head -20
            ;;
    esac
    
    echo ""
    echo -e "${GREEN}🎉 Import 성공! 이제 Apply 가능합니다.${NC}"
    echo ""
    echo -e "${BLUE}📋 다음 단계 선택:${NC}"
    echo "1. terraform apply tfplan     (로컬에서 직접 배포)"
    echo "2. git commit & push          (GitHub Actions 자동 배포)"
    echo ""
    echo -e "${YELLOW}💡 추천: 로컬에서 terraform apply tfplan 실행${NC}"
    
else
    echo -e "${RED}❌ Plan 여전히 실패${NC}"
    echo ""
    echo -e "${YELLOW}🔍 상세 에러 확인:${NC}"
    terraform plan 2>&1 | grep -A 5 -B 5 "Error:" || echo "에러 정보 없음"
    
    echo ""
    echo -e "${RED}🆘 대안 방법:${NC}"
    echo "1. 기존 리소스 수동 삭제 후 재생성"
    echo "2. Terraform state 초기화 후 새로 시작"
    echo "3. AWS 콘솔에서 직접 문제 리소스 확인"
fi