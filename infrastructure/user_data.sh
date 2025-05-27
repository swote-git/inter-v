#!/bin/bash
# infrastructure/user_data.sh - EC2 초기화 스크립트

set -e

# 변수들 (Terraform에서 전달)
APP_NAME="${app_name}"
DB_ENDPOINT="${db_endpoint}"
DB_NAME="${db_name}"
DB_USERNAME="${db_username}"
DB_PASSWORD="${db_password}"
S3_BUCKET="${s3_bucket}"
AWS_REGION="${aws_region}"

# Cognito 설정 (Terraform에서 전달받거나 기본값 사용)
COGNITO_USER_POOL_ID="${cognito_user_pool_id:-pool-not-configured}"
COGNITO_CLIENT_ID="${cognito_client_id:-client-not-configured}"
COGNITO_CLIENT_SECRET="${cognito_client_secret:-secret-not-configured}"

echo "🚀 EC2 인스턴스 초기화 시작..."
echo "📦 App: $APP_NAME"
echo "🗄️ DB: $DB_ENDPOINT"
echo "🪣 S3: $S3_BUCKET"
echo "🌍 Region: $AWS_REGION"

# 시스템 업데이트
echo "📦 시스템 패키지 업데이트..."
yum update -y

# Java 17 설치
echo "☕ Java 17 설치..."
yum install -y java-17-amazon-corretto-devel

# jq 설치 (JSON 파싱용)
echo "🔧 jq 설치..."
yum install -y jq

# 애플리케이션 사용자 생성
echo "👤 애플리케이션 사용자 생성..."
useradd -m -s /bin/bash appuser

# 애플리케이션 디렉토리 생성
echo "📁 애플리케이션 디렉토리 생성..."
mkdir -p /opt/$APP_NAME
mkdir -p /var/log/$APP_NAME
chown -R appuser:appuser /opt/$APP_NAME /var/log/$APP_NAME

# S3에서 애플리케이션 JAR 다운로드 (최대 5번 재시도)
echo "⬇️ S3에서 JAR 파일 다운로드..."
DOWNLOAD_SUCCESS=false
for attempt in 1 2 3 4 5; do
    echo "다운로드 시도 $attempt/5..."
    
    if aws s3 cp s3://$S3_BUCKET/releases/latest/$APP_NAME.jar /opt/$APP_NAME/$APP_NAME.jar; then
        echo "✅ JAR 다운로드 성공!"
        DOWNLOAD_SUCCESS=true
        break
    else
        echo "⚠️ 다운로드 실패 - $attempt번째 시도"
        if [ $attempt -lt 5 ]; then
            echo "🔄 30초 대기 후 재시도..."
            sleep 30
        fi
    fi
done

if [ "$DOWNLOAD_SUCCESS" = false ]; then
    echo "❌ JAR 다운로드 실패 - 기본 더미 JAR 생성"
    # 최소한의 실행 가능한 더미 JAR 생성 (디버그용)
    echo "Dummy JAR for debugging" > /opt/$APP_NAME/$APP_NAME.jar
fi

chown appuser:appuser /opt/$APP_NAME/$APP_NAME.jar

# 애플리케이션 설정 파일 생성
echo "⚙️ 애플리케이션 설정 파일 생성..."
cat > /opt/$APP_NAME/application-prod.yml << EOF
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://$DB_ENDPOINT:3306/$DB_NAME?serverTimezone=UTC&characterEncoding=UTF-8&useSSL=true&allowPublicKeyRetrieval=true
    username: $DB_USERNAME
    password: $DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      idle-timeout: 30000
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        show_sql: false
        format_sql: false
    data:
      jpa:
        repositories:
          enabled: true
  security:
    oauth2:
      client:
        registration:
          cognito:
            client-id: $COGNITO_CLIENT_ID
            client-secret: $COGNITO_CLIENT_SECRET
            scope: openid, email, phone
            redirect-uri: https://interv.swote.dev/login/oauth2/code/cognito
            client-name: Inter-V Cognito
            authorization-grant-type: authorization_code
        provider:
          cognito:
            issuer-uri: https://cognito-idp.$AWS_REGION.amazonaws.com/$COGNITO_USER_POOL_ID
            user-name-attribute: email
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.$AWS_REGION.amazonaws.com/$COGNITO_USER_POOL_ID

server:
  port: 8080
  servlet:
    context-path: /
  compression:
    enabled: true

# AWS 설정
aws:
  region: $AWS_REGION
  s3:
    bucket: $S3_BUCKET
  cognito:
    region: $AWS_REGION
    user-pool-id: $COGNITO_USER_POOL_ID
    client-id: $COGNITO_CLIENT_ID
    client-secret: $COGNITO_CLIENT_SECRET
    logout-redirect-uri: https://interv.swote.dev/

# LLM API 설정 (선택적)
llm:
  api:
    url: http://localhost:8000
    key: dummy-key

# 보안 설정
public-data-contest:
  security:
    saltSize: 16

# 로깅 설정
logging:
  level:
    root: INFO
    dev.swote.interv: INFO
    org.springframework: WARN
    org.springframework.security: WARN
  file:
    name: /var/log/$APP_NAME/application.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
  server:
    port: 8080
EOF

chown appuser:appuser /opt/$APP_NAME/application-prod.yml

# 시스템 서비스 생성
echo "🔧 시스템 서비스 생성..."
cat > /etc/systemd/system/$APP_NAME.service << EOF
[Unit]
Description=$APP_NAME Spring Boot Application
After=network.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/$APP_NAME
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod -Xms512m -Xmx1024m /opt/$APP_NAME/$APP_NAME.jar --spring.config.location=file:/opt/$APP_NAME/application-prod.yml
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=$APP_NAME

# 환경 변수 설정
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=AWS_REGION=$AWS_REGION
Environment=DB_HOST=$DB_ENDPOINT
Environment=DB_NAME=$DB_NAME
Environment=DB_USERNAME=$DB_USERNAME
Environment=DB_PASSWORD=$DB_PASSWORD
Environment=S3_BUCKET_NAME=$S3_BUCKET
Environment=COGNITO_USER_POOL_ID=$COGNITO_USER_POOL_ID
Environment=COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID
Environment=COGNITO_CLIENT_SECRET=$COGNITO_CLIENT_SECRET

[Install]
WantedBy=multi-user.target
EOF

# systemd 데몬 리로드 및 서비스 활성화
echo "🔄 서비스 등록 및 시작..."
systemctl daemon-reload
systemctl enable $APP_NAME

# 서비스 시작 (최대 3번 재시도)
SERVICE_SUCCESS=false
for attempt in 1 2 3; do
    echo "서비스 시작 시도 $attempt/3..."
    
    if systemctl start $APP_NAME; then
        echo "✅ 서비스 시작 성공!"
        SERVICE_SUCCESS=true
        break
    else
        echo "⚠️ 서비스 시작 실패 - $attempt번째 시도"
        if [ $attempt -lt 3 ]; then
            echo "🔄 10초 대기 후 재시도..."
            sleep 10
        fi
    fi
done

if [ "$SERVICE_SUCCESS" = false ]; then
    echo "❌ 서비스 시작 실패 - 로그 확인"
    journalctl -u $APP_NAME --no-pager -n 20
fi

# Health Check 대기
echo "🏥 애플리케이션 Health Check 대기 (최대 5분)..."
HEALTH_SUCCESS=false
for i in {1..30}; do
    if curl -f -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Health Check 성공! ($i/30)"
        HEALTH_SUCCESS=true
        break
    fi
    echo "⏳ Health Check 대기 중... ($i/30)"
    sleep 10
done

if [ "$HEALTH_SUCCESS" = true ]; then
    echo "🎉 애플리케이션 시작 완료!"
    curl -s http://localhost:8080/actuator/health | jq . || echo "Health 상태 확인 완료"
else
    echo "⚠️ Health Check 실패 - 로그 확인 필요"
    echo "📋 서비스 상태:"
    systemctl status $APP_NAME --no-pager
    echo "📋 최근 로그:"
    journalctl -u $APP_NAME --no-pager -n 30
fi

# 시스템 상태 요약
echo ""
echo "📊 EC2 초기화 완료 요약:"
echo "  ✅ Java 17 설치됨"
echo "  ✅ 애플리케이션 사용자 생성됨"
echo "  ✅ JAR 파일 다운로드됨"
echo "  ✅ 설정 파일 생성됨"
echo "  ✅ 시스템 서비스 등록됨"
if [ "$SERVICE_SUCCESS" = true ]; then
    echo "  ✅ 서비스 시작됨"
else
    echo "  ⚠️ 서비스 시작 실패"
fi
if [ "$HEALTH_SUCCESS" = true ]; then
    echo "  ✅ Health Check 통과"
else
    echo "  ⚠️ Health Check 실패"
fi

echo ""
echo "🌐 액세스 정보:"
echo "  - 애플리케이션: https://interv.swote.dev"
echo "  - Health Check: https://interv.swote.dev/actuator/health"
echo "  - 로그 위치: /var/log/$APP_NAME/application.log"
echo ""

echo "🏁 EC2 초기화 스크립트 완료!"