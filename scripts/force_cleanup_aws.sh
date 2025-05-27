#!/bin/bash
# 남아있는 InterV 리소스 강제 정리 (의존성 순서 고려)

set -e  # 에러시 중단하지 않고 계속 진행하도록 주석처리 가능

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

REGION="ap-northeast-2"
APP_NAME="interv"

echo -e "${RED}🗑️ InterV 남은 리소스 강제 정리${NC}"
echo -e "${RED}================================${NC}"
echo -e "${YELLOW}⚠️ 이 스크립트는 InterV 관련 모든 남은 리소스를 강제 삭제합니다!${NC}"
echo ""
echo -e "${RED}계속하시겠습니까? (FORCE 입력)${NC}"
read -r confirmation
if [ "$confirmation" != "FORCE" ]; then
    echo "취소되었습니다."
    exit 0
fi

# 안전한 삭제 함수 (실패해도 계속 진행)
force_delete() {
    local resource_type="$1"
    local resource_id="$2"
    local delete_command="$3"
    
    echo -e "${BLUE}🗑️ $resource_type 삭제 시도: $resource_id${NC}"
    
    if eval "$delete_command" 2>/dev/null; then
        echo -e "${GREEN}  ✅ 삭제 완료: $resource_type${NC}"
        return 0
    else
        echo -e "${YELLOW}  ⚠️ 삭제 실패 (이미 없거나 의존성 문제): $resource_type${NC}"
        return 1
    fi
}

# 대기 함수 (짧게 설정)
wait_for_deletion() {
    local resource_type="$1"
    local check_command="$2"
    local max_wait="${3:-60}"  # 기본 1분
    
    echo -e "${YELLOW}⏳ $resource_type 삭제 대기 중... (최대 ${max_wait}초)${NC}"
    
    local waited=0
    while [ $waited -lt $max_wait ]; do
        if ! eval "$check_command" >/dev/null 2>&1; then
            echo -e "${GREEN}  ✅ $resource_type 삭제 확인${NC}"
            return 0
        fi
        sleep 5
        waited=$((waited + 5))
        echo -n "."
    done
    echo ""
    echo -e "${YELLOW}  ⚠️ $resource_type 삭제 타임아웃 (계속 진행)${NC}"
    return 1
}

echo ""
echo -e "${RED}🚨 1단계: Load Balancer 및 Target Group 강제 삭제${NC}"
echo "=================================================="

# 1-1. Load Balancer Listener 삭제 (모든 InterV 관련)
echo -e "${BLUE}🎯 Load Balancer Listener 삭제...${NC}"
ALB_ARNS=$(aws elbv2 describe-load-balancers --region $REGION --query "LoadBalancers[?contains(LoadBalancerName, 'interv')].LoadBalancerArn" --output text 2>/dev/null || echo "")
for alb_arn in $ALB_ARNS; do
    if [ -n "$alb_arn" ]; then
        LISTENER_ARNS=$(aws elbv2 describe-listeners --load-balancer-arn "$alb_arn" --region $REGION --query 'Listeners[].ListenerArn' --output text 2>/dev/null || echo "")
        for listener_arn in $LISTENER_ARNS; do
            force_delete "Listener" "$listener_arn" \
                "aws elbv2 delete-listener --listener-arn $listener_arn --region $REGION"
        done
    fi
done

# 1-2. Target Group 강제 삭제 (모든 InterV 관련)
echo -e "${BLUE}🎯 Target Group 강제 삭제...${NC}"
TG_ARNS=$(aws elbv2 describe-target-groups --region $REGION --query "TargetGroups[?contains(TargetGroupName, 'interv')].TargetGroupArn" --output text 2>/dev/null || echo "")
for tg_arn in $TG_ARNS; do
    if [ -n "$tg_arn" ]; then
        force_delete "Target Group" "$tg_arn" \
            "aws elbv2 delete-target-group --target-group-arn $tg_arn --region $REGION"
    fi
done

# 1-3. Load Balancer 강제 삭제
echo -e "${BLUE}⚖️ Load Balancer 강제 삭제...${NC}"
for alb_arn in $ALB_ARNS; do
    if [ -n "$alb_arn" ]; then
        force_delete "Load Balancer" "$alb_arn" \
            "aws elbv2 delete-load-balancer --load-balancer-arn $alb_arn --region $REGION"
        
        # 삭제 대기 (짧게)
        wait_for_deletion "Load Balancer" \
            "aws elbv2 describe-load-balancers --load-balancer-arns $alb_arn --region $REGION" 120
    fi
done

echo ""
echo -e "${RED}🚨 2단계: Auto Scaling 및 EC2 리소스 강제 삭제${NC}"
echo "=============================================="

# 2-1. Auto Scaling Group 강제 삭제
echo -e "${BLUE}🔄 Auto Scaling Group 강제 삭제...${NC}"
ASG_EXISTS=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "${APP_NAME}-asg" --region $REGION --query 'length(AutoScalingGroups)' --output text 2>/dev/null || echo "0")
if [ "$ASG_EXISTS" != "0" ]; then
    # 인스턴스 수를 0으로 설정
    aws autoscaling update-auto-scaling-group \
        --auto-scaling-group-name "${APP_NAME}-asg" \
        --min-size 0 \
        --max-size 0 \
        --desired-capacity 0 \
        --region $REGION 2>/dev/null || true
    
    sleep 10  # 잠시 대기
    
    # 강제 삭제
    force_delete "Auto Scaling Group" "${APP_NAME}-asg" \
        "aws autoscaling delete-auto-scaling-group --auto-scaling-group-name ${APP_NAME}-asg --force-delete --region $REGION"
fi

# 2-2. Launch Template 삭제
echo -e "${BLUE}🚀 Launch Template 삭제...${NC}"
LT_IDS=$(aws ec2 describe-launch-templates --region $REGION --query "LaunchTemplates[?contains(LaunchTemplateName, 'interv')].LaunchTemplateId" --output text 2>/dev/null || echo "")
for lt_id in $LT_IDS; do
    if [ -n "$lt_id" ]; then
        force_delete "Launch Template" "$lt_id" \
            "aws ec2 delete-launch-template --launch-template-id $lt_id --region $REGION"
    fi
done

# 2-3. 남은 EC2 인스턴스 강제 종료
echo -e "${BLUE}💻 남은 EC2 인스턴스 강제 종료...${NC}"
INSTANCE_IDS=$(aws ec2 describe-instances --region $REGION --filters "Name=tag:Name,Values=*interv*" "Name=instance-state-name,Values=running,stopped,stopping" --query 'Reservations[*].Instances[].InstanceId' --output text 2>/dev/null || echo "")
for instance_id in $INSTANCE_IDS; do
    if [ -n "$instance_id" ]; then
        force_delete "EC2 Instance" "$instance_id" \
            "aws ec2 terminate-instances --instance-ids $instance_id --region $REGION"
    fi
done

echo ""
echo -e "${RED}🚨 3단계: EIP 강제 해제 및 삭제${NC}"
echo "=============================="

# 3-1. InterV 관련 EIP 해제 및 삭제
echo -e "${BLUE}🌐 EIP 강제 해제 및 삭제...${NC}"

# 태그로 찾은 EIP들
EIP_ALLOCS=$(aws ec2 describe-addresses --region $REGION --query "Addresses[?Tags[?Key=='Name' && contains(Value, 'interv')]].AllocationId" --output text 2>/dev/null || echo "")
for eip_alloc in $EIP_ALLOCS; do
    if [ -n "$eip_alloc" ]; then
        # 연결 해제
        ASSOCIATION_ID=$(aws ec2 describe-addresses --allocation-ids $eip_alloc --region $REGION --query 'Addresses[0].AssociationId' --output text 2>/dev/null || echo "None")
        if [ "$ASSOCIATION_ID" != "None" ] && [ -n "$ASSOCIATION_ID" ]; then
            echo -e "${YELLOW}  🔗 EIP 연결 해제: $eip_alloc${NC}"
            aws ec2 disassociate-address --association-id $ASSOCIATION_ID --region $REGION 2>/dev/null || true
        fi
        
        force_delete "Tagged EIP" "$eip_alloc" \
            "aws ec2 release-address --allocation-id $eip_alloc --region $REGION"
    fi
done

# 연결되지 않은 EIP들 (InterV 관련일 가능성)
echo -e "${BLUE}🔍 연결되지 않은 EIP 확인...${NC}"
UNATTACHED_EIPS=$(aws ec2 describe-addresses --region $REGION --query 'Addresses[?AssociationId==null].AllocationId' --output text 2>/dev/null || echo "")
if [ -n "$UNATTACHED_EIPS" ]; then
    echo -e "${YELLOW}⚠️ 연결되지 않은 EIP들을 발견했습니다:${NC}"
    aws ec2 describe-addresses --allocation-ids $UNATTACHED_EIPS --region $REGION --query 'Addresses[*].[AllocationId,PublicIp]' --output table 2>/dev/null || true
    
    echo -e "${YELLOW}이 EIP들을 모두 삭제하시겠습니까? (y/n)${NC}"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        for eip_alloc in $UNATTACHED_EIPS; do
            force_delete "Unattached EIP" "$eip_alloc" \
                "aws ec2 release-address --allocation-id $eip_alloc --region $REGION"
        done
    fi
fi

echo ""
echo -e "${RED}🚨 4단계: Security Group 강제 삭제${NC}"
echo "==================================="

# 4-1. InterV Security Group 찾기 및 삭제
echo -e "${BLUE}🛡️ Security Group 강제 삭제...${NC}"
SG_IDS=$(aws ec2 describe-security-groups --region $REGION --query "SecurityGroups[?contains(GroupName, 'interv') && GroupName!='default'].GroupId" --output text 2>/dev/null || echo "")

# Security Group 삭제 (여러 번 시도 - 의존성 때문에)
for attempt in 1 2 3; do
    echo -e "${BLUE}Security Group 삭제 시도 $attempt/3...${NC}"
    
    remaining_sgs=""
    for sg_id in $SG_IDS; do
        if [ -n "$sg_id" ]; then
            # Security Group이 아직 존재하는지 확인
            if aws ec2 describe-security-groups --group-ids $sg_id --region $REGION >/dev/null 2>&1; then
                if force_delete "Security Group" "$sg_id" \
                    "aws ec2 delete-security-group --group-id $sg_id --region $REGION"; then
                    echo -e "${GREEN}  ✅ SG 삭제 성공: $sg_id${NC}"
                else
                    remaining_sgs="$remaining_sgs $sg_id"
                fi
            fi
        fi
    done
    
    SG_IDS="$remaining_sgs"
    if [ -z "$SG_IDS" ]; then
        echo -e "${GREEN}✅ 모든 Security Group 삭제 완료${NC}"
        break
    fi
    
    if [ $attempt -lt 3 ]; then
        echo -e "${YELLOW}⏳ 의존성 해제를 위해 30초 대기...${NC}"
        sleep 30
    fi
done

echo ""
echo -e "${RED}🚨 5단계: VPC 및 네트워크 인프라 정리${NC}"
echo "===================================="

# 5-1. VPC 찾기
VPC_ID=""
# 태그로 찾기
VPC_ID=$(aws ec2 describe-vpcs --region $REGION --filters "Name=tag:Name,Values=${APP_NAME}-vpc" --query 'Vpcs[0].VpcId' --output text 2>/dev/null || echo "None")
if [ "$VPC_ID" = "None" ]; then
    # CIDR로 찾기 (기본 VPC가 아닌 것)
    VPC_ID=$(aws ec2 describe-vpcs --region $REGION --filters "Name=cidr-block,Values=10.0.0.0/16" "Name=is-default,Values=false" --query 'Vpcs[0].VpcId' --output text 2>/dev/null || echo "None")
fi

if [ "$VPC_ID" != "None" ] && [ -n "$VPC_ID" ]; then
    echo -e "${BLUE}🌐 InterV VPC 발견: $VPC_ID${NC}"
    
    # 5-2. NAT Gateway 삭제
    echo -e "${BLUE}🌉 NAT Gateway 삭제...${NC}"
    NAT_GATEWAYS=$(aws ec2 describe-nat-gateways --filter "Name=vpc-id,Values=$VPC_ID" "Name=state,Values=available" --region $REGION --query 'NatGateways[].NatGatewayId' --output text 2>/dev/null || echo "")
    for nat_gw in $NAT_GATEWAYS; do
        if [ -n "$nat_gw" ]; then
            force_delete "NAT Gateway" "$nat_gw" \
                "aws ec2 delete-nat-gateway --nat-gateway-id $nat_gw --region $REGION"
        fi
    done
    
    # 5-3. Route Table 정리
    echo -e "${BLUE}🛣️ Route Table 정리...${NC}"
    ROUTE_TABLES=$(aws ec2 describe-route-tables --filters "Name=vpc-id,Values=$VPC_ID" --region $REGION --query 'RouteTables[?Associations[0].Main!=`true`].RouteTableId' --output text 2>/dev/null || echo "")
    for rt in $ROUTE_TABLES; do
        if [ -n "$rt" ]; then
            # 연결 해제
            ASSOCIATIONS=$(aws ec2 describe-route-tables --route-table-ids "$rt" --region $REGION --query 'RouteTables[0].Associations[?Main!=`true`].RouteTableAssociationId' --output text 2>/dev/null || echo "")
            for assoc in $ASSOCIATIONS; do
                if [ -n "$assoc" ]; then
                    aws ec2 disassociate-route-table --association-id "$assoc" --region $REGION 2>/dev/null || true
                fi
            done
            
            force_delete "Route Table" "$rt" \
                "aws ec2 delete-route-table --route-table-id $rt --region $REGION"
        fi
    done
    
    # 5-4. Subnet 삭제
    echo -e "${BLUE}🏗️ Subnet 삭제...${NC}"
    SUBNETS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --region $REGION --query 'Subnets[].SubnetId' --output text 2>/dev/null || echo "")
    for subnet in $SUBNETS; do
        if [ -n "$subnet" ]; then
            force_delete "Subnet" "$subnet" \
                "aws ec2 delete-subnet --subnet-id $subnet --region $REGION"
        fi
    done
    
    # 5-5. Internet Gateway 분리 및 삭제
    echo -e "${BLUE}🌐 Internet Gateway 삭제...${NC}"
    IGW=$(aws ec2 describe-internet-gateways --filters "Name=attachment.vpc-id,Values=$VPC_ID" --region $REGION --query 'InternetGateways[0].InternetGatewayId' --output text 2>/dev/null || echo "None")
    if [ "$IGW" != "None" ] && [ -n "$IGW" ]; then
        aws ec2 detach-internet-gateway --internet-gateway-id "$IGW" --vpc-id "$VPC_ID" --region $REGION 2>/dev/null || true
        force_delete "Internet Gateway" "$IGW" \
            "aws ec2 delete-internet-gateway --internet-gateway-id $IGW --region $REGION"
    fi
    
    # 5-6. VPC 삭제
    echo -e "${BLUE}🌐 VPC 삭제...${NC}"
    sleep 10  # 잠시 대기
    force_delete "VPC" "$VPC_ID" \
        "aws ec2 delete-vpc --vpc-id $VPC_ID --region $REGION"
fi

echo ""
echo -e "${RED}🚨 6단계: 기타 남은 리소스 정리${NC}"
echo "==============================="

# 6-1. RDS 삭제
echo -e "${BLUE}🗄️ RDS 삭제...${NC}"
RDS_EXISTS=$(aws rds describe-db-instances --db-instance-identifier "${APP_NAME}-db" --region $REGION --query 'length(DBInstances)' --output text 2>/dev/null || echo "0")
if [ "$RDS_EXISTS" != "0" ]; then
    force_delete "RDS Instance" "${APP_NAME}-db" \
        "aws rds delete-db-instance --db-instance-identifier ${APP_NAME}-db --skip-final-snapshot --region $REGION"
fi

# 6-2. DB Subnet Group 삭제
force_delete "DB Subnet Group" "${APP_NAME}-db-subnet-group" \
    "aws rds delete-db-subnet-group --db-subnet-group-name ${APP_NAME}-db-subnet-group --region $REGION"

# 6-3. IAM 리소스 정리
echo -e "${BLUE}👤 IAM 리소스 정리...${NC}"
aws iam remove-role-from-instance-profile --instance-profile-name "${APP_NAME}-ec2-profile" --role-name "${APP_NAME}-ec2-role" 2>/dev/null || true
force_delete "IAM Instance Profile" "${APP_NAME}-ec2-profile" \
    "aws iam delete-instance-profile --instance-profile-name ${APP_NAME}-ec2-profile"
force_delete "IAM Role Policy" "${APP_NAME}-ec2-policy" \
    "aws iam delete-role-policy --role-name ${APP_NAME}-ec2-role --policy-name ${APP_NAME}-ec2-policy"  
force_delete "IAM Role" "${APP_NAME}-ec2-role" \
    "aws iam delete-role --role-name ${APP_NAME}-ec2-role"

echo ""
echo -e "${GREEN}🎉 InterV 남은 리소스 강제 정리 완료!${NC}"
echo "============================================="
echo ""
echo -e "${BLUE}📋 다음 명령어로 정리 확인:${NC}"
echo "./check_resources.sh"
echo ""
echo -e "${GREEN}✅ 이제 깨끗한 상태에서 새로 배포할 수 있습니다!${NC}"