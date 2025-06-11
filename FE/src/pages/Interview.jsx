/* =========================================================
   Interview.jsx  (update v4)
   ========================================================= */
import { useCallback, useEffect, useRef, useState } from "react";
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
} from '../utils/api';

export default function Interview() {
  /* --------------------------------------------------
     local state
  -------------------------------------------------- */
  const [interview, setInterview] = useState(null);            // 전체 세션 정보
  const [question, setQuestion]   = useState(null);            // 현재 질문
  const [seconds, setSeconds]     = useState(0);               // 타이머(질문별)
  const [totalSec, setTotalSec]   = useState(0);               // 세션 전체 타임
  const [isCounting, setIsCounting] = useState(false);         // 카운트 활성화 여부
  const [isRecording, setIsRecording] = useState(false);       // MediaRecorder 진행중
  const [audioBlob, setAudioBlob] = useState(null);           // 녹음된 오디오
  const [isSubmitting, setIsSubmitting] = useState(false);     // 답변 제출중
  const [summary, setSummary] = useState(null);                // 마지막 요약뷰용 데이터
  const mediaRef = useRef(null);                               // MediaRecorder 인스턴스
  const chunks   = useRef([]);
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
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);

  /* --------------------------------------------------
     초기 데이터 로드
  -------------------------------------------------- */
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        // 이력서 존재 여부 확인
        const { data: existsData } = await resumeExists();
        setHasResume(existsData.data);
        
        if (existsData.data) {
          // 이력서가 있으면 ID 가져오기
          const { data: resumeData } = await getMyResume();
          setResumeId(resumeData.data.id);
        }
        
        // 회사 목록 가져오기
        const { data: companiesData } = await listCompanies();
        setCompanies(companiesData.data || []);
      } catch (err) {
        console.error("초기 데이터 로드 실패:", err);
      } finally {
        setLoadingSetup(false);
      }
    };
    
    loadInitialData();
  }, []);
  
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
      } catch (err) {
        console.error("포지션 로드 실패:", err);
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
     Interview 시작
  -------------------------------------------------- */
  const handleStart = async () => {
    if (!selectedPosition) {
      alert("포지션을 선택해주세요.");
      return;
    }
    
    try {
      // 면접 세션 생성
      const createParams = {
        resumeId: resumeId,
        positionId: Number(selectedPosition),
        title: "모의 면접 연습",
        type: "TEXT",
        mode: "PRACTICE",
        useAI: false,
        questionCount: questionCount
      };
      
      const { data } = await createInterview(createParams);
      setInterview(data.data);
      
      // 면접 시작 상태로 변경
      await startInterview(data.data.id);
      
      // 첫 번째 질문 가져오기
      const { data: questionData } = await getNextQuestion(data.data.id);
      setQuestion(questionData.data);
      setCurrentQuestionIndex(1);
      
    } catch (err) {
      console.error("면접 시작 실패:", err);
      alert("면접을 시작할 수 없습니다.");
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
      stopTimer(); // 녹음 종료 시 타이머 정지
    }
  };

  /* --------------------------------------------------
     답변 제출
  -------------------------------------------------- */
  const handleSubmitAnswer = async () => {
    if (!audioBlob || !question) return;
    
    setIsSubmitting(true);
    
    try {
      // FormData로 오디오 파일 전송
      const formData = new FormData();
      formData.append('file', audioBlob, 'answer.webm');
      
      await uploadAudioAnswer(question.id, formData);
      
      // 다음 질문으로 이동 또는 면접 종료
      if (currentQuestionIndex >= questionCount) {
        // 마지막 질문이었으면 면접 종료
        await handleCompleteInterview();
      } else {
        // 다음 질문 가져오기
        await fetchNextQuestion();
      }
      
    } catch (err) {
      console.error("답변 제출 실패:", err);
      alert("답변 제출에 실패했습니다.");
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
      const { data } = await getNextQuestion(interview.id);
      
      if (data.data) {
        setQuestion(data.data);
        setCurrentQuestionIndex(prev => prev + 1);
        resetQuestionTimer();
        setIsSubmitting(false);
      }
    } catch (err) {
      if (err.response?.status === 410) {
        // 410 Gone - 더 이상 질문이 없음
        await handleCompleteInterview();
      } else {
        console.error("다음 질문 가져오기 실패:", err);
        setIsSubmitting(false);
      }
    }
  };

  /* --------------------------------------------------
     면접 종료 처리
  -------------------------------------------------- */
  const handleCompleteInterview = async () => {
    try {
      // 타이머 정지
      stopTimer();
      setIsSubmitting(true);
      
      // 면접 상태를 완료로 변경
      await completeInterview(interview.id);
      
      // 총 소요 시간 업데이트
      await updateInterviewTime(interview.id, { timeInSeconds: totalSec });
      
      // 모든 질문과 답변 가져오기
      const { data } = await getInterviewQuestions(interview.id);
      setSummary(data.data);
      setQuestion(null);
      
    } catch (err) {
      console.error("면접 종료 처리 실패:", err);
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

  /* --------------------------------------------------
     JSX 렌더링
  -------------------------------------------------- */
  
  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />
      
      <main className="grow">
        <section className="relative">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">
              
              {/* 면접 시작 전 */}
              {!interview && !summary && (
                <div className="max-w-3xl mx-auto">
                  <h1 className="h2 mb-8 text-center">모의 면접 연습</h1>
                  
                  {loadingSetup ? (
                    <div className="text-center text-gray-400">로딩 중...</div>
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
                      
                      {/* 질문 개수 선택 */}
                      <div>
                        <label className="block text-sm font-medium text-gray-300 mb-2">질문 개수</label>
                        <div className="flex gap-4">
                          <label className="flex items-center">
                            <input
                              type="radio"
                              name="questionCount"
                              value="5"
                              checked={questionCount === 5}
                              onChange={(e) => setQuestionCount(Number(e.target.value))}
                              className="mr-2"
                            />
                            <span className="text-gray-300">5개</span>
                          </label>
                          <label className="flex items-center">
                            <input
                              type="radio"
                              name="questionCount"
                              value="10"
                              checked={questionCount === 10}
                              onChange={(e) => setQuestionCount(Number(e.target.value))}
                              className="mr-2"
                            />
                            <span className="text-gray-300">10개</span>
                          </label>
                          <label className="flex items-center">
                            <input
                              type="radio"
                              name="questionCount"
                              value="15"
                              checked={questionCount === 15}
                              onChange={(e) => setQuestionCount(Number(e.target.value))}
                              className="mr-2"
                            />
                            <span className="text-gray-300">15개</span>
                          </label>
                        </div>
                      </div>
                      
                      {/* 시작 버튼 */}
                      <button
                        onClick={handleStart}
                        className="btn w-full text-white bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600"
                        disabled={!selectedPosition}
                      >
                        ▶️ 면접 시작하기
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* 면접 진행 중 */}
              {interview && question && !summary && (
                <div className="max-w-3xl mx-auto space-y-6">
                  {/* 진행 상황 */}
                  <div className="bg-gray-800/50 p-4 rounded-xl text-center">
                    <p className="text-gray-300">
                      질문 {currentQuestionIndex} / {questionCount}
                    </p>
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
                  <div className="bg-gray-800/50 rounded-2xl p-6">
                    <h3 className="font-semibold mb-4 text-gray-300">
                      질문 {question.sequence || currentQuestionIndex}
                    </h3>
                    <p className="whitespace-pre-wrap text-lg text-white">{question.content}</p>
                  </div>

                  {/* 상태 메시지 */}
                  {isSubmitting && (
                    <p className="text-center text-gray-400 animate-pulse">
                      🤖 답변을 제출하고 있습니다...
                    </p>
                  )}

                  {/* 컨트롤 버튼 */}
                  <div className="flex justify-center gap-4">
                    {!isRecording && !audioBlob && (
                      <button
                        className="btn text-white bg-purple-600 hover:bg-purple-700 flex items-center gap-2"
                        onClick={handleStartRecording}
                        disabled={isSubmitting}
                      >
                        🎙️ 녹음 시작
                      </button>
                    )}
                    
                    {isRecording && (
                      <button
                        className="btn text-white bg-red-600 hover:bg-red-700 flex items-center gap-2"
                        onClick={handleStopRecording}
                      >
                        ⏹️ 녹음 종료
                      </button>
                    )}
                    
                    {audioBlob && !isRecording && (
                      <button
                        className="btn text-white bg-green-600 hover:bg-green-700 flex items-center gap-2"
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
                  <h2 className="h2 mb-6 text-center">면접 결과 요약</h2>
                  
                  <div className="bg-gray-800/50 p-6 rounded-xl mb-6">
                    <p className="text-lg text-gray-300">총 소요 시간: {Math.floor(totalSec / 60)}분 {totalSec % 60}초</p>
                    <p className="text-lg text-gray-300">답변한 질문 수: {summary.filter(q => q.answer).length} / {summary.length}</p>
                  </div>
                  
                  {summary.map((q, index) => (
                    <div key={q.id} className="bg-gray-800/50 rounded-xl overflow-hidden">
                      <details className="group">
                        <summary className="cursor-pointer p-4 hover:bg-gray-700/50 transition-colors">
                          <div className="flex items-center justify-between">
                            <h3 className="font-medium text-lg text-white">
                              Q{index + 1}. {q.content}
                            </h3>
                            <span className="text-sm text-gray-400 group-open:rotate-180 transition-transform">
                              ▼
                            </span>
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
                                  <div className="text-center">
                                    <p className="text-sm text-gray-400">의사소통</p>
                                    <p className="text-2xl font-bold text-blue-400">{q.answer.communicationScore}/10</p>
                                  </div>
                                )}
                                {q.answer.technicalScore && (
                                  <div className="text-center">
                                    <p className="text-sm text-gray-400">기술적 이해</p>
                                    <p className="text-2xl font-bold text-green-400">{q.answer.technicalScore}/10</p>
                                  </div>
                                )}
                                {q.answer.structureScore && (
                                  <div className="text-center">
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
                  
                  <div className="flex justify-center mt-8">
                    <button
                      className="btn text-white bg-purple-600 hover:bg-purple-700"
                      onClick={() => window.location.href = '/'}
                    >
                      홈으로 돌아가기
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