from bert_score import BERTScorer
import pandas as pd
import torch

# --- 1. 비교 대상 정의 ---

# 생성 질문 목록록
question_interv = "InterV 서비스에서 생성된 면접 예상 질문입니다. 귀하의 가장 큰 성과는 무엇이었으며, 그 과정에서 어떤 어려움을 극복했는지 설명해주십시오."
question_saramin = "사람인 사이트의 채용 공고를 기반으로 생성한 면접 질문입니다. 우리 회사의 경쟁사 대비 강점은 무엇이라고 생각하며, 입사 후 어떤 기여를 할 수 있습니까?"
question_wanted = "원티드 플랫폼의 기업 정보를 바탕으로 만든 면접 질문입니다. 지원하신 직무에서 가장 중요하다고 생각하는 역량 세 가지와 그 이유를 말씀해주세요."

candidate_reports_map = {
    "Generated_Interview_Questions": question_interv,
    "Generated_Saramin_Questions": question_saramin,
    "Generated_Wanted_Questions": question_wanted
}

# 고객 데이터
data_introduce = "저는 창의적인 문제 해결 능력을 가진 개발자입니다. 새로운 기술을 배우는 것을 좋아하며, 팀 프로젝트에서 협업을 중요하게 생각합니다. 저의 주요 기술 스택은 파이썬과 자바스크립트입니다."
data_resume = "경력사항: ABC회사 선임 개발자 (3년), XYZ스타트업 주니어 개발자 (2년). 학력: OO대학교 컴퓨터공학과 졸업. 보유 기술: Python, Java, JavaScript, React, SQL. 프로젝트 경험: 고객 관리 시스템 개발, 빅데이터 분석 플랫폼 구축."
data_project = "프로젝트명: AI 기반 추천 시스템 개발. 역할: 백엔드 개발 및 데이터 분석. 사용 기술: Python, TensorFlow, Flask. 주요 성과: 추천 정확도 15% 향상. 고객 관리 시스템 프로젝트에서는 Spring Boot와 JPA를 사용했습니다."
data_company = "우리 회사는 AI 기술을 선도하는 혁신적인 기업입니다. 데이터 분석가와 소프트웨어 엔지니어를 모집 중입니다. 주요 기술 스택은 Python, AWS, Spark 입니다. 창의적이고 도전적인 인재를 환영합니다."
data_all = data_introduce + "\n" + data_resume + "\n" + data_project + "\n" + data_company

reference_reports_list = [data_introduce, data_resume, data_project, data_company, data_all]
reference_names_list = ["자기소개서", "이력서", "프로젝트 내역", "회사 공고", "전체 데이터"]


# --- 2. BERTScore 계산 ---
model_name = "klue/bert-base"
bert_score_num_layers = 12

print(f"BERTScore 계산 시작 (모델: {model_name}, 사용할 BERT 레이어 인덱스: {bert_score_num_layers})...")
if torch.cuda.is_available():
    device = 'cuda'
    print(f"GPU 사용 가능 ({torch.cuda.get_device_name(0)}). BERTScore 계산이 더 빠를 수 있습니다.")
else:
    device = 'cpu'
    print("GPU 사용 불가능. CPU로 계산합니다 (다소 느릴 수 있음).")

scorer = BERTScorer(model_type=model_name,
                    lang="ko",
                    num_layers=bert_score_num_layers,
                    idf=False,
                    device=device,
                    rescale_with_baseline=False
                   )

all_results_data = []

print(f"\n총 {len(candidate_reports_map) * len(reference_reports_list)}회의 개별 BERTScore 계산을 수행합니다.")

for candidate_name, candidate_text in candidate_reports_map.items():
    print(f"\n--- Candidate: {candidate_name} 처리 중 ---")

    for i, ref_text in enumerate(reference_reports_list):
        ref_name = reference_names_list[i]

        P_tensor_single, R_tensor_single, F1_tensor_single = scorer.score(
            cands=[candidate_text],
            refs=[ref_text]
        )

        precision = P_tensor_single[0].item()
        recall = R_tensor_single[0].item()
        f1 = F1_tensor_single[0].item()

        all_results_data.append({
            "Candidate Report": candidate_name,
            "Reference Report": ref_name,
            "BERT_Precision": precision,
            "BERT_Recall": recall,
            "BERT_F1": f1
        })
        print(f"    {candidate_name} vs {ref_name}: P={precision:.4f}, R={recall:.4f}, F1={f1:.4f}")

# --- 3. Pandas DataFrame으로 결과 변환 및 출력 ---
results_df = pd.DataFrame(all_results_data)

print("\n\n" + "="*70)
print("--- 최종 BERTScore 평가 결과 (DataFrame) ---")
print("="*70)
if not results_df.empty:
    print(results_df.to_string())

    print("\n\n" + "="*70)
    print("--- Candidate Report별 평균 BERTScore ---")
    print("="*70)
    avg_scores_df = results_df.groupby("Candidate Report")[["BERT_Precision", "BERT_Recall", "BERT_F1"]].mean()
    print(avg_scores_df.to_string())
else:
    print("처리된 결과가 없습니다.")
print("="*70)

# --- 4. CSV 파일로 저장 ---
csv_filename = "bertscore_evaluation.csv"
try:
    results_df.to_csv(csv_filename, index=False, encoding='utf-8-sig')
    print(f"\n결과가 '{csv_filename}' 파일로 성공적으로 저장되었습니다.")
except Exception as e:
    print(f"\nCSV 파일 저장 중 오류 발생: {e}")