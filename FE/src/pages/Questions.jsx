// src/pages/Questions.jsx
import React, { useState, useEffect } from 'react';
import Header from '../partials/Header';
import Footer from '../partials/Footer';
import axios from 'axios';

/* ───────── 메인 컴포넌트 ───────── */
function Questions() {
  /* ==== 데이터 ==== */
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [questionCount, setQuestionCount] = useState(5);
  const token = localStorage.getItem('token');

  /* ── 저장된 질문 불러오기 ── */
  useEffect(() => {
    const savedQuestions = localStorage.getItem('savedQuestions');
    if (savedQuestions) {
      setQuestions(JSON.parse(savedQuestions));
    }
  }, []);

  /* ── 질문 저장 ── */
  const saveQuestions = (newQuestions) => {
    // 기존 질문이 있으면 새로운 질문으로 교체
    localStorage.setItem('savedQuestions', JSON.stringify(newQuestions));
    setQuestions(newQuestions);
  };

  /* ── 새 질문 생성 ── */
  const generateQuestions = async () => {
    setGenerating(true);
    try {
      /* 1) 내 이력서 정보 가져오기 */
      let myResumeId;
      try {
        const resumeRes = await axios.get('https://api.interv.swote.dev/api/resume', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        if (resumeRes.data.data) {
          myResumeId = resumeRes.data.data.id;
        } else {
          alert('이력서를 먼저 작성해주세요.');
          return;
        }
      } catch (e) {
        console.error('이력서 조회 오류:', e);
        alert('이력서를 불러오는데 실패했습니다.');
        return;
      }

      /* 2) AI를 사용한 질문 생성 */
      const generateQuestionsRes = await axios.get('https://api.interv.swote.dev/api/interviews/questions/generate', {
        params: {
          count: questionCount
        },
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      // 생성된 질문들을 저장하고 화면에 표시
      if (generateQuestionsRes.data.data && generateQuestionsRes.data.data.length > 0) {
        // 새로운 질문으로 교체
        saveQuestions(generateQuestionsRes.data.data);
        alert(`${generateQuestionsRes.data.data.length}개의 면접 질문이 생성되었습니다!`);
      } else {
        alert('질문이 생성되지 않았습니다. 다시 시도해주세요.');
      }
    } catch (e) {
      console.error('질문 생성 오류:', e);
      if (e.response) {
        console.error('에러 응답:', e.response.data);
        alert(`질문 생성에 실패했습니다: ${e.response.data.message || '알 수 없는 오류'}`);
      } else {
        alert('질문 생성에 실패했습니다. 잠시 후 다시 시도해주세요.');
      }
    } finally {
      setGenerating(false);
    }
  };

  /* ── 질문 개수 변경 핸들러 ── */
  const handleQuestionCountChange = (e) => {
    const newCount = Number(e.target.value);
    setQuestionCount(newCount);
    // 질문 개수가 변경되면 기존 질문 목록 초기화
    setQuestions([]);
    localStorage.removeItem('savedQuestions');
  };

  /* ─────── JSX ─────── */
  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />

      <main className="grow">
        <section className="pt-32 pb-20 max-w-5xl mx-auto px-4 space-y-8">
          <div className="flex justify-between items-center">
            <h1 className="h2">예상 질문 리스트</h1>
            <div className="flex gap-4 items-center">
              <div className="relative">
                <select
                  value={questionCount}
                  onChange={handleQuestionCountChange}
                  className="appearance-none bg-gray-700 text-white pl-4 pr-10 py-2 rounded-lg border border-gray-600 focus:outline-none focus:border-purple-500 disabled:opacity-50 disabled:cursor-not-allowed"
                  disabled={generating}
                >
                  {Array.from({ length: 15 }, (_, i) => i + 1).map((num) => (
                    <option key={num} value={num}>
                      {num}개
                    </option>
                  ))}
                </select>
                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-400">
                  <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                    <path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z" />
                  </svg>
                </div>
              </div>
              <button
                className="btn bg-purple-600 hover:bg-purple-700 text-white disabled:opacity-50 disabled:cursor-not-allowed"
                onClick={generateQuestions}
                disabled={generating}
              >
                {generating ? '질문 생성 중...' : '✨ 새 질문 생성'}
              </button>
            </div>
          </div>

          {/* 리스트 */}
          <div className="bg-gray-800/50 p-6 rounded-xl space-y-4 min-h-[200px] relative">
            {generating && (
              <div className="absolute inset-0 bg-gray-900/80 flex items-center justify-center z-10">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mx-auto mb-4"></div>
                  <p className="text-white text-lg">질문이 생성되는 중...</p>
                </div>
              </div>
            )}

            {!generating && questions.length === 0 && (
              <div className="text-center space-y-4 py-12">
                <p className="text-gray-400 text-lg">맞춤 질문을 생성해주세요</p>
                <p className="text-gray-500 text-sm">이력서를 기반으로 AI가 맞춤형 면접 질문을 생성합니다</p>
              </div>
            )}

            {questions.map((q, index) => (
              <div key={index} className="bg-gray-700/50 rounded-xl overflow-hidden">
                <div className="p-4">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <p className="text-white mb-2">{q.content}</p>
                      <div className="flex gap-4 text-sm">
                        <span className="text-gray-400">{q.category}</span>
                        <span className="text-gray-400">난이도 {q.difficultyLevel}</span>
                        {q.subCategory && <span className="text-gray-400">{q.subCategory}</span>}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}

export default Questions;