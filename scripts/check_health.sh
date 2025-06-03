#!/bin/bash
# scripts/diagnose_health.sh - Health Check 문제 진단 및 해결

set -e

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${RED}🔍 InterV Health Check 문제 진단${NC}"
echo "=================================="

# 1. 현재 타겟 상태 확인
echo -e "${BLUE}🎯 Target Group 상태 확인...${NC}"
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names interv-tg --query 'TargetGroups[0].TargetGroupArn' --output text)

if [ "$TARGET_GROUP_ARN" = "None" ]; then
    echo -e "${RED}❌ Target Group을 찾을 수 없습니다${NC}"
    exit 1
fi

aws elbv2 describe-target-health --target-group-arn "$TARGET_GROUP_ARN" \
    --query 'TargetHealthDescriptions[].{InstanceId:Target.Id,Port:Target.Port,State:TargetHealth.State,Reason:TargetHealth.Reason}' \
    --output table

# 2. Healthy가 아닌 인스턴스 찾기
UNHEALTHY_INSTANCES=$(aws elbv2 describe-target-health --target-group-arn "$TARGET_GROUP_ARN" \
    --query 'TargetHealthDescriptions[?TargetHealth.State!=`healthy`].Target.Id' --output text)

if [ -z "$UNHEALTHY_INSTANCES" ]; then
    echo -e "${GREEN}✅ 모든 인스턴스가 healthy 상태입니다!${NC}"
    exit 0
fi

echo -e "${YELLOW}⚠️ Unhealthy 인스턴스들: $UNHEALTHY_INSTANCES${NC}"

# 3. 각 인스턴스의 퍼블릭 IP 및 상태 확인
echo -e "${BLUE}📋 인스턴스 상세 정보:${NC}"
for INSTANCE_ID in $UNHEALTHY_INSTANCES; do
    echo ""
    echo -e "${BLUE}=== 인스턴스: $INSTANCE_ID ===${NC}"
    
    # 인스턴스 기본 정보
    INSTANCE_INFO=$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
        --query 'Reservations[0].Instances[0].{PublicIp:PublicIpAddress,PrivateIp:PrivateIpAddress,State:State.Name,LaunchTime:LaunchTime}' \
        --output json)
    
    PUBLIC_IP=$(echo "$INSTANCE_INFO" | jq -r '.PublicIp // "None"')
    PRIVATE_IP=$(echo "$INSTANCE_INFO" | jq -r '.PrivateIp // "None"')
    STATE=$(echo "$INSTANCE_INFO" | jq -r '.State')
    LAUNCH_TIME=$(echo "$INSTANCE_INFO" | jq -r '.LaunchTime')
    
    echo "  ├─ 퍼블릭 IP: $PUBLIC_IP"
    echo "  ├─ 프라이빗 IP: $PRIVATE_IP"
    echo "  ├─ 상태: $STATE"
    echo "  └─ 시작 시간: $LAUNCH_TIME"
    
    # SSH 접속 가능한 경우 진단
    if [ "$PUBLIC_IP" != "None" ] && [ "$STATE" = "running" ]; then
        echo ""
        echo -e "${BLUE}🔧 $INSTANCE_ID 진단 명령어:${NC}"
        echo "ssh -i ~/.ssh/interv-keypair.pem ec2-user@$PUBLIC_IP"
        echo ""
        echo -e "${YELLOW}진단할 명령어들:${NC}"
        echo "# 1. 애플리케이션 서비스 상태"
        echo "sudo systemctl status interv"
        echo ""
        echo "# 2. 애플리케이션 로그 확인"
        echo "sudo journalctl -u interv -f --no-pager"
        echo ""
        echo "# 3. 포트 8080 리스닝 확인"
        echo "sudo netstat -tlnp | grep 8080"
        echo ""
        echo "# 4. Health 엔드포인트 직접 테스트"
        echo "curl -v http://localhost:8080/actuator/health"
        echo ""
        echo "# 5. 최근 시스템 로그"
        echo "sudo tail -100 /var/log/messages"
        echo ""
        echo "# 6. JAR 파일 확인"
        echo "ls -la /opt/interv/"
        echo ""
        echo "# 7. 애플리케이션 프로세스 확인"
        echo "ps aux | grep java"
    fi
done

# 4. Health Check 설정 확인
echo ""
echo -e "${BLUE}🏥 Target Group Health Check 설정:${NC}"
aws elbv2 describe-target-groups --target-group-arns "$TARGET_GROUP_ARN" \
    --query 'TargetGroups[0].{HealthCheckPath:HealthCheckPath,HealthCheckPort:HealthCheckPort,HealthCheckProtocol:HealthCheckProtocol,HealthyThresholdCount:HealthyThresholdCount,UnhealthyThresholdCount:UnhealthyThresholdCount,HealthCheckTimeoutSeconds:HealthCheckTimeoutSeconds,HealthCheckIntervalSeconds:HealthCheckIntervalSeconds}' \
    --output table

# 5. 자동 수정 제안
echo ""
echo -e "${GREEN}🔧 자동 수정 방법:${NC}"
echo ""

# 가장 최근 인스턴스 선택 (가장 가능성이 높음)
LATEST_INSTANCE=$(aws ec2 describe-instances --instance-ids $UNHEALTHY_INSTANCES \
    --query 'Reservations[].Instances[] | sort_by(@, &LaunchTime) | [-1].InstanceId' --output text)

if [ -n "$LATEST_INSTANCE" ] && [ "$LATEST_INSTANCE" != "None" ]; then
    LATEST_PUBLIC_IP=$(aws ec2 describe-instances --instance-ids "$LATEST_INSTANCE" \
        --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
    
    echo -e "${BLUE}가장 최근 인스턴스 ($LATEST_INSTANCE)에 자동 접속하여 진단하시겠습니까? (y/n)${NC}"
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]] && [ "$LATEST_PUBLIC_IP" != "None" ]; then
        echo -e "${BLUE}🔍 자동 진단 시작...${NC}"
        
        # SSH 키 파일 확인
        SSH_KEY_PATHS=(
            "~/.ssh/interv-keypair.pem"
            "~/.ssh/interv-keypair"
            "~/interv-keypair.pem"
            "./interv-keypair.pem"
        )
        
        SSH_KEY=""
        for key_path in "${SSH_KEY_PATHS[@]}"; do
            expanded_path=$(eval echo "$key_path")
            if [ -f "$expanded_path" ]; then
                SSH_KEY="$expanded_path"
                chmod 400 "$SSH_KEY"
                break
            fi
        done
        
        if [ -z "$SSH_KEY" ]; then
            echo -e "${RED}❌ SSH 키 파일을 찾을 수 없습니다${NC}"
            echo -e "${YELLOW}다음 위치에 키 파일이 있는지 확인해주세요:${NC}"
            for path in "${SSH_KEY_PATHS[@]}"; do
                echo "  • $path"
            done
        else
            echo -e "${GREEN}✅ SSH 키 발견: $SSH_KEY${NC}"
            echo -e "${BLUE}🔗 $LATEST_INSTANCE ($LATEST_PUBLIC_IP)에 접속 중...${NC}"
            
            # SSH 접속 테스트
            if ssh -i "$SSH_KEY" -o ConnectTimeout=10 -o StrictHostKeyChecking=no ec2-user@"$LATEST_PUBLIC_IP" "echo 'SSH 연결 성공'" 2>/dev/null; then
                echo -e "${GREEN}✅ SSH 연결 성공${NC}"
                
                # 원격 진단 스크립트 실행
                echo -e "${BLUE}📊 원격 진단 시작...${NC}"
                
                ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$LATEST_PUBLIC_IP" << 'EOF'
#!/bin/bash
echo "=== 서비스 상태 ==="
sudo systemctl status interv --no-pager || echo "서비스 상태 확인 실패"

echo ""
echo "=== 포트 8080 확인 ==="
sudo netstat -tlnp | grep 8080 || echo "포트 8080이 열려있지 않음"

echo ""
echo "=== 프로세스 확인 ==="
ps aux | grep java | grep -v grep || echo "Java 프로세스가 실행되지 않음"

echo ""
echo "=== JAR 파일 확인 ==="
ls -la /opt/interv/ || echo "애플리케이션 디렉토리 없음"

echo ""
echo "=== Health Check 테스트 ==="
curl -s http://localhost:8080/actuator/health || echo "Health 엔드포인트 접근 실패"

echo ""
echo "=== 최근 애플리케이션 로그 (마지막 20줄) ==="
sudo journalctl -u interv -n 20 --no-pager || echo "로그 확인 실패"
EOF
                
                echo ""
                echo -e "${BLUE}🔧 서비스 재시작을 시도하시겠습니까? (y/n)${NC}"
                read -r restart_response
                
                if [[ "$restart_response" =~ ^[Yy]$ ]]; then
                    echo -e "${BLUE}🔄 서비스 재시작 중...${NC}"
                    
                    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$LATEST_PUBLIC_IP" << 'EOF'
#!/bin/bash
echo "=== 서비스 중지 ==="
sudo systemctl stop interv

echo "=== 잠시 대기 ==="
sleep 5

echo "=== 서비스 시작 ==="
sudo systemctl start interv

echo "=== 서비스 상태 확인 ==="
sudo systemctl status interv --no-pager

echo "=== 30초 대기 후 Health Check ==="
sleep 30
curl -s http://localhost:8080/actuator/health || echo "아직 Health Check 실패"
EOF
                    
                    echo -e "${GREEN}✅ 서비스 재시작 완료${NC}"
                    echo -e "${BLUE}💡 몇 분 후에 Target Group 상태를 다시 확인해주세요${NC}"
                fi
                
            else
                echo -e "${RED}❌ SSH 연결 실패${NC}"
                echo -e "${YELLOW}수동으로 연결해주세요: ssh -i $SSH_KEY ec2-user@$LATEST_PUBLIC_IP${NC}"
            fi
        fi
    fi
fi

# 6. 추가 해결 방법
echo ""
echo -e "${GREEN}📋 추가 해결 방법:${NC}"
echo ""
echo -e "${BLUE}1. 새 인스턴스로 교체:${NC}"
for INSTANCE_ID in $UNHEALTHY_INSTANCES; do
    echo "aws autoscaling set-instance-health --instance-id $INSTANCE_ID --health-status Unhealthy"
done
echo ""
echo -e "${BLUE}2. Auto Scaling Group 재시작:${NC}"
echo "aws autoscaling start-instance-refresh --auto-scaling-group-name interv-asg --preferences MinHealthyPercentage=0"
echo ""
echo -e "${BLUE}3. Launch Template 확인:${NC}"
echo "aws ec2 describe-launch-templates --launch-template-names interv-lt"
echo ""
echo -e "${BLUE}4. Target Group Health Check 확인:${NC}"
echo "현재 Health Check 경로: /actuator/health"
echo "포트: 8080"
echo "애플리케이션이 이 경로에서 응답하는지 확인 필요"

echo ""
echo -e "${YELLOW}💡 가장 흔한 문제들:${NC}"
echo "• 애플리케이션이 시작되지 않음 (JAR 파일 문제)"
echo "• 포트 8080이 열려있지 않음"
echo "• Health Check 엔드포인트가 비활성화됨"
echo "• 데이터베이스 연결 문제"
echo "• 메모리 부족"
echo ""
echo -e "${GREEN}🎯 다음 단계: 위의 진단 결과를 바탕으로 문제를 해결해주세요${NC}"