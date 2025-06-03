#!/bin/bash

echo "🔧 ECS 클러스터 문제 해결 스크립트"
echo "=================================="

AWS_REGION="ap-northeast-2"
CLUSTER_NAME="interv-cluster"
ASG_NAME="interv-asg"

# 1. ECS 클러스터 생성
echo "🐳 ECS 클러스터 생성..."
aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION

if [ $? -eq 0 ]; then
    echo "✅ ECS 클러스터 생성 완료"
else
    echo "❌ ECS 클러스터 생성 실패"
    exit 1
fi

# 2. 클러스터 생성 확인
echo ""
echo "🔍 클러스터 생성 확인..."
sleep 5
CLUSTER_STATUS=$(aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].status' --output text)
echo "클러스터 상태: $CLUSTER_STATUS"

# 3. Launch Template 정보 가져오기
echo ""
echo "📋 Launch Template 정보 조회..."
LAUNCH_TEMPLATE_INFO=$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names $ASG_NAME \
    --region $AWS_REGION \
    --query 'AutoScalingGroups[0].LaunchTemplate')

LAUNCH_TEMPLATE_NAME=$(echo $LAUNCH_TEMPLATE_INFO | jq -r '.LaunchTemplateName')
LAUNCH_TEMPLATE_VERSION=$(echo $LAUNCH_TEMPLATE_INFO | jq -r '.Version')

echo "Launch Template: $LAUNCH_TEMPLATE_NAME (Version: $LAUNCH_TEMPLATE_VERSION)"

# 4. 현재 Launch Template의 UserData 확인
echo ""
echo "🔍 현재 UserData 확인..."
CURRENT_USERDATA=$(aws ec2 describe-launch-template-versions \
    --launch-template-name $LAUNCH_TEMPLATE_NAME \
    --region $AWS_REGION \
    --query 'LaunchTemplateVersions[0].LaunchTemplateData.UserData' \
    --output text)

if [ "$CURRENT_USERDATA" != "None" ] && [ -n "$CURRENT_USERDATA" ]; then
    echo "현재 UserData 존재함"
    echo "$CURRENT_USERDATA" | base64 -d > current_userdata.sh
    echo "현재 UserData 내용:"
    echo "===================="
    cat current_userdata.sh
    echo "===================="
else
    echo "현재 UserData 없음"
fi

# 5. ECS 클러스터 설정이 포함된 새 UserData 생성
echo ""
echo "📝 새 UserData 생성..."

cat > new_userdata.sh << 'EOF'
#!/bin/bash

# ECS 클러스터 설정
echo ECS_CLUSTER=interv-cluster >> /etc/ecs/ecs.config
echo ECS_ENABLE_CONTAINER_METADATA=true >> /etc/ecs/ecs.config
echo ECS_ENABLE_TASK_IAM_ROLE=true >> /etc/ecs/ecs.config
echo ECS_ENABLE_TASK_IAM_ROLE_NETWORK_HOST=true >> /etc/ecs/ecs.config

# ECS 에이전트 재시작
systemctl restart ecs

# 로그 확인용
echo "ECS Agent started at $(date)" >> /var/log/ecs-init.log

# 기존 UserData가 있다면 여기에 추가
EOF

# 기존 UserData 내용 추가 (ECS 설정 제외)
if [ -f current_userdata.sh ]; then
    echo "" >> new_userdata.sh
    echo "# 기존 UserData 내용" >> new_userdata.sh
    grep -v "ECS_CLUSTER\|ecs\|ECS" current_userdata.sh >> new_userdata.sh 2>/dev/null || true
fi

echo "새 UserData 내용:"
echo "=================="
cat new_userdata.sh
echo "=================="

# 6. UserData를 Base64로 인코딩
NEW_USERDATA_B64=$(base64 -w 0 new_userdata.sh)

# 7. 기존 Launch Template 데이터 가져오기
echo ""
echo "📋 Launch Template 데이터 가져오기..."
aws ec2 describe-launch-template-versions \
    --launch-template-name $LAUNCH_TEMPLATE_NAME \
    --region $AWS_REGION \
    --query 'LaunchTemplateVersions[0].LaunchTemplateData' > current_template.json

# 8. UserData 업데이트
echo ""
echo "🔄 UserData 업데이트..."
jq --arg userdata "$NEW_USERDATA_B64" '.UserData = $userdata' current_template.json > updated_template.json

# 9. 새 Launch Template 버전 생성
echo ""
echo "🆕 새 Launch Template 버전 생성..."
NEW_VERSION=$(aws ec2 create-launch-template-version \
    --launch-template-name $LAUNCH_TEMPLATE_NAME \
    --launch-template-data file://updated_template.json \
    --region $AWS_REGION \
    --query 'LaunchTemplateVersion.VersionNumber' \
    --output text)

if [ $? -eq 0 ]; then
    echo "✅ 새 Launch Template 버전 생성 완료: v$NEW_VERSION"
else
    echo "❌ Launch Template 버전 생성 실패"
    exit 1
fi

# 10. Auto Scaling Group 업데이트
echo ""
echo "🔄 Auto Scaling Group 업데이트..."
aws autoscaling update-auto-scaling-group \
    --auto-scaling-group-name $ASG_NAME \
    --launch-template "LaunchTemplateName=$LAUNCH_TEMPLATE_NAME,Version=$NEW_VERSION" \
    --region $AWS_REGION

if [ $? -eq 0 ]; then
    echo "✅ Auto Scaling Group 업데이트 완료"
else
    echo "❌ Auto Scaling Group 업데이트 실패"
    exit 1
fi

# 11. 기존 인스턴스들 종료 (새 설정으로 시작하도록)
echo ""
echo "🔄 기존 인스턴스들 종료 중... (새 설정으로 재시작)"

RUNNING_INSTANCES=$(aws ec2 describe-instances \
    --region $AWS_REGION \
    --filters "Name=instance-state-name,Values=running" "Name=tag:aws:autoscaling:groupName,Values=$ASG_NAME" \
    --query 'Reservations[*].Instances[*].InstanceId' \
    --output text)

if [ -n "$RUNNING_INSTANCES" ] && [ "$RUNNING_INSTANCES" != "None" ]; then
    echo "종료할 인스턴스들: $RUNNING_INSTANCES"
    
    for instance_id in $RUNNING_INSTANCES; do
        echo "인스턴스 종료: $instance_id"
        aws ec2 terminate-instances --instance-ids $instance_id --region $AWS_REGION
    done
    
    echo "⏳ 새 인스턴스 시작 대기 중... (2분)"
    sleep 120
else
    echo "실행 중인 인스턴스가 없습니다."
fi

# 12. 새 인스턴스들이 ECS 클러스터에 조인했는지 확인
echo ""
echo "🔍 ECS 클러스터 조인 상태 확인..."

for i in {1..10}; do
    echo "확인 시도 $i/10..."
    
    CONTAINER_INSTANCES=$(aws ecs list-container-instances \
        --cluster $CLUSTER_NAME \
        --region $AWS_REGION \
        --query 'containerInstanceArns' \
        --output text)
    
    if [ -n "$CONTAINER_INSTANCES" ] && [ "$CONTAINER_INSTANCES" != "None" ]; then
        echo "✅ Container Instance가 클러스터에 조인됨!"
        aws ecs describe-container-instances \
            --cluster $CLUSTER_NAME \
            --container-instances $CONTAINER_INSTANCES \
            --region $AWS_REGION \
            --query 'containerInstances[*].[ec2InstanceId,status,runningTasksCount]' \
            --output table
        break
    else
        echo "아직 조인되지 않음... 30초 후 재시도"
        sleep 30
    fi
    
    if [ $i -eq 10 ]; then
        echo "❌ Container Instance 조인 실패"
        echo ""
        echo "🔧 수동 확인 방법:"
        echo "1. EC2 인스턴스에 SSH 접속"
        echo "2. sudo cat /var/log/ecs/ecs-init.log 확인"
        echo "3. sudo cat /etc/ecs/ecs.config 확인"
        echo "4. sudo systemctl status ecs 확인"
    fi
done

# 13. 정리
rm -f current_userdata.sh new_userdata.sh current_template.json updated_template.json

echo ""
echo "🎉 ECS 클러스터 수정 완료!"
echo "========================="
echo ""
echo "📋 결과:"
echo "  - ECS 클러스터: $CLUSTER_NAME 생성됨"
echo "  - Launch Template: $LAUNCH_TEMPLATE_NAME v$NEW_VERSION로 업데이트"
echo "  - Auto Scaling Group: 새 설정으로 업데이트"
echo ""
echo "📊 확인 명령어:"
echo "  - 클러스터 상태: aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION"
echo "  - Container Instance: aws ecs list-container-instances --cluster $CLUSTER_NAME --region $AWS_REGION"
echo "  - 인스턴스 상태: aws ec2 describe-instances --region $AWS_REGION --filters \"Name=instance-state-name,Values=running\""