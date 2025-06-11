/* =========================================================
   Interview.jsx  (Enhanced v6 - 인증 강화 & 난이도 선택)
   ========================================================= */
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';
import {
  createInterview,
  startInterview,
  getNextQuestion,
  getInterviewQuestions,
  uploadAudioAnswer,
  completeInterview,
  updateInterviewTime,
  getMyResume,
  resumeExists,
  listCompanies,
  listPositions,
  checkAuthStatus,
} from '../utils/api';

export default function Interview() {
  const navigate = useNavigate();

  /* --------------------------------------------------
     local state
  -------------------------------------------------- */
  const [interview, setInterview] = useState(null);
  const [question, setQuestion] = useState(null);
  const [seconds, setSeconds] = useState(0);
  const [totalSec, setTotalSec] = useState(0);
  const [isCounting, setIsCounting] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [audioBlob, setAudioBlob] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [summary, setSummary] = useState(null);
  const mediaRef = useRef(null);
  const chunks = useRef([]);
  const timerRef = useRef(null);
  
  // 면접 설정 상태
  const [companies, setCompanies] = useState([]);
  const [positions, setPositions] = useState([]);
  const [selectedCompany, setSelectedCompany] = useState('');
  const [selectedPosition, setSelectedPosition] = useState('');
  const [resumeId, setResumeId] = useState(null);
  const [hasResume, setHasResume] = useState(false);
  const [loadingSetup, setLoadingSetup] = useState(true);
  const [questionCount, setQuestionCount] = useState(5);
  const [selectedDifficulty, setSelectedDifficulty] = useState(3); // 새로 추가
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [authStatus, setAuthStatus] = useState(null);

  /* --------------------------------------------------
     인증 상태 확인
  -------------------------------------------------- */
  useEffect(() => {
    const auth = checkAuthStatus();
    if (!auth.isAuthenticated) {
      console.log('인증되지 않음 - 로그인 페이지로 이동');
      navigate('/signin');
      return;
    }
    
    setAuthStatus(auth);
    console.log('인증 확인됨:', auth);
  }, [navigate]);

  /* --------------------------------------------------
     초기 데이터 로드
  -------------------------------------------------- */
  useEffect(() => {
    if (!authStatus?.isAuthenticated) return;
    
    const loadInitialData = async () => {
      try {
        console.log('초기 데이터 로드 시작...');
        
        // 이력서 존재 여부 확인
        const { data: existsData } = await resumeExists();
        setHasResume(existsData.data);
        console.log('이력서 존재 여부:', existsData.data);
        
        if (existsData.data) {
          // 이력서가 있으면 ID 가져오기
          const { data: resumeData } = await getMyResume();
          setResumeId(resumeData.data.id);
          console.log('내 이력서 ID:', resumeData.data.id);
        }
        
        // 회사 목록 가져오기
        const { data: companiesData } = await listCompanies();
        setCompanies(companiesData.data || []);
        console.log('회사 목록:', companiesData.data?.length, '개');
      } catch (err) {
        console.error("초기 데이터 로드 실패:", err);
        if (err.response?.status === 401) {
          navigate('/signin');
        }
      } finally {
        setLoadingSetup(false);
      }
    };
    
    loadInitialData();
  }, [authStatus, navigate]);
  
  /* --------------------------------------------------
     회사 선택 시 포지션 로드
  -------------------------------------------------- */
  const handleCompanyChange = async (companyId) => {
    setSelectedCompany(companyId);
    setSelectedPosition('');
    setPositions([]);
    
    if (companyId) {
      try {
        const { data } = await listPositions(companyId);
        setPositions(data.data || []);
        console.log('포지션 목록:', data.data?.length, '개');
      } catch (err) {
        console.error("포지션 로드 실패:", err);
        if (err.response?.status === 401) {
          navigate('/signin');
        }
      }
    }
  };

  /* --------------------------------------------------
     타이머 관리
  -------------------------------------------------- */
  const startTimer = useCallback(() => {
    if (!isCounting) {
      setIsCounting(true);
      timerRef.current = setInterval(() => {
        setSeconds((s) => s + 1);
        setTotalSec((t) => t + 1);
      }, 1000);
    }
  }, [isCounting]);

  const stopTimer = useCallback(() => {
    setIsCounting(false);
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const resetQuestionTimer = useCallback(() => {
    setSeconds(0);
  }, []);

  /* --------------------------------------------------
     면접 시작 (난이도 포함)
  -------------------------------------------------- */
  const handleStart = async () => {
    if (!selectedPosition) {
      alert("포지션을 선택해주세요.");
      return;
    }
    
    if (!resumeId) {
      alert("이력서를 먼저 작성해주세요.");
      return;
    }
    
    try {
      // 난이도가 포함된 면접 세션 생성
      const createParams = {
        resumeId: resumeId,
        positionId: Number(selectedPosition),
        title: `모의 면접 연습 (난이도 ${selectedDifficulty}단계)`,
        description: `${selectedDifficulty}단계 난이도의 모의 면접 연습`,
        type: "TEXT",
        mode: "PRACTICE",
        useAI: false,
        questionCount: questionCount,
        difficultyLevel: selectedDifficulty, // 사용자가 선택한 난이도
        expectedDurationMinutes: questionCount * 5,
        public: false
      };
      
      console.log('면접 생성 요청:', createParams);
      const { data } = await createInterview(createParams);
      const createdInterview = data.data || data;
      setInterview(createdInterview);
      console.log('생성된 면접:', createdInterview);
      
      // 면접 시작 상태로 변경
      await startInterview(createdInterview.id);
      console.log('면접 시작됨:', createdInterview.id);
      
      // 첫 번째 질문 가져오기
      const { data: questionData } = await getNextQuestion(createdInterview.id);
      const firstQuestion = questionData.data || questionData;
      setQuestion(firstQuestion);
      setCurrentQuestionIndex(1);
      console.log('첫 번째 질문:', firstQuestion);
      
    } catch (err) {
      console.error("면접 시작 실패:", err);
      if (err.response?.status === 401) {
        navigate('/signin');
        return;
      }
      alert("면접을 시작할 수 없습니다: " + (err.response?.data?.message || err.message));
    }
  };

  /* --------------------------------------------------
     녹음 시작
  -------------------------------------------------- */
  const handleStartRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      mediaRef.current = recorder;
      chunks.current = [];

      recorder.ondataavailable = (e) => chunks.current.push(e.data);
      recorder.onstop = () => {
        const blob = new Blob(chunks.current, { type: 'audio/webm' });
        setAudioBlob(blob);
        stream.getTracks().forEach((t) => t.stop());
      };

      recorder.start();
      setIsRecording(true);
      
      // 녹음 시작할 때만 타이머 시작
      resetQuestionTimer();
      startTimer();
    } catch (err) {
      console.error("🎙️ 마이크 권한 에러", err);
      alert("마이크 권한을 허용해주세요.");
    }
  };

  /* --------------------------------------------------
     녹음 종료
  -------------------------------------------------- */
  const handleStopRecording = () => {
    if (mediaRef.current && isRecording) {
      mediaRef.current.stop();
      setIsRecording(false);
      stopTimer();
    }
  };

  /* --------------------------------------------------
     답변 제출
  -------------------------------------------------- */
  const handleSubmitAnswer = async () => {
    if (!audioBlob || !question) return;
    
    setIsSubmitting(true);
    
    try {
      const formData = new FormData();
      formData.append('file', audioBlob, 'answer.webm');
      
      console.log('답변 제출 중:', question.id);
      await uploadAudioAnswer(question.id, formData);
      console.log('답변 제출 완료');
      
      if (currentQuestionIndex >= questionCount) {
        await handleCompleteInterview();
      } else {
        await fetchNextQuestion();
      }
      
    } catch (err) {
      console.error("답변 제출 실패:", err);
      if (err.response?.status === 401) {
        navigate('/signin');
        return;
      }
      alert("답변 제출에 실패했습니다: " + (err.response?.data?.message || err.message));
      setIsSubmitting(false);
    } finally {
      setAudioBlob(null);
    }
  };

  /* --------------------------------------------------
     다음 질문 가져오기
  -------------------------------------------------- */
  const fetchNextQuestion = async () => {
    try {
      console.log('다음 질문 요청:', interview.id);
      const { data } = await getNextQuestion(interview.id);
      const nextQuestion = data.data || data;
      
      if (nextQuestion) {
        setQuestion(nextQuestion);
        setCurrentQuestionIndex(prev => prev + 1);
        resetQuestionTimer();
        setIsSubmitting(false);
        console.log('다음 질문:', nextQuestion);
      }
    } catch (err) {
      console.error("다음 질문 가져오기 오류:", err);
      if (err.response?.status === 410) {
        console.log('더 이상 질문이 없음 - 면접 종료');
        await handleCompleteInterview();
      } else if (err.response?.status === 401) {
        navigate('/signin');
      } else {
        setIsSubmitting(false);
        alert("다음 질문을 가져올 수 없습니다.");
      }
    }
  };

  /* --------------------------------------------------
     면접 종료 처리
  -------------------------------------------------- */
  const handleCompleteInterview = async () => {
    try {
      stopTimer();
      setIsSubmitting(true);
      
      console.log('면접 종료 처리 시작:', interview.id);
      
      await completeInterview(interview.id);
      console.log('면접 완료 상태 변경됨');
      
      await updateInterviewTime(interview.id, { timeInSeconds: totalSec });
      console.log('면접 시간 업데이트됨:', totalSec, '초');
      
      const { data } = await getInterviewQuestions(interview.id);
      const allQuestions = data.data || data;
      setSummary(allQuestions);
      setQuestion(null);
      console.log('면접 결과 요약:', allQuestions);
      
    } catch (err) {
      console.error("면접 종료 처리 실패:", err);
      if (err.response?.status === 401) {
        navigate('/signin');
        return;
      }
      alert("면접 종료 처리 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  /* --------------------------------------------------
     컴포넌트 언마운트 시 정리
  -------------------------------------------------- */
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, []);

  if (!authStatus?.isAuthenticated) {
    return (
      <div className="flex flex-col min-h-screen overflow-hidden">
        <Header />
        <main className="grow flex items-center justify-center">
          <div className="text-center">
            <p className="text-gray-400">로그인이 필요합니다.</p>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />
      
      <main className="grow">
        <section className="relative">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">
              
              {/* 사용자 정보 표시 */}
              <div className="text-center mb-8">
                <p className="text-sm text-gray-400">
                  사용자: {authStatus.userEmail} (ID: {authStatus.userId})
                </p>
              </div>
              
              {/* 면접 시작 전 */}
              {!interview && !summary && (
                <div className="max-w-3xl mx-auto">
                  <h1 className="h2 mb-8 text-center">🎯 모의 면접 연습</h1>
                  
                  {loadingSetup ? (
                    <div className="text-center text-gray-400">설정을 불러오는 중...</div>
                  ) : !hasResume ? (
                    <div className="text-center space-y-4">
                      <p className="text-red-400">이력서를 먼저 작성해주세요.</p>
                      <button
                        onClick={() => window.location.href = '/resume'}
                        className="btn text-white bg-purple-600 hover:bg-purple-700"
                      >
                        이력서 작성하러 가기
                      </button>
                    </div>
                  ) : (
                    <div className="bg-gray-800/50 p-6 rounded-xl space-y-6">
                      {/* 회사 선택 */}
                      <div>
                        <label className="block text-sm font-medium text-gray-300 mb-2">회사 선택</label>
                        <select
                          className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                          value={selectedCompany}
                          onChange={(e) => handleCompanyChange(e.target.value)}
                        >
                          <option value="">회사를 선택하세요</option>
                          {companies.map(company => (
                            <option key={company.id} value={company.id}>
                              {company.name}
                            </option>
                          ))}
                        </select>
                      </div>
                      
                      {/* 포지션 선택 */}
                      <div>
                        <label className="block text-sm font-medium text-gray-300 mb-2">포지션 선택</label>
                        <select
                          className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                          value={selectedPosition}
                          onChange={(e) => setSelectedPosition(e.target.value)}
                          disabled={!selectedCompany}
                        >
                          <option value="">포지션을 선택하세요</option>
                          {positions.map(position => (
                            <option key={position.id} value={position.id}>
                              {position.title || position.name}
                            </option>
                          ))}
                        </select>
                      </div>
                      
                      {/* 난이도 선택 */}
                      <div>
                        <label className="block text-sm font-medium text-gray-300 mb-2">면접 난이도</label>
                        <div className="grid grid-cols-5 gap-2">
                          {[1, 2, 3, 4, 5].map(level => (
                            <button
                              key={level}
                              type="button"
                              onClick={() => setSelectedDifficulty(level)}
                              className={`p-3 rounded-lg text-sm font-medium transition-all ${
                                selectedDifficulty === level
                                  ? 'bg-purple-600 text-white shadow-lg transform scale-105'
                                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                              }`}
                            >
                              <div>{level}단계</div>
                              <div className="text-xs opacity-75">
                                {level === 1 && '기초'}
                                {level === 2 && '초급'}
                                {level === 3 && '중급'}
                                {level === 4 && '고급'}
                                {level === 5 && '전문가'}
                              </div>
                            </button>
                          ))}
                        </div>
                        <p className="text-xs text-gray-500 mt-2">
                          선택한 난이도에 맞는 질문들로 면접이 구성됩니다.
                        </p>
                      </div>
                      
                      {/* 질문 개수 선택 */}
                      <div>
                        <label className="block text-sm font-medium text-gray-300 mb-2">질문 개수</label>
                        <div className="flex gap-4">
                          {[5, 10, 15].map(count => (
                            <label key={count} className="flex items-center">
                              <input
                                type="radio"
                                name="questionCount"
                                value={count}
                                checked={questionCount === count}
                                onChange={(e) => setQuestionCount(Number(e.target.value))}
                                className="mr-2"
                              />
                              <span className="text-gray-300">{count}개</span>
                            </label>
                          ))}
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          예상 소요 시간: {questionCount * 3}~{questionCount * 5}분
                        </p>
                      </div>
                      
                      {/* 시작 버튼 */}
                      <button
                        onClick={handleStart}
                        className="btn w-full text-white bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 py-4 text-lg"
                        disabled={!selectedPosition || !resumeId}
                      >
                        🚀 {selectedDifficulty}단계 면접 시작하기
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* 면접 진행 중 */}
              {interview && question && !summary && (
                <div className="max-w-3xl mx-auto space-y-6">
                  {/* 진행 상황 */}
                  <div className="bg-gray-800/50 p-4 rounded-xl">
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="text-gray-300">
                          질문 {currentQuestionIndex} / {questionCount}
                        </p>
                        <p className="text-sm text-gray-500">난이도 {selectedDifficulty}단계</p>
                      </div>
                      <div className="w-32 bg-gray-700 rounded-full h-2">
                        <div 
                          className="bg-purple-600 h-2 rounded-full transition-all duration-300"
                          style={{ width: `${(currentQuestionIndex / questionCount) * 100}%` }}
                        />
                      </div>
                    </div>
                  </div>
                  
                  {/* 타이머 */}
                  <div className="text-center">
                    <div className="text-4xl font-mono text-white">
                      {String(Math.floor(seconds / 60)).padStart(2, "0")}
                      :
                      {String(seconds % 60).padStart(2, "0")}
                    </div>
                    <p className="text-sm text-gray-500 mt-1">
                      {isCounting ? "답변 중..." : "녹음을 시작하면 타이머가 시작됩니다"}
                    </p>
                  </div>

                  {/* 질문 카드 */}
                  <div className="bg-gray-800/50 rounded-2xl p-6 border-l-4 border-purple-500">
                    <div className="flex justify-between items-start mb-4">
                      <h3 className="font-semibold text-gray-300">
                        질문 {question.sequence || currentQuestionIndex}
                      </h3>
                      <span className="text-xs bg-purple-600 text-white px-2 py-1 rounded">
                        {selectedDifficulty}단계
                      </span>
                    </div>
                    <p className="whitespace-pre-wrap text-lg text-white leading-relaxed">
                      {question.content}
                    </p>
                  </div>

                  {/* 상태 메시지 */}
                  {isSubmitting && (
                    <div className="text-center p-4 bg-blue-900/20 rounded-xl">
                      <p className="text-blue-300 animate-pulse">
                        🤖 답변을 제출하고 있습니다...
                      </p>
                    </div>
                  )}

                  {/* 컨트롤 버튼 */}
                  <div className="flex justify-center gap-4">
                    {!isRecording && !audioBlob && (
                      <button
                        className="btn text-white bg-purple-600 hover:bg-purple-700 flex items-center gap-2 px-8 py-3"
                        onClick={handleStartRecording}
                        disabled={isSubmitting}
                      >
                        🎙️ 녹음 시작
                      </button>
                    )}
                    
                    {isRecording && (
                      <button
                        className="btn text-white bg-red-600 hover:bg-red-700 flex items-center gap-2 px-8 py-3 animate-pulse"
                        onClick={handleStopRecording}
                      >
                        ⏹️ 녹음 종료
                      </button>
                    )}
                    
                    {audioBlob && !isRecording && (
                      <button
                        className="btn text-white bg-green-600 hover:bg-green-700 flex items-center gap-2 px-8 py-3"
                        onClick={handleSubmitAnswer}
                        disabled={isSubmitting}
                      >
                        ✅ 답변 제출
                      </button>
                    )}
                  </div>
                </div>
              )}

              {/* 면접 종료 후 결과 요약 */}
              {summary && (
                <div className="max-w-4xl mx-auto space-y-6">
                  <div className="text-center mb-8">
                    <h2 className="h2 mb-4">🏆 면접 결과 요약</h2>
                    <div className="inline-flex items-center gap-2 bg-purple-600 text-white px-4 py-2 rounded-full">
                      <span>난이도 {selectedDifficulty}단계</span>
                      <span>•</span>
                      <span>{Math.floor(totalSec / 60)}분 {totalSec % 60}초</span>
                    </div>
                  </div>
                  
                  <div className="bg-gray-800/50 p-6 rounded-xl mb-6">
                    <div className="grid grid-cols-3 gap-6 text-center">
                      <div>
                        <p className="text-2xl font-bold text-purple-400">{summary.filter(q => q.answer).length}</p>
                        <p className="text-sm text-gray-400">답변한 질문</p>
                      </div>
                      <div>
                        <p className="text-2xl font-bold text-blue-400">{summary.length}</p>
                        <p className="text-sm text-gray-400">전체 질문</p>
                      </div>
                      <div>
                        <p className="text-2xl font-bold text-green-400">{Math.round((summary.filter(q => q.answer).length / summary.length) * 100)}%</p>
                        <p className="text-sm text-gray-400">완료율</p>
                      </div>
                    </div>
                  </div>
                  
                  {summary.map((q, index) => (
                    <div key={q.id} className="bg-gray-800/50 rounded-xl overflow-hidden">
                      <details className="group">
                        <summary className="cursor-pointer p-4 hover:bg-gray-700/50 transition-colors">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                              <span className="text-xs bg-gray-700 text-gray-300 px-2 py-1 rounded">
                                Q{index + 1}
                              </span>
                              <h3 className="font-medium text-lg text-white">
                                {q.content}
                              </h3>
                            </div>
                            <div className="flex items-center gap-2">
                              {q.answer && (
                                <span className="text-xs bg-green-600 text-white px-2 py-1 rounded">
                                  답변 완료
                                </span>
                              )}
                              <span className="text-sm text-gray-400 group-open:rotate-180 transition-transform">
                                ▼
                              </span>
                            </div>
                          </div>
                        </summary>
                        
                        {q.answer ? (
                          <div className="p-4 border-t border-gray-700 space-y-4">
                            <div>
                              <h4 className="font-semibold text-gray-300 mb-2">내 답변:</h4>
                              <p className="whitespace-pre-wrap bg-gray-700/50 p-3 rounded text-gray-100">
                                {q.answer.content || "음성 답변이 텍스트로 변환 중입니다..."}
                              </p>
                            </div>
                            
                            {q.answer.feedback && (
                              <div>
                                <h4 className="font-semibold text-gray-300 mb-2">AI 피드백:</h4>
                                <p className="whitespace-pre-wrap bg-blue-900/30 p-3 rounded text-blue-200">
                                  {q.answer.feedback}
                                </p>
                              </div>
                            )}
                            
                            {(q.answer.communicationScore || q.answer.technicalScore || q.answer.structureScore) && (
                              <div className="grid grid-cols-3 gap-4 mt-4">
                                {q.answer.communicationScore && (
                                  <div className="text-center bg-blue-900/20 p-3 rounded">
                                    <p className="text-sm text-gray-400">의사소통</p>
                                    <p className="text-2xl font-bold text-blue-400">{q.answer.communicationScore}/10</p>
                                  </div>
                                )}
                                {q.answer.technicalScore && (
                                  <div className="text-center bg-green-900/20 p-3 rounded">
                                    <p className="text-sm text-gray-400">기술적 이해</p>
                                    <p className="text-2xl font-bold text-green-400">{q.answer.technicalScore}/10</p>
                                  </div>
                                )}
                                {q.answer.structureScore && (
                                  <div className="text-center bg-purple-900/20 p-3 rounded">
                                    <p className="text-sm text-gray-400">답변 구조</p>
                                    <p className="text-2xl font-bold text-purple-400">{q.answer.structureScore}/10</p>
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        ) : (
                          <div className="p-4 border-t border-gray-700">
                            <p className="text-gray-500 italic">답변이 제출되지 않았습니다.</p>
                          </div>
                        )}
                      </details>
                    </div>
                  ))}
                  
                  <div className="flex justify-center gap-4 mt-8">
                    <button
                      className="btn text-white bg-purple-600 hover:bg-purple-700"
                      onClick={() => window.location.href = '/'}
                    >
                      홈으로 돌아가기
                    </button>
                    <button
                      className="btn text-white bg-gray-600 hover:bg-gray-700"
                      onClick={() => window.location.reload()}
                    >
                      다시 연습하기
                    </button>
                  </div>
                </div>
              )}
              
            </div>
          </div>
        </section>
      </main>
      
      <Footer />
    </div>
  );
}