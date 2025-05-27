#!/bin/bash
# Terraform을 사용한 안전한 리소스 삭제

cd infrastructure/

echo "🗑️ Terraform Destroy - 안전한 리소스 삭제"
echo "=========================================="
echo ""
echo "⚠️ 경고: 모든 InterV 리소스가 삭제됩니다!"
echo "⚠️ 데이터베이스, 스토리지 등 모든 데이터가 영구 삭제됩니다!"
echo ""
echo "정말로 계속하시겠습니까? (yes 입력)"
read -r confirmation
if [ "$confirmation" != "yes" ]; then
    echo "취소되었습니다."
    exit 0
fi

# 환경변수 설정
export TF_VAR_db_password="interv2025!"
export TF_VAR_key_pair_name="interv-keypair"
export TF_VAR_aws_region="ap-northeast-2"
export TF_VAR_app_name="interv"
export TF_VAR_domain_name="interv.swote.dev"

echo ""
echo "🏗️ Terraform 초기화..."
terraform init

echo ""
echo "📋 삭제할 리소스 미리보기..."
terraform plan -destroy

echo ""
echo "정말로 삭제하시겠습니까? (DELETE 입력)"
read -r final_confirmation
if [ "$final_confirmation" != "DELETE" ]; then
    echo "취소되었습니다."
    exit 0
fi

echo ""
echo "🗑️ Terraform Destroy 실행..."
if terraform destroy -auto-approve; then
    echo ""
    echo "✅ Terraform Destroy 완료!"
    
    # State 파일 정리
    echo "🧹 State 파일 정리..."
    rm -f terraform.tfstate*
    rm -f tfplan
    rm -rf .terraform/
    
    echo ""
    echo "🎉 모든 리소스 삭제 완료!"
    echo "✅ 이제 깨끗한 상태에서 새로 배포할 수 있습니다."
    
else
    echo ""
    echo "❌ Terraform Destroy 실패"
    echo "수동으로 리소스를 정리해야 할 수 있습니다."
    echo ""
    echo "대안: complete_cleanup.sh 스크립트 사용"
fi