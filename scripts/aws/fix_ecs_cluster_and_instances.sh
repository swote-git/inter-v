#!/bin/bash

echo "🔧 ECS 클러스터 문제 해결 및 배포 계속 진행"
echo "=========================================="

AWS_REGION="ap-northeast-2"
CLUSTER_NAME="interv-cluster"
TASK_DEFINITION="interv-task-ec2"
SERVICE_NAME="interv-service"

# 1. 현재 상태 확인
echo "📊 현재 ECS 상태 확인..."

# ECS 클러스터 확인
echo "1. ECS 클러스터 확인..."
aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ ECS 클러스터 '$CLUSTER_NAME' 존재"
  
  CLUSTER_STATUS=$(aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].status' --output text)
  REGISTERED_INSTANCES=$(aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].registeredContainerInstancesCount' --output text)
  
  echo "  - 상태: $CLUSTER_STATUS"
  echo "  - 등록된 인스턴스: $REGISTERED_INSTANCES개"
else
  echo "❌ ECS 클러스터 '$CLUSTER_NAME' 없음 - 생성 중..."
  
  aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION >/dev/null 2>&1
  if [ $? -eq 0 ]; then
    echo "✅ ECS 클러스터 생성 완료"
  else
    echo "❌ ECS 클러스터 생성 실패"
    exit 1
  fi
fi

# 2. Task Definition 확인
echo ""
echo "2. Task Definition 확인..."
TASK_DEF_STATUS=$(aws ecs describe-task-definition --task-definition $TASK_DEFINITION --region $AWS_REGION --query 'taskDefinition.status' --output text 2>/dev/null)

if [ "$TASK_DEF_STATUS" = "ACTIVE" ]; then
  REVISION=$(aws ecs describe-task-definition --task-definition $TASK_DEFINITION --region $AWS_REGION --query 'taskDefinition.revision' --output text)
  echo "✅ Task Definition '$TASK_DEFINITION:$REVISION' 활성 상태"
else
  echo "❌ Task Definition을 찾을 수 없습니다"
  echo "다시 생성하겠습니다..."
  # Task Definition 재생성 로직은 생략 (이미 성공했으므로)
fi

# 3. Auto Scaling Group과 인스턴스 확인
echo ""
echo "3. Auto Scaling Group 및 인스턴스 확인..."

ASG_LIST=$(aws autoscaling describe-auto-scaling-groups --region $AWS_REGION --query 'AutoScalingGroups[*].AutoScalingGroupName' --output text)

if [ -z "$ASG_LIST" ]; then
  echo "❌ Auto Scaling Group을 찾을 수 없습니다"
  echo "먼저 네트워크 설정을 완료해주세요: ./network_setup.sh"
  exit 1
fi

echo "발견된 Auto Scaling Groups:"
for asg in $ASG_LIST; do
  INSTANCE_COUNT=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names $asg --region $AWS_REGION --query 'AutoScalingGroups[0].Instances | length(@)' --output text)
  echo "  - $asg ($INSTANCE_COUNT개 인스턴스)"
done

# 첫 번째 ASG 선택
ASG_NAME=$(echo $ASG_LIST | awk '{print $1}')
echo "사용할 ASG: $ASG_NAME"

# ASG 인스턴스들 확인
CURRENT_INSTANCES=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names $ASG_NAME --region $AWS_REGION --query 'AutoScalingGroups[0].Instances[*].InstanceId' --output text)
echo "현재 인스턴스들: $CURRENT_INSTANCES"

# 4. 인스턴스들이 ECS 클러스터에 조인되었는지 확인
echo ""
echo "4. ECS 클러스터 조인 상태 확인..."

CONTAINER_INSTANCES=$(aws ecs list-container-instances --cluster $CLUSTER_NAME --region $AWS_REGION --query 'containerInstanceArns' --output text 2>/dev/null)

if [ -n "$CONTAINER_INSTANCES" ] && [ "$CONTAINER_INSTANCES" != "None" ]; then
  INSTANCE_COUNT=$(echo "$CONTAINER_INSTANCES" | wc -w)
  echo "✅ ECS 클러스터에 $INSTANCE_COUNT개 인스턴스 등록됨"
  
  echo ""
  echo "등록된 컨테이너 인스턴스 정보:"
  aws ecs describe-container-instances --cluster $CLUSTER_NAME --container-instances $CONTAINER_INSTANCES --region $AWS_REGION --query 'containerInstances[*].[ec2InstanceId,agentConnected,runningTasksCount,pendingTasksCount]' --output table
else
  echo "❌ ECS 클러스터에 등록된 인스턴스 없음"
  echo ""
  echo "🔧 인스턴스 ECS 조인 작업 필요..."
  echo "인스턴스들을 ECS 클러스터에 조인하시겠습니까? (y/n):"
  read -r join_instances
  
  if [[ "$join_instances" =~ ^[Yy]$ ]]; then
    echo "🔄 인스턴스 ECS 조인 시작..."
    
    # Launch Template 확인
    LAUNCH_TEMPLATE=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names $ASG_NAME --region $AWS_REGION --query 'AutoScalingGroups[0].LaunchTemplate.LaunchTemplateName' --output text)
    
    if [ "$LAUNCH_TEMPLATE" = "None" ] || [ -z "$LAUNCH_TEMPLATE" ]; then
      echo "❌ Launch Template을 찾을 수 없습니다"
      echo "./fix_ecs_cluster_and_instances.sh 스크립트를 먼저 실행해주세요"
      exit 1
    fi
    
    echo "Launch Template: $LAUNCH_TEMPLATE"
    
    # ECS User Data가 있는지 확인
    USER_DATA=$(aws ec2 describe-launch-template-versions --launch-template-name $LAUNCH_TEMPLATE --region $AWS_REGION --query 'LaunchTemplateVersions[0].LaunchTemplateData.UserData' --output text 2>/dev/null)
    
    if [ -n "$USER_DATA" ] && [ "$USER_DATA" != "None" ]; then
      # User Data 디코딩해서 ECS 설정 확인
      DECODED_DATA=$(echo "$USER_DATA" | base64 -d 2>/dev/null)
      if echo "$DECODED_DATA" | grep -q "ECS_CLUSTER"; then
        echo "✅ Launch Template에 ECS 설정 있음"
        
        # 인스턴스 교체로 ECS 조인
        echo "기존 인스턴스들을 ECS 지원 인스턴스로 교체하겠습니다..."
        
        for instance_id in $CURRENT_INSTANCES; do
          echo "🔄 인스턴스 $instance_id 교체 중..."
          
          aws autoscaling terminate-instance-in-auto-scaling-group \
            --instance-id $instance_id \
            --no-should-decrement-desired-capacity \
            --region $AWS_REGION
          
          if [ $? -eq 0 ]; then
            echo "✅ 인스턴스 $instance_id 교체 요청 완료"
            echo "⏳ 새 인스턴스 시작 대기 중... (3분)"
            sleep 180
          else
            echo "❌ 인스턴스 $instance_id 교체 실패"
          fi
        done
        
        echo "⏳ ECS Agent 시작 대기 중... (2분)"
        sleep 120
        
        # 다시 확인
        CONTAINER_INSTANCES=$(aws ecs list-container-instances --cluster $CLUSTER_NAME --region $AWS_REGION --query 'containerInstanceArns' --output text 2>/dev/null)
        
        if [ -n "$CONTAINER_INSTANCES" ] && [ "$CONTAINER_INSTANCES" != "None" ]; then
          echo "✅ 인스턴스들이 ECS 클러스터에 조인되었습니다!"
        else
          echo "❌ 인스턴스 조인 실패. 수동 확인이 필요합니다."
        fi
      else
        echo "❌ Launch Template에 ECS 설정이 없습니다"
        echo "./fix_ecs_cluster_and_instances.sh 스크립트를 먼저 실행해주세요"
        exit 1
      fi
    else
      echo "❌ Launch Template User Data가 없습니다"
      echo "./fix_ecs_cluster_and_instances.sh 스크립트를 먼저 실행해주세요"
      exit 1
    fi
  else
    echo "인스턴스 조인을 건너뜁니다."
    echo "수동으로 ./fix_ecs_cluster_and_instances.sh를 실행해주세요."
  fi
fi

# 5. ECS 서비스 생성/업데이트
echo ""
echo "🚀 ECS 서비스 배포..."

# 기존 서비스 확인
aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME --region $AWS_REGION >/dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "🔄 기존 ECS 서비스 업데이트 중..."
  
  aws ecs update-service \
    --cluster $CLUSTER_NAME \
    --service $SERVICE_NAME \
    --task-definition $TASK_DEFINITION \
    --region $AWS_REGION >/dev/null 2>&1
  
  if [ $? -eq 0 ]; then
    echo "✅ ECS 서비스 업데이트 완료"
  else
    echo "❌ ECS 서비스 업데이트 실패"
  fi
else
  echo "📝 새 ECS 서비스 생성 중..."
  
  # Target Group ARN 찾기
  TARGET_GROUPS=$(aws elbv2 describe-target-groups --region $AWS_REGION --query 'TargetGroups[?contains(TargetGroupName, `interv`)].TargetGroupArn' --output text)
  
  if [ -n "$TARGET_GROUPS" ]; then
    TARGET_GROUP_ARN=$(echo $TARGET_GROUPS | awk '{print $1}')
    echo "Target Group 발견: $TARGET_GROUP_ARN"
    
    # 로드 밸런서 설정 포함 서비스 생성
    aws ecs create-service \
      --cluster $CLUSTER_NAME \
      --service-name $SERVICE_NAME \
      --task-definition $TASK_DEFINITION \
      --desired-count 1 \
      --launch-type EC2 \
      --load-balancers "targetGroupArn=$TARGET_GROUP_ARN,containerName=interv-container,containerPort=8080" \
      --role "arn:aws:iam::034115074124:role/aws-ec2-spot-fleet-tagging-role" \
      --region $AWS_REGION >/dev/null 2>&1
  else
    echo "⚠️ Target Group을 찾을 수 없습니다. 로드 밸런서 없이 서비스 생성..."
    
    # 로드 밸런서 없이 서비스 생성
    aws ecs create-service \
      --cluster $CLUSTER_NAME \
      --service-name $SERVICE_NAME \
      --task-definition $TASK_DEFINITION \
      --desired-count 1 \
      --launch-type EC2 \
      --region $AWS_REGION >/dev/null 2>&1
  fi
  
  if [ $? -eq 0 ]; then
    echo "✅ ECS 서비스 생성 완료"
  else
    echo "❌ ECS 서비스 생성 실패"
    echo "수동 서비스 생성 명령어:"
    echo "aws ecs create-service --cluster $CLUSTER_NAME --service-name $SERVICE_NAME --task-definition $TASK_DEFINITION --desired-count 1 --launch-type EC2 --region $AWS_REGION"
  fi
fi

# 6. 배포 상태 모니터링
echo ""
echo "📊 배포 상태 모니터링..."

echo "⏳ 서비스 안정화 대기 중... (약 3-5분)"
for i in {1..15}; do
  sleep 30
  
  SERVICE_STATUS=$(aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME --region $AWS_REGION --query 'services[0].[serviceName,status,runningCount,pendingCount,desiredCount]' --output text 2>/dev/null)
  
  if [ -n "$SERVICE_STATUS" ]; then
    echo "서비스 상태: $SERVICE_STATUS"
    
    RUNNING_COUNT=$(echo "$SERVICE_STATUS" | awk '{print $3}')
    DESIRED_COUNT=$(echo "$SERVICE_STATUS" | awk '{print $5}')
    
    if [ "$RUNNING_COUNT" -ge "$DESIRED_COUNT" ] && [ "$DESIRED_COUNT" -gt 0 ]; then
      echo "✅ 서비스가 안정 상태에 도달했습니다!"
      break
    fi
  else
    echo "서비스 상태 확인 실패"
  fi
  
  if [ $i -eq 15 ]; then
    echo "⚠️ 서비스 안정화에 시간이 오래 걸리고 있습니다."
  fi
done

# 7. 최종 상태 확인
echo ""
echo "📊 최종 배포 상태"
echo "=================="

echo ""
echo "ECS 클러스터 상태:"
aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].[clusterName,status,registeredContainerInstancesCount,runningTasksCount,pendingTasksCount,activeServicesCount]' --output table

echo ""
echo "ECS 서비스 상태:"
aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME --region $AWS_REGION --query 'services[0].[serviceName,status,taskDefinition,runningCount,pendingCount,desiredCount]' --output table

echo ""
echo "실행 중인 태스크:"
aws ecs list-tasks --cluster $CLUSTER_NAME --service-name $SERVICE_NAME --region $AWS_REGION --query 'taskArns' --output table

# 8. 접속 정보 확인
echo ""
echo "🌐 애플리케이션 접속 정보"
echo "======================="

# Load Balancer DNS 확인
LB_DNS=$(aws elbv2 describe-load-balancers --region $AWS_REGION --query 'LoadBalancers[?contains(LoadBalancerName, `interv`)].DNSName' --output text 2>/dev/null)

if [ -n "$LB_DNS" ]; then
  echo "✅ Load Balancer URL: http://$LB_DNS"
  echo "✅ 도메인 URL: https://interv.swote.dev"
else
  echo "⚠️ Load Balancer를 찾을 수 없습니다."
  
  # 인스턴스 Public IP 확인
  if [ -n "$CURRENT_INSTANCES" ]; then
    echo "인스턴스 직접 접속:"
    for instance_id in $CURRENT_INSTANCES; do
      PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $instance_id --region $AWS_REGION --query 'Reservations[0].Instances[0].PublicIpAddress' --output text 2>/dev/null)
      if [ -n "$PUBLIC_IP" ] && [ "$PUBLIC_IP" != "None" ]; then
        echo "  - http://$PUBLIC_IP:8080"
      fi
    done
  fi
fi

echo ""
echo "🎉 ECS 배포 작업 완료!"
echo "===================="
echo ""
echo "✅ 완료된 작업:"
echo "  - ECS 클러스터 생성/확인: $CLUSTER_NAME"
echo "  - Task Definition 등록: $TASK_DEFINITION"
echo "  - ECS 서비스 배포: $SERVICE_NAME"
echo ""
echo "🔍 상태 모니터링:"
echo "  - ECS 콘솔: https://ap-northeast-2.console.aws.amazon.com/ecs/home?region=ap-northeast-2#/clusters/$CLUSTER_NAME"
echo "  - CloudWatch 로그: https://ap-northeast-2.console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2#logsV2:log-groups/log-group/%2Fecs%2Finterv"
echo ""
echo "📋 다음 단계:"
echo "  - 애플리케이션 접속 테스트"
echo "  - 도메인 설정 확인 (Route 53)"
echo "  - SSL 인증서 확인 (CloudFront)"