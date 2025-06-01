from konlpy.tag import Okt
import pandas as pd
import os
import re
import nltk

# NLTK 필요 리소스 다운로드 (처음 실행 시 필요) - 수정
try:
    nltk.download('punkt')
    nltk.download('stopwords')
    nltk.download('wordnet')
except LookupError:
    print("필요한 NLTK 리소스 다운로드 중...")
    nltk.download('punkt')
    nltk.download('stopwords')
    nltk.download('wordnet')

# punkt_tab 오류 해결 시도
try:
    from nltk.tokenize import word_tokenize
except ImportError:
    print("word_tokenize 가져오기 실패, 대체 토크나이저 사용")
    def word_tokenize(text):
        return text.lower().split()

# 불용어와 레마타이저 임포트
try:
    from nltk.corpus import stopwords
    english_stop_words = set(stopwords.words('english'))
except:
    print("stopwords 로드 실패, 기본 영어 불용어 사용")
    english_stop_words = {'i', 'me', 'my', 'myself', 'we', 'our', 'ours', 'ourselves', 'you', 'your', 'yours', 
                        'yourself', 'yourselves', 'he', 'him', 'his', 'himself', 'she', 'her', 'hers', 
                        'herself', 'it', 'its', 'itself', 'they', 'them', 'their', 'theirs', 'themselves', 
                        'what', 'which', 'who', 'whom', 'this', 'that', 'these', 'those', 'am', 'is', 'are', 
                        'was', 'were', 'be', 'been', 'being', 'have', 'has', 'had', 'having', 'do', 'does', 
                        'did', 'doing', 'a', 'an', 'the', 'and', 'but', 'if', 'or', 'because', 'as', 'until', 
                        'while', 'of', 'at', 'by', 'for', 'with', 'about', 'against', 'between', 'into', 'through', 
                        'during', 'before', 'after', 'above', 'below', 'to', 'from', 'up', 'down', 'in', 'out', 
                        'on', 'off', 'over', 'under', 'again', 'further', 'then', 'once', 'here', 'there', 'when', 
                        'where', 'why', 'how', 'all', 'any', 'both', 'each', 'few', 'more', 'most', 'other', 
                        'some', 'such', 'no', 'nor', 'not', 'only', 'own', 'same', 'so', 'than', 'too', 'very', 
                        's', 't', 'can', 'will', 'just', 'don', 'should', 'now'}

try:
    from nltk.stem import WordNetLemmatizer
    english_lemmatizer = WordNetLemmatizer()
except:
    print("WordNetLemmatizer 로드 실패, 기본 함수 사용")
    def english_lemmatizer():
        def lemmatize(word):
            # 매우 기본적인 레마타이저 (s, es, ed, ing 제거)
            if word.endswith('s') and not word.endswith('ss'):
                return word[:-1]
            elif word.endswith('es'):
                return word[:-2]
            elif word.endswith('ed') and len(word) > 4:
                return word[:-2]
            elif word.endswith('ing') and len(word) > 5:
                return word[:-3]
            return word
        return type('obj', (object,), {'lemmatize': lemmatize})()

# 형태소 분석기 준비
okt = Okt()

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
            
        # UTF-8과 CP949 인코딩 모두 시도하며, 구분자 문제를 처리
        try:
            df = pd.read_csv(file_path, encoding='utf-8', quotechar='"', escapechar='\\')
        except Exception as e:
            print(f"{csv_file} - UTF-8 인코딩 실패 ({e}), CP949 시도")
            try:
                df = pd.read_csv(file_path, encoding='cp949', quotechar='"', escapechar='\\')
            except Exception as e:
                print(f"{csv_file} - CP949 인코딩도 실패 ({e}), 다른 방식 시도")
                # 다양한 구분자와 인코딩 시도
                try:
                    df = pd.read_csv(file_path, sep=None, engine='python')
                except:
                    print(f"{csv_file} - 모든 시도 실패, 파일 내용 직접 읽기")
                    # 파일 직접 읽기 시도
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        lines = f.readlines()
                    # 첫 줄은 헤더로 가정
                    header = lines[0].strip()
                    questions = [line.strip() for line in lines[1:] if line.strip()]
                    return questions
        
        print(f"CSV 파일 읽기 성공: {csv_file}")
        print(f"CSV 파일 컬럼: {df.columns.tolist()}")
        
        # '질문' 또는 'question' 컬럼이 있는지 확인
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
            print(f"첫 번째 질문 샘플: {questions[0][:50]}...")  # 첫 질문 샘플 출력
            
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

# 핵심 정보(주요 명사) 추출 함수 - 한글과 영어 모두 지원 - 오류 처리 개선
def extract_key_nouns(text):
    print(f"텍스트 길이: {len(str(text))}")
    if not text or str(text).strip() == '':
        print("텍스트가 비어 있습니다.")
        return set()
        
    # 간단한 테스트 - 텍스트의 앞부분 출력
    sample_text = str(text)[:100] + "..." if len(str(text)) > 100 else str(text)
    print(f"텍스트 샘플: {sample_text}")
    
    # 텍스트가 너무 짧으면 경고
    if len(str(text)) < 50:
        print("경고: 텍스트가 너무 짧습니다. 충분한 핵심 명사 추출이 어려울 수 있습니다.")
    
    # 키워드 추출 결과를 저장할 세트
    key_nouns = set()
    
    # 1. 한글 키워드 추출 (Konlpy/Okt 사용)
    try:
        cleaned_text_kr = re.sub(r'[^\w\s]', ' ', str(text))
        korean_nouns = okt.nouns(cleaned_text_kr)
        korean_key_nouns = {noun for noun in korean_nouns if len(noun) > 1}
        print(f"한글 핵심 명사 추출 수: {len(korean_key_nouns)}")
        if korean_key_nouns:
            print(f"한글 명사 샘플: {list(korean_key_nouns)[:10]}")
        
        # 한글 키워드 결과에 추가
        key_nouns.update(korean_key_nouns)
    except Exception as e:
        print(f"한글 키워드 추출 중 오류: {e}")
    
    # 2. 영어 키워드 추출 (NLTK 대신 직접 처리)
    try:
        # 영어 텍스트 추출 (알파벳과 공백만 남김)
        english_text = re.sub(r'[^a-zA-Z\s]', ' ', str(text))
        
        # 단어 목록 생성 (간단한 토큰화)
        try:
            words = word_tokenize(english_text.lower())
        except:
            # 백업 토큰화 방식
            words = english_text.lower().split()
        
        # 불용어 제거 및 기본형 변환
        filtered_words = []
        for word in words:
            if word.isalpha() and len(word) > 2 and word not in english_stop_words:
                try:
                    filtered_words.append(english_lemmatizer.lemmatize(word))
                except:
                    filtered_words.append(word)
        
        # 영어 키워드 수집 (중복 제거)
        english_key_nouns = set(filtered_words)
        print(f"영어 핵심 키워드 추출 수: {len(english_key_nouns)}")
        if english_key_nouns:
            print(f"영어 키워드 샘플: {list(english_key_nouns)[:10]}")
        
        # 영어 키워드 결과에 추가
        key_nouns.update(english_key_nouns)
    except Exception as e:
        print(f"영어 키워드 추출 중 오류: {e}")
    
    # 3. 특수 키워드 - 고유 명사 및 전문용어 추출 (정규표현식 사용)
    try:
        special_terms = set()
        
        # 원본 텍스트에서 직접 추출 (대소문자 구분)
        # API 관련 용어
        api_terms = ['RESTful API', 'API', 'HTTP', 'JSON', 'XML']
        # 프로그래밍 언어 및 기술
        tech_terms = ['Java', 'Spring', 'Boot', 'Django', 'Python', 'MySQL', 
                     'PostgreSQL', 'AWS', 'EC2', 'S3', 'RDS', 'NoSQL', 
                     'Git', 'GitHub', 'Docker', 'Kubernetes', 'DevOps', 'CI/CD']
        # 회사/제품 이름
        company_terms = ['NAVER', '이노베이트 솔루션즈', 'E-commerce']
        
        # 모든 특수 용어 목록
        all_special_terms = api_terms + tech_terms + company_terms
        
        # 원본 텍스트에서 용어 검색
        for term in all_special_terms:
            if term.lower() in str(text).lower():
                # 대소문자 보존을 위해 원래 형태로 추가
                special_terms.add(term)
        
        print(f"특수 키워드 추출 수: {len(special_terms)}")
        if special_terms:
            print(f"특수 키워드: {special_terms}")
        
        # 특수 키워드 결과에 추가
        key_nouns.update(special_terms)
    except Exception as e:
        print(f"특수 키워드 추출 중 오류: {e}")
    
    print(f"최종 추출된 핵심 키워드 총 개수: {len(key_nouns)}")
    return key_nouns

# CSV 파일에서 질문 목록 로드
interv_questions = load_questions_from_csv('interv_questions.csv')
wanted_questions = load_questions_from_csv('wanted_questions.csv')

# 질문별 키워드 추출 및 저장 함수
def extract_and_save_keywords_by_question(questions, file_prefix):
    print(f"\n{'='*20} 질문별 키워드 추출: {file_prefix} {'='*20}")
    
    # 결과를 저장할 리스트
    all_question_keywords = []
    
    # 각 질문별로 처리
    for i, question in enumerate(questions):
        print(f"\n--- 질문 {i+1} 처리 중 ---")
        print(f"질문: {question[:100]}..." if len(question) > 100 else f"질문: {question}")
        
        # 개선된 키워드 추출 함수 사용
        try:
            key_nouns = extract_key_nouns(question)
        except Exception as e:
            print(f"키워드 추출 전체 오류: {e}")
            key_nouns = set()  # 빈 세트로 초기화하여 계속 진행
        
        # 한글/영어/특수 키워드 분류
        korean_keywords = []
        english_keywords = []
        special_keywords = []
        
        try:
            for word in key_nouns:
                # 한글 포함 확인
                has_korean = any('\uAC00' <= ch <= '\uD7A3' for ch in word)
                # 영어만 포함 확인
                is_english_only = all(ord(ch) < 128 for ch in word)
                
                if has_korean and not is_english_only:
                    korean_keywords.append(word)
                elif is_english_only:
                    english_keywords.append(word)
                else:
                    special_keywords.append(word)
        except Exception as e:
            print(f"키워드 분류 오류: {e}")
        
        # 결과 저장
        all_question_keywords.append({
            "Question_ID": i+1,
            "Question": question,
            "Keywords": sorted(list(key_nouns)),
            "Keyword_Count": len(key_nouns),
            "Keywords_Korean": sorted(korean_keywords),
            "Keywords_English": sorted(english_keywords),
            "Keywords_Special": sorted(special_keywords)
        })
        
        print(f"추출된 키워드 ({len(key_nouns)}개): {sorted(list(key_nouns))}")
    
    # 결과 디렉토리 생성
    result_dir = os.path.join(os.getcwd(), 'result')
    os.makedirs(result_dir, exist_ok=True)
    
    # 파일로 저장
    output_filename = os.path.join(result_dir, f"{file_prefix}_keywords.csv")
    try:
        # DataFrame 생성
        keywords_df = pd.DataFrame(all_question_keywords)
        
        # 리스트 컬럼들을 문자열로 변환
        for col in ['Keywords', 'Keywords_Korean', 'Keywords_English', 'Keywords_Special']:
            keywords_df[col] = keywords_df[col].apply(lambda x: ', '.join(x))
        
        # CSV 파일로 저장
        keywords_df.to_csv(output_filename, index=False, encoding='utf-8-sig')
        print(f"\n{file_prefix} 질문별 키워드가 '{output_filename}' 파일로 저장되었습니다.")
    except Exception as e:
        print(f"\n{file_prefix} 키워드 CSV 파일 저장 중 오류 발생: {e}")
    
    return all_question_keywords

# 고객 데이터에서 키워드 추출 및 저장하는 함수
def extract_and_save_customer_keywords(customer_data):
    print(f"\n{'='*20} 고객 데이터 키워드 추출 {'='*20}")
    
    # 결과를 저장할 리스트
    all_customer_keywords = []
    
    # 각 데이터 유형별로 처리
    for data_type, data_text in customer_data.items():
        if data_type == 'all':  # 전체 합쳐진 데이터는 건너뛰기
            continue
            
        print(f"\n--- {data_type} 데이터 처리 중 ---")
        sample_text = str(data_text)[:100] + "..." if len(str(data_text)) > 100 else str(data_text)
        print(f"{data_type}: {sample_text}")
        
        # 개선된 키워드 추출 함수 사용
        try:
            key_nouns = extract_key_nouns(data_text)
        except Exception as e:
            print(f"키워드 추출 전체 오류: {e}")
            key_nouns = set()  # 빈 세트로 초기화하여 계속 진행
        
        # 한글/영어/특수 키워드 분류
        korean_keywords = []
        english_keywords = []
        special_keywords = []
        
        try:
            for word in key_nouns:
                # 한글 포함 확인
                has_korean = any('\uAC00' <= ch <= '\uD7A3' for ch in word)
                # 영어만 포함 확인
                is_english_only = all(ord(ch) < 128 for ch in word)
                
                if has_korean and not is_english_only:
                    korean_keywords.append(word)
                elif is_english_only:
                    english_keywords.append(word)
                else:
                    special_keywords.append(word)
        except Exception as e:
            print(f"키워드 분류 오류: {e}")
        
        # 결과 저장
        all_customer_keywords.append({
            "Data_Type": data_type,
            "Keywords": sorted(list(key_nouns)),
            "Keyword_Count": len(key_nouns),
            "Keywords_Korean": sorted(korean_keywords),
            "Keywords_English": sorted(english_keywords),
            "Keywords_Special": sorted(special_keywords)
        })
        
        print(f"추출된 키워드 ({len(key_nouns)}개): {sorted(list(key_nouns))}")
    
    # 결과 디렉토리 생성
    result_dir = os.path.join(os.getcwd(), 'result')
    os.makedirs(result_dir, exist_ok=True)
    
    # 파일로 저장
    output_filename = os.path.join(result_dir, "customer_data_keywords.csv")
    try:
        # DataFrame 생성
        keywords_df = pd.DataFrame(all_customer_keywords)
        
        # 리스트 컬럼들을 문자열로 변환
        for col in ['Keywords', 'Keywords_Korean', 'Keywords_English', 'Keywords_Special']:
            keywords_df[col] = keywords_df[col].apply(lambda x: ', '.join(x))
        
        # CSV 파일로 저장
        keywords_df.to_csv(output_filename, index=False, encoding='utf-8-sig')
        print(f"\n고객 데이터 키워드가 '{output_filename}' 파일로 저장되었습니다.")
    except Exception as e:
        print(f"\n고객 데이터 키워드 CSV 파일 저장 중 오류 발생: {e}")
    
    return all_customer_keywords

# 질문별 키워드 추출 및 저장
interv_keywords = extract_and_save_keywords_by_question(interv_questions, "interv")
wanted_keywords = extract_and_save_keywords_by_question(wanted_questions, "wanted")

# 고객 데이터 CSV에서 로드
customer_data = load_customer_data_from_csv('customer_data.csv')

# 고객 데이터 키워드 추출 및 저장 (새로 추가된 코드)
customer_keywords = extract_and_save_customer_keywords(customer_data)

# 질문 목록을 문자열로 변환
question_interv = ' '.join(interv_questions)
question_wanted = ' '.join(wanted_questions)

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
# 결과 디렉토리 생성
result_dir = os.path.join(os.getcwd(), 'result')
os.makedirs(result_dir, exist_ok=True)

csv_filename = os.path.join(result_dir, "keyword_including_evaluation.csv")
try:
    results_df.to_csv(csv_filename, index=False, encoding='utf-8-sig')
    print(f"\n결과가 '{csv_filename}' 파일로 성공적으로 저장되었습니다.")
except Exception as e:
    print(f"\nCSV 파일 저장 중 오류 발생: {e}")