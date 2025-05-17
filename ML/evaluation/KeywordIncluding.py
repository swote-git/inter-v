from konlpy.tag import Okt
import pandas as pd # Import pandas

# 형태소 분석기 준비
okt = Okt()

# 핵심 정보(주요 명사) 추출 함수
def extract_key_nouns(text):
    if not text:
        return set()
    nouns = okt.nouns(text)
    key_nouns = {noun for noun in nouns if len(noun) > 1}
    return key_nouns

# --- 1. 비교 대상 정의 ---

# 생성 질문 목록
question_interv = "InterV 생성 면접 질문 목록"
question_saramin = "사람인 생성 면접 질문 목록"
question_wanted = "원티드 플랫폼 생성 면접 질문 목록"

candidate_reports_map = {
    "Generated_Interview_Questions": question_interv,
    "Generated_Saramin_Questions": question_saramin,
    "Generated_Wanted_Questions": question_wanted
}

# Reference Reports (고객 데이터)
data_introduce = "저는 창의적인 문제 해결 능력을 가진 개발자입니다. 새로운 기술을 배우는 것을 좋아하며, 팀 프로젝트에서 협업을 중요하게 생각합니다. 저의 주요 기술 스택은 파이썬과 자바스크립트입니다."
data_resume = "경력사항: ABC회사 선임 개발자 (3년), XYZ스타트업 주니어 개발자 (2년). 학력: OO대학교 컴퓨터공학과 졸업. 보유 기술: Python, Java, JavaScript, React, SQL. 프로젝트 경험: 고객 관리 시스템 개발, 빅데이터 분석 플랫폼 구축."
data_project = "프로젝트명: AI 기반 추천 시스템 개발. 역할: 백엔드 개발 및 데이터 분석. 사용 기술: Python, TensorFlow, Flask. 주요 성과: 추천 정확도 15% 향상. 고객 관리 시스템 프로젝트에서는 Spring Boot와 JPA를 사용했습니다."
data_company = "우리 회사는 AI 기술을 선도하는 혁신적인 기업입니다. 데이터 분석가와 소프트웨어 엔지니어를 모집 중입니다. 주요 기술 스택은 Python, AWS, Spark 입니다. 창의적이고 도전적인 인재를 환영합니다."
data_all = data_introduce + "\n" + data_resume + "\n" + data_project + "\n" + data_company

reference_reports_list = [data_introduce, data_resume, data_project, data_company, data_all]
# 출력 시 각 대상을 구분하기 위한 리스트
reference_names_list = ["자기소개서", "이력서", "프로젝트 내역", "회사 공고", "전체 데이터"]

# --- 2. 결과 저장을 위한 리스트 초기화 ---
all_results_data = []

# --- 3. 각 후보 보고서(B)에 대해 반복 ---
for candidate_name, candidate_text in candidate_reports_map.items():
    print(f"\n\n{'='*20} CANDIDATE REPORT: {candidate_name} {'='*20}")

    # 현재 후보 보고서(B)에서 핵심 명사 추출
    key_nouns_B = extract_key_nouns(candidate_text)
    print(f"--- {candidate_name} 핵심 명사 (총 {len(key_nouns_B)}개) ---")
    if len(key_nouns_B) < 20: # 너무 많으면 출력 생략
        print(sorted(list(key_nouns_B)))
    print("-" * 30)

    if not key_nouns_B:
        print(f"경고: {candidate_name}에서 추출된 핵심 명사가 없습니다. Precision이 0으로 계산됩니다.")

    # --- 4. 각 참조 보고서(A)와 비교 및 점수 계산 ---
    print("--- 개별 참조 보고서와의 핵심 정보 포함도 평가 ---")
    for i, ref_report_text in enumerate(reference_reports_list):
        ref_name = reference_names_list[i] if i < len(reference_names_list) else f"Reference {i+1}"
        print(f"\n=== 비교: {candidate_name} vs {ref_name} ===")

        # 현재 참조 보고서(A_i)에서 핵심 명사 추출
        key_nouns_A = extract_key_nouns(ref_report_text)
        print(f"--- {ref_name} 핵심 명사 (총 {len(key_nouns_A)}개) ---")
        if len(key_nouns_A) < 20: # 너무 많으면 출력 생략
             print(sorted(list(key_nouns_A)))

        if not key_nouns_A:
            print(f"경고: {ref_name}에서 추출된 핵심 명사가 없습니다. Recall이 0으로 계산됩니다.")

        # 교집합 계산
        common_nouns = key_nouns_A.intersection(key_nouns_B)
        print(f"\n* 공통 핵심 명사 수: {len(common_nouns)}")
        if 0 < len(common_nouns) < 20:
             print(f"  - 공통 명사 목록: {sorted(list(common_nouns))}")

        # Recall 계산
        if len(key_nouns_A) == 0:
            recall = 0.0 # 실제 보고서에 명사가 없으면 Recall은 0
        else:
            recall = len(common_nouns) / len(key_nouns_A)

        # Precision 계산
        if len(key_nouns_B) == 0:
            precision = 0.0 # LLM 생성 보고서에 명사가 없으면 Precision은 0
        else:
            precision = len(common_nouns) / len(key_nouns_B)

        # F1-Score 계산
        if (recall + precision) == 0:
            f1_score = 0.0
        else:
            f1_score = 2 * (recall * precision) / (recall + precision)

        # 결과 저장
        all_results_data.append({
            "Candidate Report": candidate_name,
            "Reference Report": ref_name,
            "Candidate Nouns": len(key_nouns_B),
            "Reference Nouns": len(key_nouns_A),
            "Common Nouns": len(common_nouns),
            "Recall": recall,
            "Precision": precision,
            "F1-Score": f1_score
        })

        print(f"\n[결과 for {candidate_name} vs {ref_name}]")
        print(f"Recall (재현율): {recall:.4f}")
        print(f"Precision (정밀도): {precision:.4f}")
        print(f"F1-Score: {f1_score:.4f}")
        print("-" * 30)

# --- 5. Pandas DataFrame으로 결과 변환 및 출력 ---
results_df = pd.DataFrame(all_results_data)

print("\n\n" + "="*70)
print("--- 최종 핵심 정보 포함도 비교 결과 (DataFrame) ---")
print("="*70)
if not results_df.empty:
    print(results_df.to_string())

    # 각 Candidate Report 별 평균 점수
    print("\n\n" + "="*70)
    print("--- Candidate Report별 평균 점수 ---")
    print("="*70)
    # 'Candidate Nouns', 'Reference Nouns', 'Common Nouns'는 평균내는 것이 의미가 없을 수 있어 제외하거나 다른 방식으로 집계
    numeric_cols = ["Recall", "Precision", "F1-Score"]
    # numeric_only=True 옵션은 최신 pandas에서 groupby().mean()에 기본 적용될 수 있으나 명시적으로 컬럼 선택
    avg_scores_df = results_df.groupby("Candidate Report")[numeric_cols].mean()
    print(avg_scores_df.to_string())

else:
    print("처리된 결과가 없습니다.")
print("="*70)

# --- 6. CSV 파일로 저장 ---
csv_filename = "keyword_including_evaluation.csv"
try:
    results_df.to_csv(csv_filename, index=False, encoding='utf-8-sig')
    print(f"\n결과가 '{csv_filename}' 파일로 성공적으로 저장되었습니다.")
except Exception as e:
    print(f"\nCSV 파일 저장 중 오류 발생: {e}")