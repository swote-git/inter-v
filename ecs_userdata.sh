#!/bin/bash

# 로그 파일 설정
exec > >(tee /var/log/user-data.log) 2>&1
echo "UserData 스크립트 시작: $(date)"

# ECS 클러스터 설정
echo "ECS_CLUSTER=interv-cluster" >> /etc/ecs/ecs.config
echo "ECS_ENABLE_CONTAINER_METADATA=true" >> /etc/ecs/ecs.config
echo "ECS_ENABLE_TASK_IAM_ROLE=true" >> /etc/ecs/ecs.config
echo "ECS_ENABLE_TASK_IAM_ROLE_NETWORK_HOST=true" >> /etc/ecs/ecs.config

# ECS Agent 재시작
echo "ECS Agent 재시작 중..."
systemctl restart ecs

# ECS Agent 상태 확인
echo "ECS Agent 상태:"
systemctl status ecs

echo "UserData 스크립트 완료: $(date)"
