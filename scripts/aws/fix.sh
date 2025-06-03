#!/bin/bash

echo "🚀 간단한 퍼블릭 인스턴스 배포"
echo "============================="

AWS_REGION="ap-northeast-2"
TARGET_GROUP_ARN="arn:aws:elasticloadbalancing:ap-northeast-2:034115074124:targetgroup/interv-tg/37dfe24a3529f0f3"

# 1. 퍼블릭 인스턴스 찾기
echo "📊 퍼블릭 인스턴스 검색..."

PUBLIC_INSTANCE_DATA=$(aws ec2 describe-instances \
  --region $AWS_REGION \
  --filters "Name=instance-state-name,Values=running" \
  --query 'Reservations[*].Instances[?PublicIpAddress!=null].[InstanceId,PublicIpAddress,PrivateIpAddress,LaunchTime]' \
  --output text | sort -k4 -r | head -n1)

if [ -z "$PUBLIC_INSTANCE_DATA" ]; then
    echo "❌ 퍼블릭 인스턴스가 없습니다!"
    exit 1
fi

INSTANCE_ID=$(echo $PUBLIC_INSTANCE_DATA | cut -d' ' -f1)
PUBLIC_IP=$(echo $PUBLIC_INSTANCE_DATA | cut -d' ' -f2)
PRIVATE_IP=$(echo $PUBLIC_INSTANCE_DATA | cut -d' ' -f3)

echo "선택된 인스턴스:"
echo "  ID: $INSTANCE_ID"
echo "  퍼블릭 IP: $PUBLIC_IP"
echo "  프라이빗 IP: $PRIVATE_IP"

# 2. Python 웹 앱 파일 생성 (로컬에서)
echo ""
echo "📝 웹 애플리케이션 생성..."

cat > simple_app.py << 'EOF'
#!/usr/bin/env python3
import http.server
import socketserver
import json
from datetime import datetime

class MyHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            html = '''<!DOCTYPE html>
<html>
<head><title>Interv Application</title></head>
<body>
    <h1>🚀 Interv Application</h1>
    <p>서버 시간: ''' + str(datetime.now()) + '''</p>
    <p>상태: 정상 운영 중</p>
    <ul>
        <li><a href="/health">Health Check</a></li>
        <li><a href="/actuator/health">Actuator Health</a></li>
    </ul>
</body>
</html>'''
            self.wfile.write(html.encode())
        elif self.path == '/health':
            self.send_response(200)
            self.send_header('Content-type', 'text/plain')
            self.end_headers()
            self.wfile.write(b'OK')
        elif self.path == '/actuator/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            health = {"status": "UP", "timestamp": datetime.now().isoformat()}
            self.wfile.write(json.dumps(health).encode())
        else:
            super().do_GET()

PORT = 8080
print(f"서버 시작: 포트 {PORT}")
with socketserver.TCPServer(("", PORT), MyHandler) as httpd:
    httpd.serve_forever()
EOF

echo "✅ 애플리케이션 파일 생성 완료"

# 3. 보안 그룹 설정
echo ""
echo "🔒 보안 그룹 포트 8080 열기..."

SECURITY_GROUPS=$(aws ec2 describe-instances \
  --instance-ids $INSTANCE_ID \
  --region $AWS_REGION \
  --query 'Reservations[0].Instances[0].SecurityGroups[*].GroupId' \
  --output text)

for sg in $SECURITY_GROUPS; do
    echo "보안 그룹 $sg에 포트 8080 규칙 추가..."
    aws ec2 authorize-security-group-ingress \
      --group-id $sg \
      --protocol tcp \
      --port 8080 \
      --cidr 0.0.0.0/0 \
      --region $AWS_REGION 2>/dev/null && echo "✅ 추가됨" || echo "ℹ️  이미 존재함"
done

# 4. SSM을 통한 배포
echo ""
echo "🚀 SSM을 통한 배포..."

# 단순한 명령어들로 분리
aws ssm send-command \
  --document-name "AWS-RunShellScript" \
  --parameters 'commands=["yum update -y","yum install -y python3","mkdir -p /home/ec2-user/app"]' \
  --targets "Key=instanceids,Values=$INSTANCE_ID" \
  --region $AWS_REGION \
  --query 'Command.CommandId' \
  --output text

echo "⏳ 패키지 설치 대기... (60초)"
sleep 60

# 5. 애플리케이션 파일 전송 및 실행
echo "애플리케이션 파일 전송..."

# 파일 내용을 base64로 인코딩해서 전송
APP_CONTENT=$(base64 -w 0 simple_app.py)

DEPLOY_CMD_ID=$(aws ssm send-command \
  --document-name "AWS-RunShellScript" \
  --parameters "commands=[
    \"echo '$APP_CONTENT' | base64 -d > /home/ec2-user/app/app.py\",
    \"cd /home/ec2-user/app\",
    \"pkill -f 'python.*8080' || echo 'No existing process'\",
    \"nohup python3 app.py > server.log 2>&1 &\",
    \"sleep 5\",
    \"ps aux | grep python\",
    \"netstat -tlnp | grep 8080\",
    \"curl -s http://localhost:8080/health || echo 'Health check failed'\"
  ]" \
  --targets "Key=instanceids,Values=$INSTANCE_ID" \
  --region $AWS_REGION \
  --query 'Command.CommandId' \
  --output text)

echo "배포 명령어 ID: $DEPLOY_CMD_ID"
echo "⏳ 애플리케이션 시작 대기... (30초)"
sleep 30

# 6. 배포 결과 확인
echo ""
echo "📊 배포 결과:"
aws ssm get-command-invocation \
  --command-id $DEPLOY_CMD_ID \
  --instance-id $INSTANCE_ID \
  --region $AWS_REGION \
  --query 'StandardOutputContent' \
  --output text

# 7. 퍼블릭 IP 테스트
echo ""
echo "🌐 퍼블릭 IP 직접 테스트:"
curl -s http://$PUBLIC_IP:8080/health && echo " ✅ 헬스체크 성공" || echo " ❌ 헬스체크 실패"

# 8. Target Group 설정
echo ""
echo "🎯 ALB Target Group 설정..."

# 기존 타겟 제거
EXISTING_TARGETS=$(aws elbv2 describe-target-health \
  --target-group-arn $TARGET_GROUP_ARN \
  --region $AWS_REGION \
  --query 'TargetHealthDescriptions[*].Target.Id' \
  --output text 2>/dev/null)

if [ -n "$EXISTING_TARGETS" ]; then
    for target in $EXISTING_TARGETS; do
        aws elbv2 deregister-targets \
          --target-group-arn $TARGET_GROUP_ARN \
          --targets Id=$target \
          --region $AWS_REGION 2>/dev/null
    done
    echo "기존 타겟 제거 완료"
fi

# 새 인스턴스 등록
aws elbv2 register-targets \
  --target-group-arn $TARGET_GROUP_ARN \
  --targets Id=$INSTANCE_ID,Port=8080 \
  --region $AWS_REGION

echo "✅ 새 인스턴스 등록 완료: $INSTANCE_ID"

# 9. 최종 확인
echo ""
echo "⏳ ALB 헬스체크 대기... (90초)"
sleep 90

echo ""
echo "🎉 최종 상태"
echo "==========="

echo "Target Group 헬스 상태:"
aws elbv2 describe-target-health \
  --target-group-arn $TARGET_GROUP_ARN \
  --region $AWS_REGION \
  --query 'TargetHealthDescriptions[*].[Target.Id,TargetHealth.State,TargetHealth.Description]' \
  --output table

echo ""
echo "🌐 접속 정보:"
echo "- ALB: http://interv.swote.dev"
echo "- 직접: http://$PUBLIC_IP:8080"
echo "- 헬스: http://interv.swote.dev/health"

echo ""
echo "📊 서버 정보:"
echo "- 인스턴스: $INSTANCE_ID"
echo "- 퍼블릭 IP: $PUBLIC_IP"
echo "- 포트: 8080"

# cleanup
rm -f simple_app.py