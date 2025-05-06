import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';

function Interview() {
  const location = useLocation();
  const navigate = useNavigate();
  const [isRecording, setIsRecording] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [currentQuestion, setCurrentQuestion] = useState(null);
  const [timer, setTimer] = useState(0);
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [interviewMode, setInterviewMode] = useState('practice'); // 'practice' or 'real'
  const [interviewType, setInterviewType] = useState('technical'); // 'technical', 'behavioral', 'general'
  const [questionCount, setQuestionCount] = useState(5);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [interviewStarted, setInterviewStarted] = useState(false);
  const [interviewEnded, setInterviewEnded] = useState(false);
  const [answers, setAnswers] = useState([]);
  const [showHint, setShowHint] = useState(false);
  const timerRef = useRef(null);
  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);

  // 면접 유형별 질문 목록
  const questions = {
    technical: [
      {
        id: 1,
        question: "REST API의 특징과 장단점에 대해 설명해주세요.",
        category: "technical",
        difficulty: "hard",
        hint: "REST의 주요 원칙과 HTTP 메서드의 활용을 중심으로 설명해보세요."
      },
      {
        id: 2,
        question: "마이크로서비스 아키텍처의 장단점은 무엇인가요?",
        category: "technical",
        difficulty: "hard",
        hint: "서비스 분리, 독립적 배포, 확장성 측면에서 설명해보세요."
      },
      {
        id: 3,
        question: "JWT와 Session 기반 인증의 차이점은 무엇인가요?",
        category: "technical",
        difficulty: "hard",
        hint: "상태 유지, 보안, 확장성 측면에서 비교해보세요."
      }
    ],
    behavioral: [
      {
        id: 4,
        question: "팀 프로젝트에서 갈등이 있었을 때 어떻게 해결했나요?",
        category: "behavioral",
        difficulty: "medium",
        hint: "구체적인 상황, 본인의 역할, 해결 과정을 순차적으로 설명해보세요."
      },
      {
        id: 5,
        question: "실패했던 프로젝트 경험이 있다면, 그로부터 배운 점은 무엇인가요?",
        category: "behavioral",
        difficulty: "hard",
        hint: "실패의 원인, 대처 방법, 향후 개선점을 중심으로 설명해보세요."
      },
      {
        id: 6,
        question: "리더십을 발휘했던 경험이 있다면 말씀해주세요.",
        category: "behavioral",
        difficulty: "hard",
        hint: "팀원들의 동기부여, 의사소통, 목표 달성 과정을 설명해보세요."
      }
    ],
    general: [
      {
        id: 7,
        question: "자기소개를 해주세요.",
        category: "general",
        difficulty: "easy",
        hint: "경력, 기술 스택, 강점을 중심으로 간단명료하게 설명해보세요."
      },
      {
        id: 8,
        question: "지원 동기를 말씀해주세요.",
        category: "general",
        difficulty: "medium",
        hint: "회사의 비전, 본인의 경험, 성장 가능성을 연결지어 설명해보세요."
      },
      {
        id: 9,
        question: "앞으로의 커리어 목표는 무엇인가요?",
        category: "general",
        difficulty: "medium",
        hint: "단기/장기 목표와 이를 달성하기 위한 구체적인 계획을 설명해보세요."
      }
    ]
  };

  useEffect(() => {
    // URL에서 질문 ID 가져오기
    const params = new URLSearchParams(location.search);
    const questionId = params.get('question');
    
    if (questionId) {
      // 모든 카테고리에서 질문 찾기
      let foundQuestion = null;
      Object.values(questions).forEach(categoryQuestions => {
        const question = categoryQuestions.find(q => q.id === parseInt(questionId));
        if (question) foundQuestion = question;
      });
      
      if (foundQuestion) {
        setCurrentQuestion(foundQuestion);
        setInterviewStarted(true);
      }
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [location]);

  const startTimer = () => {
    setIsTimerRunning(true);
    timerRef.current = setInterval(() => {
      setTimer(prev => prev + 1);
    }, 1000);
  };

  const stopTimer = () => {
    setIsTimerRunning(false);
    clearInterval(timerRef.current);
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      mediaRecorder.ondataavailable = (event) => {
        audioChunksRef.current.push(event.data);
      };

      mediaRecorder.start();
      setIsRecording(true);
      startTimer();
    } catch (error) {
      console.error('Error accessing microphone:', error);
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      stopTimer();
      analyzeAnswer();
    }
  };

  const analyzeAnswer = () => {
    setIsAnalyzing(true);
    // TODO: 실제 음성 분석 및 피드백 생성 로직 구현
    setTimeout(() => {
      setFeedback({
        strengths: [
          "명확한 구조로 답변을 전개했습니다.",
          "구체적인 예시를 잘 활용했습니다.",
          "자신감 있게 답변했습니다."
        ],
        improvements: [
          "답변 시간을 좀 더 조절해보세요.",
          "기술적 용어를 더 정확하게 사용해보세요.",
          "결론을 더 명확하게 제시해보세요."
        ],
        score: 85
      });
      setIsAnalyzing(false);
    }, 2000);
  };

  const startNewInterview = () => {
    setInterviewStarted(true);
    setInterviewEnded(false);
    setCurrentQuestionIndex(0);
    setAnswers([]);
    setTimer(0);
    setFeedback(null);
    setShowHint(false);
    
    // 선택된 면접 유형의 첫 번째 질문 설정
    const selectedQuestions = questions[interviewType];
    setCurrentQuestion(selectedQuestions[0]);
  };

  const handleNextQuestion = () => {
    const selectedQuestions = questions[interviewType];
    const nextIndex = currentQuestionIndex + 1;
    
    if (nextIndex < questionCount) {
      setCurrentQuestionIndex(nextIndex);
      setCurrentQuestion(selectedQuestions[nextIndex]);
      setFeedback(null);
      setShowHint(false);
      setTimer(0);
    } else {
      setInterviewEnded(true);
    }
  };

  const handlePreviousQuestion = () => {
    if (currentQuestionIndex > 0) {
      const prevIndex = currentQuestionIndex - 1;
      setCurrentQuestionIndex(prevIndex);
      setCurrentQuestion(questions[interviewType][prevIndex]);
      setFeedback(null);
      setShowHint(false);
      setTimer(0);
    }
  };

  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />

      <main className="grow">
        <section className="relative">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">
              <div className="max-w-3xl mx-auto">
                <h1 className="h2 mb-8">모의 면접</h1>

                {!interviewStarted ? (
                  // 면접 시작 전 설정 화면
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <div className="space-y-6">
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          면접 모드
                        </label>
                        <div className="grid grid-cols-2 gap-4">
                          <button
                            className={`btn w-full ${
                              interviewMode === 'practice'
                                ? 'bg-purple-600 text-white'
                                : 'bg-gray-700/50 text-gray-300'
                            }`}
                            onClick={() => setInterviewMode('practice')}
                          >
                            연습 모드
                          </button>
                          <button
                            className={`btn w-full ${
                              interviewMode === 'real'
                                ? 'bg-purple-600 text-white'
                                : 'bg-gray-700/50 text-gray-300'
                            }`}
                            onClick={() => setInterviewMode('real')}
                          >
                            실전 모드
                          </button>
                        </div>
                      </div>

                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          면접 유형
                        </label>
                        <select
                          className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={interviewType}
                          onChange={(e) => setInterviewType(e.target.value)}
                        >
                          <option value="technical">기술 면접</option>
                          <option value="behavioral">행동 면접</option>
                          <option value="general">일반 면접</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          질문 개수
                        </label>
                        <select
                          className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={questionCount}
                          onChange={(e) => setQuestionCount(Number(e.target.value))}
                        >
                          <option value="3">3개</option>
                          <option value="5">5개</option>
                          <option value="7">7개</option>
                        </select>
                      </div>

                      <button
                        className="btn w-full bg-purple-600 text-white hover:bg-purple-700"
                        onClick={startNewInterview}
                      >
                        면접 시작하기
                      </button>
                    </div>
                  </div>
                ) : interviewEnded ? (
                  // 면접 종료 화면
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <h2 className="h3 mb-6 text-center">면접이 종료되었습니다</h2>
                    <div className="space-y-6">
                      <div className="bg-gray-700/50 rounded-lg p-4">
                        <h3 className="text-lg font-semibold mb-4">전체 피드백</h3>
                        <div className="space-y-4">
                          {answers.map((answer, index) => (
                            <div key={index} className="border-b border-gray-600 pb-4">
                              <p className="text-gray-300 mb-2">Q: {answer.question}</p>
                              <p className="text-white mb-2">A: {answer.answer}</p>
                              <p className="text-sm text-gray-400">점수: {answer.score}점</p>
                            </div>
                          ))}
                        </div>
                      </div>
                      <div className="flex space-x-4">
                        <button
                          className="btn flex-1 bg-purple-600 text-white hover:bg-purple-700"
                          onClick={() => navigate('/questions')}
                        >
                          질문 목록으로
                        </button>
                        <button
                          className="btn flex-1 bg-gray-700 text-white hover:bg-gray-600"
                          onClick={startNewInterview}
                        >
                          새로운 면접 시작
                        </button>
                      </div>
                    </div>
                  </div>
                ) : (
                  // 면접 진행 화면
                  <div className="space-y-6">
                    {/* 타이머 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-4 border border-gray-700/50 shadow-lg text-center">
                      <div className="text-3xl font-bold text-white mb-2">
                        {formatTime(timer)}
                      </div>
                      <div className="text-sm text-gray-400">
                        {currentQuestionIndex + 1} / {questionCount} 질문
                      </div>
                    </div>

                    {/* 질문 카드 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <div className="space-y-4">
                        <div className="flex justify-between items-start">
                          <h2 className="text-xl font-semibold text-white">
                            {currentQuestion?.question}
                          </h2>
                          <button
                            className="text-gray-400 hover:text-white"
                            onClick={() => setShowHint(!showHint)}
                          >
                            {showHint ? '힌트 숨기기' : '힌트 보기'}
                          </button>
                        </div>
                        
                        {showHint && (
                          <div className="bg-gray-700/50 rounded-lg p-4">
                            <p className="text-gray-300">{currentQuestion?.hint}</p>
                          </div>
                        )}

                        <div className="flex justify-center space-x-4">
                          {!isRecording ? (
                            <button
                              className="btn bg-purple-600 text-white hover:bg-purple-700"
                              onClick={startRecording}
                            >
                              답변 시작
                            </button>
                          ) : (
                            <button
                              className="btn bg-red-600 text-white hover:bg-red-700"
                              onClick={stopRecording}
                            >
                              답변 종료
                            </button>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* 피드백 */}
                    {feedback && (
                      <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                        <h3 className="text-lg font-semibold text-white mb-4">피드백</h3>
                        <div className="space-y-4">
                          <div>
                            <h4 className="text-sm font-medium text-gray-300 mb-2">장점</h4>
                            <ul className="list-disc list-inside space-y-1">
                              {feedback.strengths.map((strength, index) => (
                                <li key={index} className="text-green-400">{strength}</li>
                              ))}
                            </ul>
                          </div>
                          <div>
                            <h4 className="text-sm font-medium text-gray-300 mb-2">개선점</h4>
                            <ul className="list-disc list-inside space-y-1">
                              {feedback.improvements.map((improvement, index) => (
                                <li key={index} className="text-yellow-400">{improvement}</li>
                              ))}
                            </ul>
                          </div>
                          <div className="text-center">
                            <span className="text-2xl font-bold text-white">
                              {feedback.score}점
                            </span>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* 네비게이션 버튼 */}
                    <div className="flex justify-between">
                      <button
                        className="btn bg-gray-700 text-white hover:bg-gray-600"
                        onClick={handlePreviousQuestion}
                        disabled={currentQuestionIndex === 0}
                      >
                        이전 질문
                      </button>
                      <button
                        className="btn bg-purple-600 text-white hover:bg-purple-700"
                        onClick={handleNextQuestion}
                        disabled={!feedback}
                      >
                        다음 질문
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}

export default Interview; 