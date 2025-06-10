import React, { useState, useEffect, useRef } from 'react';
import {
  createInterview,
  getInterviewQuestions,
  getNextQuestion,
  submitAnswer,
} from '../utils/api';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';

/* ───── 로그인 여부(= 토큰 존재) 체크 ───── */
const hasToken = () => !!localStorage.getItem('accessToken');

function Interview() {
  const location = useLocation();
  const navigate  = useNavigate();

  /* ───────── 상태 ───────── */
  const [isRecording,  setIsRecording]  = useState(false);
  const [isAnalyzing,  setIsAnalyzing]  = useState(false);
  const [currentQuestion, setCurrentQuestion] = useState(null);
  const [timer,        setTimer]        = useState(0);
  const [feedback,     setFeedback]     = useState(null);

  const [interviewMode, setInterviewMode] = useState('practice');   // practice | real
  const [interviewType, setInterviewType] = useState('technical');  // technical | behavioral | general
  const [questionCount, setQuestionCount] = useState(5);

  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [interviewStarted,     setInterviewStarted]     = useState(false);
  const [interviewEnded,       setInterviewEnded]       = useState(false);
  const [showHint,             setShowHint]             = useState(false);
  const [interviewId,          setInterviewId]          = useState(null);

  /* ───────── ref ───────── */
  const timerRef         = useRef(null);
  const mediaRecorderRef = useRef(null);
  const audioChunksRef   = useRef([]);

  /* ───────── 마운트 시 URL 파라미터 확인 ───────── */
  useEffect(() => {
    const params     = new URLSearchParams(location.search);
    const questionId = params.get('question');
    if (questionId) {
      // 단일 미리보기: 질문 API로 직접 가져오는 방법을 쓰려면 여기서 호출
      // 현재는 인터뷰 전체 흐름에서 받아오기 때문에 생략
    }
    return () => timerRef.current && clearInterval(timerRef.current);
  }, [location]);

  /* ───────── 타이머 ───────── */
  const startTimer = () =>
    (timerRef.current = setInterval(() => setTimer(t => t + 1), 1000));
  const stopTimer  = () => clearInterval(timerRef.current);
  const fmt        = s =>
    `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  /* ───────── 녹음 ───────── */
  const startRec = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const rec    = new MediaRecorder(stream);
      mediaRecorderRef.current = rec;
      audioChunksRef.current   = [];
      rec.ondataavailable      = e => audioChunksRef.current.push(e.data);
      rec.start();
      setIsRecording(true);
      startTimer();
    } catch (e) {
      console.error('mic error', e);
      alert('마이크 사용을 허용해 주세요.');
    }
  };
  const stopRec = () => {
    if (!isRecording) return;
    mediaRecorderRef.current.stop();
    setIsRecording(false);
    stopTimer();
    analyzeAnswer();
  };

  /* ───────── 답변 분석 ───────── */
  const analyzeAnswer = async () => {
    if (!currentQuestion) return;
    setIsAnalyzing(true);
    try {
      const { data } = await submitAnswer(currentQuestion.id, {
        content: 'STT 결과 혹은 입력 텍스트', // TODO: 실제 STT 결과로 교체
      });
      setFeedback({
        strengths:    data.data.strengths,
        improvements: data.data.improvements,
        score:        data.data.score,
      });
    } catch (e) {
      console.error(e);
      alert('답변 전송/평가에 실패했습니다.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  /* ───────── 인터뷰 세션 시작 ───────── */
  const startNewInterview = async () => {
    if (!hasToken()) {
      alert('먼저 로그인 해주세요.');
      return;
    }

    try {
      const payload = {
        // TODO: 필요 시 resumeId, positionId 등 추가
        type:          'TEXT',
        mode:          interviewMode.toUpperCase(),   // PRACTICE | REAL
        questionCount: Number(questionCount),
        useAI:         true,
        questionIds:   [],
        difficultyLevel: 1,
      };

      const { data } = await createInterview(payload);
      const id = data.data.id;
      setInterviewId(id);

      const { data: qRes } = await getInterviewQuestions(id);
      setCurrentQuestion(qRes.data[0]);

      // 로컬 상태 초기화
      setInterviewStarted(true);
      setInterviewEnded(false);
      setCurrentQuestionIndex(0);
      setTimer(0);
      setFeedback(null);
      setShowHint(false);
    } catch (e) {
      console.error(e);
      alert(e.response?.data?.message || '인터뷰 세션 생성 실패');
    }
  };

  /* ───────── 다음 질문 ───────── */
  const nextQuestion = async () => {
    try {
      const { data } = await getNextQuestion(interviewId);
      if (data.data) {
        setCurrentQuestion(data.data);
        setCurrentQuestionIndex(i => i + 1);
        setFeedback(null);
        setShowHint(false);
        setTimer(0);
      } else {
        setInterviewEnded(true);
      }
    } catch (e) {
      console.error(e);
    }
  };

  /* (필요 시) 이전 질문 버튼용 – 서버에 저장해 두지 않았다면 캐싱 로직이 더 필요 */
  const prevQuestion = () => alert('이전 질문 기능은 아직 구현되지 않았습니다.');

  /* ───────── JSX ───────── */
  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />

      <main className="grow">
        <section className="relative">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">
              <div className="max-w-3xl mx-auto">
                <h1 className="h2 mb-8">모의 면접</h1>

                {/* ───────── 설정 화면 ───────── */}
                {!interviewStarted && (
                  <div className="bg-gray-800/50 rounded-xl p-6 space-y-6">
                    {/* 모드 선택 */}
                    <div>
                      <label className="block text-sm font-medium mb-2 text-gray-300">
                        면접 모드
                      </label>
                      <div className="grid grid-cols-2 gap-4">
                        {['practice', 'real'].map(m => (
                          <button
                            key={m}
                            className={`btn w-full ${
                              interviewMode === m
                                ? 'bg-purple-600 text-white'
                                : 'bg-gray-700/50 text-gray-300'
                            }`}
                            onClick={() => setInterviewMode(m)}
                          >
                            {m === 'practice' ? '연습 모드' : '실전 모드'}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* 유형 선택 */}
                    <div>
                      <label className="block text-sm font-medium mb-2 text-gray-300">
                        면접 유형
                      </label>
                      <select
                        className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                        value={interviewType}
                        onChange={e => setInterviewType(e.target.value)}
                      >
                        <option value="technical">기술 면접</option>
                        <option value="behavioral">행동 면접</option>
                        <option value="general">일반 면접</option>
                      </select>
                    </div>

                    {/* 질문 개수 */}
                    <div>
                      <label className="block text-sm font-medium mb-2 text-gray-300">
                        질문 개수
                      </label>
                      <select
                        className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                        value={questionCount}
                        onChange={e => setQuestionCount(Number(e.target.value))}
                      >
                        {[3, 5, 7].map(n => (
                          <option key={n} value={n}>
                            {n}개
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* 시작 버튼 */}
                    <button
                      className="btn w-full bg-purple-600 text-white hover:bg-purple-700"
                      onClick={startNewInterview}
                    >
                      면접 시작하기
                    </button>
                  </div>
                )}

                {/* ───────── 종료 화면 ───────── */}
                {interviewStarted && interviewEnded && (
                  <div className="bg-gray-800/50 rounded-xl p-6 space-y-6">
                    <h2 className="h3 text-center">면접이 종료되었습니다</h2>
                    <button
                      className="btn w-full bg-purple-600 text-white"
                      onClick={() => navigate('/')}
                    >
                      홈으로
                    </button>
                  </div>
                )}

                {/* ───────── 진행 화면 ───────── */}
                {interviewStarted && !interviewEnded && (
                  <div className="space-y-6">
                    {/* 타이머 */}
                    <div className="text-center text-3xl text-white">{fmt(timer)}</div>

                    {/* 질문 카드 */}
                    <div className="bg-gray-800/50 rounded-xl p-6">
                      <div className="flex justify-between">
                        <h2 className="text-xl text-white">
                          {currentQuestion?.question || '질문을 불러오는 중...'}
                        </h2>
                        <button
                          className="text-gray-400"
                          onClick={() => setShowHint(!showHint)}
                        >
                          {showHint ? '힌트 숨기기' : '힌트 보기'}
                        </button>
                      </div>

                      {showHint && currentQuestion?.hint && (
                        <div className="bg-gray-700/50 rounded-lg p-4 mt-4 text-gray-300">
                          {currentQuestion.hint}
                        </div>
                      )}
                    </div>

                    {/* 녹음 버튼 */}
                    <div className="flex justify-center">
                      {!isRecording ? (
                        <button
                          className="btn bg-purple-600 text-white"
                          onClick={startRec}
                          disabled={isAnalyzing}
                        >
                          답변 시작
                        </button>
                      ) : (
                        <button
                          className="btn bg-red-600 text-white"
                          onClick={stopRec}
                        >
                          답변 종료
                        </button>
                      )}
                    </div>

                    {/* 피드백 */}
                    {feedback && (
                      <div className="bg-gray-800/50 rounded-xl p-6 space-y-2">
                        <p className="text-green-400">
                          장점: {feedback.strengths.join(', ')}
                        </p>
                        <p className="text-yellow-400">
                          개선점: {feedback.improvements.join(', ')}
                        </p>
                        <p className="text-white font-bold">점수: {feedback.score}</p>
                      </div>
                    )}

                    {/* 네비게이션 */}
                    <div className="flex justify-between">
                      <button
                        className="btn bg-gray-700 text-white"
                        onClick={prevQuestion}
                        disabled={currentQuestionIndex === 0}
                      >
                        이전 질문
                      </button>
                      <button
                        className="btn bg-purple-600 text-white"
                        onClick={nextQuestion}
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
