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

# 시스템 업데이트
yum update -y

# Java 17 설치
yum install -y java-17-amazon-corretto-devel

# 애플리케이션 사용자 생성
useradd -m -s /bin/bash appuser

# 애플리케이션 디렉토리 생성
mkdir -p /opt/$APP_NAME
mkdir -p /var/log/$APP_NAME
chown -R appuser:appuser /opt/$APP_NAME /var/log/$APP_NAME

# S3에서 애플리케이션 JAR 다운로드
aws s3 cp s3://$S3_BUCKET/releases/latest/$APP_NAME.jar /opt/$APP_NAME/$APP_NAME.jar
chown appuser:appuser /opt/$APP_NAME/$APP_NAME.jar

# 애플리케이션 설정 파일 생성
cat > /opt/$APP_NAME/application-prod.yml << EOF
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://$DB_ENDPOINT:3306/$DB_NAME?serverTimezone=UTC&characterEncoding=UTF-8&useSSL=true
    username: $DB_USERNAME
    password: $DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        show_sql: false

server:
  port: 8080

aws:
  region: $AWS_REGION
  s3:
    bucket: $S3_BUCKET

logging:
  level:
    root: INFO
  file:
    name: /var/log/$APP_NAME/application.log

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
EOF

chown appuser:appuser /opt/$APP_NAME/application-prod.yml

# 시스템 서비스 생성
cat > /etc/systemd/system/$APP_NAME.service << EOF
[Unit]
Description=$APP_NAME Spring Boot Application
After=network.target

[Service]
Type=forking
User=appuser
Group=appuser
WorkingDirectory=/opt/$APP_NAME
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod -Xms512m -Xmx1024m /opt/$APP_NAME/$APP_NAME.jar
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=$APP_NAME

Environment=SPRING_PROFILES_ACTIVE=prod
Environment=AWS_REGION=$AWS_REGION

[Install]
WantedBy=multi-user.target
EOF

# 서비스 시작
systemctl daemon-reload
systemctl enable $APP_NAME
systemctl start $APP_NAME

# Health Check 대기
echo "Waiting for application to start..."
for i in {1..30}; do
    if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
        echo "Application started successfully!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 10
done

echo "EC2 initialization completed!"