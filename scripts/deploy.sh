#!/bin/bash
# scripts/deploy.sh - 수동 배포 스크립트

set -e

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# 설정
APP_NAME="interv"
AWS_REGION="ap-northeast-2"

echo -e "${BLUE}🚀 Starting InterV deployment...${NC}"

# 1. 애플리케이션 빌드
echo -e "${BLUE}📦 Building application...${NC}"
if [ -d "inter-v" ]; then
    cd inter-v
elif [ -d "BE/inter-v" ]; then
    cd BE/inter-v
else
    echo -e "${RED}❌ Application directory not found!${NC}"
    exit 1
fi

mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed!${NC}"
    exit 1
fi

cd - > /dev/null

# 2. Terraform 배포 (인프라)
echo -e "${BLUE}🏗️  Deploying infrastructure...${NC}"
cd infrastructure/

# terraform.tfvars 파일 확인
if [ ! -f "terraform.tfvars" ]; then
    echo -e "${RED}❌ terraform.tfvars not found!${NC}"
    echo "Please copy terraform.tfvars.example to terraform.tfvars and update the values."
    exit 1
fi

terraform init
terraform plan -out=tfplan
terraform apply tfplan

# S3 버킷 이름 가져오기
S3_BUCKET=$(terraform output -raw s3_bucket_name)
echo -e "${GREEN}✅ S3 Bucket: $S3_BUCKET${NC}"

cd - > /dev/null

# 3. JAR 파일 S3 업로드
echo -e "${BLUE}📤 Uploading application to S3...${NC}"

# JAR 파일 찾기
if [ -d "inter-v" ]; then
    JAR_PATH="inter-v/target"
elif [ -d "BE/inter-v" ]; then
    JAR_PATH="BE/inter-v/target"
fi

JAR_FILE=$(find $JAR_PATH -name "*.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}❌ JAR file not found!${NC}"
    exit 1
fi

# S3 업로드
aws s3 cp "$JAR_FILE" "s3://$S3_BUCKET/releases/latest/$APP_NAME.jar"
echo -e "${GREEN}✅ JAR uploaded: $JAR_FILE${NC}"

# 4. Auto Scaling Group 새로고침
echo -e "${BLUE}🔄 Refreshing instances...${NC}"
ASG_NAME="${APP_NAME}-asg"

REFRESH_ID=$(aws autoscaling start-instance-refresh \
    --auto-scaling-group-name "$ASG_NAME" \
    --preferences MinHealthyPercentage=50,InstanceWarmup=300 \
    --query 'InstanceRefreshId' --output text)

echo -e "${BLUE}Instance refresh started: $REFRESH_ID${NC}"

# 5. 배포 완료 확인
echo -e "${BLUE}⏳ Waiting for deployment to complete...${NC}"
aws autoscaling wait instance-refresh-successful \
    --auto-scaling-group-name "$ASG_NAME" \
    --instance-refresh-ids "$REFRESH_ID"

# 6. Health Check
echo -e "${BLUE}🔍 Checking application health...${NC}"
sleep 30  # 애플리케이션 시작 대기

HEALTH_URL="https://interv.swote.dev/actuator/health"
for i in {1..10}; do
    if curl -f -s "$HEALTH_URL" > /dev/null; then
        echo -e "${GREEN}✅ Application is healthy!${NC}"
        break
    fi
    echo "Waiting for application... ($i/10)"
    sleep 10
done

# 7. 배포 완료
echo ""
echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
echo ""
echo "📋 Deployment Summary:"
echo "  • Application URL: https://interv.swote.dev"
echo "  • Health Check: https://interv.swote.dev/actuator/health"
echo "  • S3 Bucket: $S3_BUCKET"
echo ""
echo -e "${BLUE}💡 Next steps:${NC}"
echo "  • Monitor the application logs in CloudWatch"
echo "  • Set up GitHub Actions for automatic deployment"
echo "  • Configure monitoring and alerts"