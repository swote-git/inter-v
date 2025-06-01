from bert_score import BERTScorer
import pandas as pd
import torch
import os

# 파일 경로 설정
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, 'data')

print(f"현재 작업 디렉토리: {os.getcwd()}")
print(f"BASE_DIR: {BASE_DIR}")
print(f"DATA_DIR: {DATA_DIR}")

# CSV 파일에서 질문 목록 읽어오는 함수
def load_questions_from_csv(csv_file):
    try:
        file_path = os.path.join(DATA_DIR, csv_file)
        print(f"CSV 파일 읽기 시도: {file_path}")
        
        if not os.path.exists(file_path):
            print(f"파일을 찾을 수 없음: {file_path}")
            return []
            
        # CSV 파일 읽기
        try:
            df = pd.read_csv(file_path, encoding='utf-8', quotechar='"', escapechar='\\')
        except Exception as e:
            print(f"{csv_file} - UTF-8 인코딩 실패 ({e}), CP949 시도")
            try:
                df = pd.read_csv(file_path, encoding='cp949', quotechar='"', escapechar='\\')
            except Exception as e:
                print(f"{csv_file} - CSV 읽기 실패: {e}")
                return []
        
        print(f"CSV 파일 읽기 성공: {csv_file}")
        print(f"CSV 파일 컬럼: {df.columns.tolist()}")
        
        # '질문' 컬럼 찾기
        question_col = None
        for col in ['질문', 'question', 'Question']:
            if col in df.columns:
                question_col = col
                break
                
        if question_col is None:
            print(f"경고: {csv_file}에서 질문 컬럼을 찾을 수 없습니다.")
            return []
            
        # NaN 값 처리 및 문자열 변환
        questions = df[question_col].fillna('').astype(str).tolist()
        questions = [q for q in questions if q and q != 'nan']
        
        print(f"질문 개수: {len(questions)}")
        if questions:
            print(f"첫 번째 질문 샘플: {questions[0][:50]}...")
            
        return questions
    except Exception as e:
        print(f"CSV 파일 '{csv_file}' 읽기 오류: {e}")
        return []

# CSV 파일에서 고객 데이터 읽어오는 함수
def load_customer_data_from_csv(csv_file):
    try:
        file_path = os.path.join(DATA_DIR, csv_file)
        print(f"고객 데이터 CSV 파일 읽기 시도: {file_path}")
        
        if not os.path.exists(file_path):
            print(f"파일을 찾을 수 없음: {file_path}")
            return {
                'introduce': "",
                'resume': "",
                'project': "",
                'company': "",
                'all': ""
            }
        
        # 간단한 방식으로 CSV 파일 읽기
        df = pd.read_csv(file_path, encoding='utf-8')
        print(f"CSV 파일 읽기 성공: {file_path}")
        print(f"컬럼: {df.columns.tolist()}")
        
        # 데이터 매핑
        data = {
            'introduce': df['자기소개서'].iloc[0] if '자기소개서' in df.columns else "",
            'resume': df['이력서'].iloc[0] if '이력서' in df.columns else "",
            'project': df['프로젝트'].iloc[0] if '프로젝트' in df.columns else "",
            'company': df['회사공고'].iloc[0] if '회사공고' in df.columns else ""
        }
        
        # 각 필드 길이 출력 (디버깅용)
        for key, value in data.items():
            value_str = str(value)
            print(f"'{key}' 데이터 길이: {len(value_str)} 문자")
            print(f"'{key}' 데이터 샘플: {value_str[:100]}..." if len(value_str) > 100 else f"'{key}' 데이터: {value_str}")
        
        # 전체 데이터 합치기
        data['all'] = ' '.join(str(value) for value in data.values())
        
        return data
    except Exception as e:
        print(f"CSV 파일 '{csv_file}' 읽기 오류: {e}")
        return {
            'introduce': "",
            'resume': "",
            'project': "",
            'company': "",
            'all': ""
        }

# --- 1. 비교 대상 정의 ---

# CSV 파일에서 질문 목록 로드
interv_questions = load_questions_from_csv('interv_questions.csv')
wanted_questions = load_questions_from_csv('wanted_questions.csv')

# 질문 목록을 문자열로 변환
question_interv = ' '.join(interv_questions)
question_wanted = ' '.join(wanted_questions)

# 사람인 관련 코드 제거하고 두 가지 질문만 유지
candidate_reports_map = {
    "Generated_InterV_Questions": question_interv,
    "Generated_Wanted_Questions": question_wanted
}

# 고객 데이터 CSV에서 로드
customer_data = load_customer_data_from_csv('customer_data.csv')

# Reference Reports (고객 데이터)
data_introduce = customer_data['introduce']
data_resume = customer_data['resume']
data_project = customer_data['project']
data_company = customer_data['company']
data_all = customer_data['all']

reference_reports_list = [data_introduce, data_resume, data_project, data_company, data_all]
reference_names_list = ["자기소개서", "이력서", "프로젝트 내역", "회사 공고", "전체 데이터"]


# --- 2. BERTScore 계산 ---
# 다국어 BERT 모델 사용 (기존 KLUE-BERT 대신)
model_name = "bert-base-multilingual-cased"
bert_score_num_layers = 12

print(f"BERTScore 계산 시작 (모델: {model_name}, 사용할 BERT 레이어 인덱스: {bert_score_num_layers})...")
print("다국어 BERT 모델을 사용하여 한글과 영어가 혼합된 텍스트에 대한 성능을 향상시킵니다.")

if torch.cuda.is_available():
    device = 'cuda'
    print(f"GPU 사용 가능 ({torch.cuda.get_device_name(0)}). BERTScore 계산이 더 빠를 수 있습니다.")
else:
    device = 'cpu'
    print("GPU 사용 불가능. CPU로 계산합니다 (다소 느릴 수 있음).")

scorer = BERTScorer(model_type=model_name,
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
print("--- 최종 BERTScore 평가 결과 (다국어 BERT) ---")
print("="*70)
if not results_df.empty:
    print(results_df.to_string())

    print("\n\n" + "="*70)
    print("--- Candidate Report별 평균 BERTScore (다국어 BERT) ---")
    print("="*70)
    avg_scores_df = results_df.groupby("Candidate Report")[["BERT_Precision", "BERT_Recall", "BERT_F1"]].mean()
    print(avg_scores_df.to_string())
else:
    print("처리된 결과가 없습니다.")
print("="*70)

# --- 4. CSV 파일로 저장 ---
# 결과 디렉토리 생성
result_dir = os.path.join(os.getcwd(), 'result')
os.makedirs(result_dir, exist_ok=True)

csv_filename = os.path.join(result_dir, "bertscore_evaluation.csv")
try:
    results_df.to_csv(csv_filename, index=False, encoding='utf-8-sig')
    print(f"\n결과가 '{csv_filename}' 파일로 성공적으로 저장되었습니다.")
except Exception as e:
    print(f"\nCSV 파일 저장 중 오류 발생: {e}")