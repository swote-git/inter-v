// src/pages/Questions.jsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';
import api from '../utils/api';

/* ───────── 필터 옵션 상수 ───────── */
const CATEGORY_OPTS = [
  { id: 'all', label: '전체' },
  { id: '기술면접', label: '기술' },
  { id: '인성면접', label: '인성' },
  { id: '경험면접', label: '경험' },
  { id: '상황면접', label: '상황' },
];
const DIFF_OPTS = [
  { id: 'all', label: '난이도 전체' },
  { id: 1, label: '1단계' },
  { id: 2, label: '2단계' },
  { id: 3, label: '3단계' },
  { id: 4, label: '4단계' },
  { id: 5, label: '5단계' },
];
const TYPE_OPTS = [
  { id: 'all', label: '타입 전체' },
  { id: 'TECHNICAL', label: 'TECHNICAL' },
  { id: 'PERSONALITY', label: 'PERSONALITY' },
  { id: 'PROJECT', label: 'PROJECT' },
  { id: 'SITUATION', label: 'SITUATION' },
];

/* ───────── 메인 컴포넌트 ───────── */
function Questions() {
  const nav = useNavigate();

  /* ==== 필터 상태 ==== */
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState('all');
  const [difficulty, setDifficulty] = useState('all');
  const [qType, setQType] = useState('all');
  const [onlyFav, setOnlyFav] = useState(false);

  /* ==== 데이터 ==== */
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedQuestions, setExpandedQuestions] = useState(new Set());
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userInfo, setUserInfo] = useState(null);

  /* ==== 모달 상태 ==== */
  const [showModal, setShowModal] = useState(false);
  const [companies, setCompanies] = useState([]);
  const [positions, setPositions] = useState([]);
  const [companyId, setCompanyId] = useState('');
  const [positionId, setPositionId] = useState('');
  const [questionCount, setQuestionCount] = useState(5);
  const [selectedDifficulty, setSelectedDifficulty] = useState(3); // 새로 추가
  const [modalBusy, setModalBusy] = useState(false);

  /* ── 간단한 인증 확인 (localStorage 기반) ── */
  useEffect(() => {
    const checkAuth = () => {
      const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
      const userId = localStorage.getItem('userId');
      const userEmail = localStorage.getItem('userEmail');
      
      if (token && userId) {
        setIsAuthenticated(true);
        setUserInfo({
          id: userId,
          email: userEmail,
          token: token
        });
        console.log('인증 확인됨 - 사용자 ID:', userId);
      } else {
        console.log('인증 정보 없음 - 로그인 페이지로 이동');
        nav('/signin');
      }
    };
    
    checkAuth();
  }, [nav]);

  /* ── 질문 검색 ── */
  const fetchQuestions = async () => {
    if (!isAuthenticated) return;
    
    setLoading(true);
    try {
      let questionData = [];

      if (onlyFav) {
        // 즐겨찾기 질문만 가져오기
        try {
          const { data } = await api.get('/api/interviews/questions/favorites');
          questionData = data.data || [];
          console.log('즐겨찾기 질문:', questionData);
        } catch (err) {
          if (err.response?.status === 401) {
            console.log('인증 만료 - 로그인 페이지로 이동');
            localStorage.clear();
            nav('/signin');
            return;
          }
          throw err;
        }
      } else {
        // 일반 질문 검색
        const params = {
          keyword: keyword || undefined,
          category: category !== 'all' ? category : undefined,
          difficultyLevel: difficulty !== 'all' ? difficulty : undefined,
          type: qType !== 'all' ? qType : undefined,
          size: 100,
        };
        
        try {
          const { data } = await api.get('/api/interviews/questions/search', { params });
          questionData = data.data?.content || [];
          console.log('검색된 질문:', questionData);
        } catch (err) {
          if (err.response?.status === 401) {
            console.log('인증 만료 - 로그인 페이지로 이동');
            localStorage.clear();
            nav('/signin');
            return;
          }
          throw err;
        }
      }

      setQuestions(questionData);
    } catch (e) {
      console.error('질문 검색 오류:', e);
      alert('질문을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchQuestions();
    }
  }, [keyword, category, difficulty, qType, onlyFav, isAuthenticated]);

  /* ── 즐겨찾기 토글 ── */
  const toggleFav = async (id) => {
    try {
      await api.post(`/api/interviews/questions/${id}/favorite`);
      setQuestions(prev => prev.map(q => (q.id === id ? { ...q, isFavorite: !q.isFavorite } : q)));
    } catch (e) {
      console.error('즐겨찾기 변경 오류:', e);
      if (e.response?.status === 401) {
        localStorage.clear();
        nav('/signin');
        return;
      }
      alert('즐겨찾기 변경 실패');
    }
  };

  /* ── 답변 토글 ── */
  const toggleAnswer = (id) => {
    setExpandedQuestions(prev => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  /* ── 모달 열릴 때 회사 목록 1회 로드 ── */
  const openModal = async () => {
    setShowModal(true);
    if (companies.length) return;
    try {
      const { data } = await api.get('/api/companies');
      setCompanies(data.data || []);
    } catch (e) {
      console.error('회사 목록 로드 오류:', e);
      if (e.response?.status === 401) {
        localStorage.clear();
        nav('/signin');
        return;
      }
      alert('회사 목록을 가져올 수 없습니다.');
    }
  };

  /* ── 회사 선택 시 포지션 가져오기 ── */
  const handleCompanyChange = async (id) => {
    setCompanyId(id);
    setPositionId('');
    if (!id) { setPositions([]); return; }
    try {
      const { data } = await api.get(`/api/companies/${id}/positions`);
      setPositions(data.data || []);
    } catch (e) {
      console.error('포지션 목록 로드 오류:', e);
      if (e.response?.status === 401) {
        localStorage.clear();
        nav('/signin');
        return;
      }
      alert('포지션 목록을 가져올 수 없습니다.');
    }
  };

  /* ── AI 질문 생성 (난이도 선택 기능 추가) ── */
  const generateQuestions = async () => {
    if (!positionId) return alert('포지션을 선택하세요.');

    setModalBusy(true);
    try {
      /* 1) 내 이력서 정보 가져오기 */
      let myResumeId;
      try {
        const resumeRes = await api.get('/api/resume');
        myResumeId = resumeRes.data.data.id;
        console.log('내 이력서 ID:', myResumeId);
      } catch (e) {
        if (e.response?.status === 404) {
          alert('이력서를 먼저 작성해주세요.');
          setModalBusy(false);
          return;
        } else if (e.response?.status === 401) {
          localStorage.clear();
          nav('/signin');
          return;
        }
        throw e;
      }

      /* 2) 선택된 포지션 정보 구하기 */
      const pos = positions.find(p => p.id === Number(positionId));
      const positionName = pos?.title || pos?.name || '';

      /* 3) 난이도가 포함된 AI 면접 세션 생성 */
      const createInterviewRes = await api.post('/api/interviews', {
        resumeId: myResumeId,
        positionId: Number(positionId),
        title: `AI 생성 질문 - ${positionName} (난이도 ${selectedDifficulty}) - ${new Date().toLocaleDateString()}`,
        description: `${positionName} 포지션을 위한 맞춤형 면접 질문 (난이도 ${selectedDifficulty}단계)`,
        type: "TEXT",
        mode: "PRACTICE",
        useAI: true,
        questionCount: questionCount,
        difficultyLevel: selectedDifficulty, // 사용자가 선택한 난이도 사용
        categoryFilter: category !== 'all' ? category : "기술면접",
        expectedDurationMinutes: questionCount * 5, // 질문당 5분 예상
        public: false
      });
      
      const interviewId = createInterviewRes.data.data?.id || createInterviewRes.data.id;
      console.log('생성된 면접 ID:', interviewId);

      /* 4) 생성된 면접의 질문들 확인 */
      const { data: questionsData } = await api.get(`/api/interviews/${interviewId}/questions`);
      
      if (questionsData.data && questionsData.data.length > 0) {
        alert(`${questionsData.data.length}개의 맞춤형 면접 질문이 생성되었습니다! (난이도 ${selectedDifficulty}단계)`);
        await fetchQuestions();
      } else {
        alert('AI 질문 생성 중... 잠시만 기다려주세요.');
        
        // 선택된 난이도로 기본 질문 검색
        const searchParams = {
          category: category !== 'all' ? category : '기술면접',
          difficultyLevel: selectedDifficulty,
          size: questionCount
        };
        
        const { data: searchData } = await api.get('/api/interviews/questions/search', { 
          params: searchParams 
        });
        
        if (searchData.data?.content && searchData.data.content.length > 0) {
          alert(`${searchData.data.content.length}개의 관련 질문을 찾았습니다! (난이도 ${selectedDifficulty}단계)`);
          await fetchQuestions();
        } else {
          alert('적합한 질문을 찾을 수 없습니다.');
        }
      }
      
      setShowModal(false);
      setCompanyId(''); 
      setPositionId('');
      setQuestionCount(5);
      setSelectedDifficulty(3);
    } catch (e) {
      console.error('질문 생성 오류:', e);
      if (e.response?.status === 401) {
        localStorage.clear();
        nav('/signin');
        return;
      } else if (e.response?.status === 404) {
        alert('이력서를 먼저 작성해주세요.');
      } else {
        alert('질문 생성에 실패했습니다. 잠시 후 다시 시도해주세요.');
      }
    } finally {
      setModalBusy(false);
    }
  };

  /* ── 화면에 보여줄 질문 ── */
  const list = questions;

  if (!isAuthenticated) {
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

  /* ─────── JSX ─────── */
  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />

      <main className="grow">
        <section className="pt-32 pb-20 max-w-5xl mx-auto px-4 space-y-8">
          <div className="flex justify-between items-center">
            <h1 className="h2">예상 질문 리스트</h1>
            <div className="text-sm text-gray-400">
              사용자: {userInfo?.email} (ID: {userInfo?.id})
            </div>
          </div>

          {/* 필터 & 새 질문 생성 버튼 */}
          <div className="bg-gray-800/50 p-6 rounded-xl space-y-4">
            <div className="flex gap-4 items-center">
              <input
                className="form-input flex-1 bg-gray-700/50 border-gray-600 text-white"
                placeholder="키워드 검색"
                value={keyword}
                onChange={e => setKeyword(e.target.value)}
              />
              <button 
                className="btn bg-purple-600 hover:bg-purple-700 text-white whitespace-nowrap"
                onClick={openModal}
              >
                ✨ 새 질문 생성
              </button>
            </div>
            <div className="grid md:grid-cols-4 gap-4">
              <select className="form-select bg-gray-700/50 border-gray-600 text-white"
                value={category} onChange={e => setCategory(e.target.value)}>
                {CATEGORY_OPTS.map(o => <option key={o.id} value={o.id}>{o.label}</option>)}
              </select>
              <select className="form-select bg-gray-700/50 border-gray-600 text-white"
                value={difficulty} onChange={e => setDifficulty(e.target.value)}>
                {DIFF_OPTS.map(o => <option key={o.id} value={o.id}>{o.label}</option>)}
              </select>
              <select className="form-select bg-gray-700/50 border-gray-600 text-white"
                value={qType} onChange={e => setQType(e.target.value)}>
                {TYPE_OPTS.map(o => <option key={o.id} value={o.id}>{o.label}</option>)}
              </select>
              <label className="flex items-center space-x-2 text-gray-300">
                <input type="checkbox" className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500"
                  checked={onlyFav} onChange={e => setOnlyFav(e.target.checked)} />
                <span>즐겨찾기만</span>
              </label>
            </div>
          </div>

          {/* 리스트 */}
          <div className="bg-gray-800/50 p-6 rounded-xl space-y-4 min-h-[200px]">
            {loading && <p className="text-gray-400">불러오는 중...</p>}

            {!loading && list.length === 0 && (
              <div className="text-center space-y-4">
                <p className="text-gray-400">조건에 맞는 질문이 없습니다.</p>
              </div>
            )}

            {list.map(q => (
              <div key={q.id} className="bg-gray-700/50 rounded-xl overflow-hidden">
                <div className="p-4">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <p className="text-white mb-2">{q.content}</p>
                      <div className="flex gap-4 text-sm">
                        <span className="text-gray-400">{q.category}</span>
                        <span className="text-gray-400">난이도 {q.difficultyLevel}</span>
                        <span className="text-gray-400">{q.type}</span>
                        {q.answer && (
                          <button 
                            onClick={() => toggleAnswer(q.id)}
                            className="text-blue-400 hover:text-blue-300"
                          >
                            {expandedQuestions.has(q.id) ? '답변 숨기기 ▲' : '답변 보기 ▼'}
                          </button>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center space-x-2 ml-4">
                      <button onClick={() => toggleFav(q.id)}
                        className={`p-2 rounded-full hover:bg-gray-600 ${q.isFavorite ? 'text-yellow-400' : 'text-gray-400'}`}>
                        ★
                      </button>
                      <button className="btn-sm bg-purple-600 hover:bg-purple-700 text-white"
                        onClick={() => nav(`/interview?question=${q.id}`)}>연습</button>
                    </div>
                  </div>
                  
                  {/* 답변 및 피드백 표시 */}
                  {q.answer && expandedQuestions.has(q.id) && (
                    <div className="mt-4 pt-4 border-t border-gray-600 space-y-3">
                      <div>
                        <h4 className="text-sm font-medium text-gray-400 mb-1">내 답변:</h4>
                        <p className="text-gray-300 bg-gray-800/50 p-3 rounded">
                          {q.answer.content || "답변 내용이 없습니다."}
                        </p>
                      </div>
                      
                      {q.answer.feedback && (
                        <div>
                          <h4 className="text-sm font-medium text-gray-400 mb-1">AI 피드백:</h4>
                          <p className="text-blue-300 bg-blue-900/20 p-3 rounded">
                            {q.answer.feedback}
                          </p>
                        </div>
                      )}
                      
                      {(q.answer.communicationScore || q.answer.technicalScore || q.answer.structureScore) && (
                        <div className="flex gap-4 text-sm">
                          {q.answer.communicationScore && (
                            <span className="text-gray-400">
                              의사소통: <span className="text-white">{q.answer.communicationScore}/10</span>
                            </span>
                          )}
                          {q.answer.technicalScore && (
                            <span className="text-gray-400">
                              기술이해: <span className="text-white">{q.answer.technicalScore}/10</span>
                            </span>
                          )}
                          {q.answer.structureScore && (
                            <span className="text-gray-400">
                              답변구조: <span className="text-white">{q.answer.structureScore}/10</span>
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>

      <Footer />

      {/* ───── AI 질문 생성 모달 (난이도 선택 기능 추가) ───── */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 w-full max-w-lg rounded-xl p-6 space-y-4">
            <h2 className="text-xl text-white">🤖 AI 맞춤형 질문 생성</h2>
            
            <p className="text-sm text-gray-400">
              선택한 포지션과 이력서를 기반으로 맞춤형 면접 질문을 생성합니다.
            </p>

            {/* 회사 선택 */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">회사 선택</label>
              <select className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                value={companyId} onChange={e => handleCompanyChange(e.target.value)}>
                <option value="">회사를 선택하세요</option>
                {companies.map(c => (
                  <option value={c.id} key={c.id}>{c.name}</option>
                ))}
              </select>
            </div>

            {/* 포지션 선택 */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">포지션 선택</label>
              <select className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                value={positionId} onChange={e => setPositionId(e.target.value)} disabled={!companyId}>
                <option value="">포지션을 선택하세요</option>
                {positions.map(p => (
                  <option value={p.id} key={p.id}>{p.title || p.name}</option>
                ))}
              </select>
            </div>

            {/* 난이도 선택 */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">질문 난이도</label>
              <div className="grid grid-cols-5 gap-2">
                {[1, 2, 3, 4, 5].map(level => (
                  <button
                    key={level}
                    type="button"
                    onClick={() => setSelectedDifficulty(level)}
                    className={`p-2 rounded text-sm font-medium transition-colors ${
                      selectedDifficulty === level
                        ? 'bg-purple-600 text-white'
                        : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                    }`}
                  >
                    {level}단계
                  </button>
                ))}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                1단계: 기초 | 2단계: 초급 | 3단계: 중급 | 4단계: 고급 | 5단계: 전문가
              </p>
            </div>

            {/* 질문 개수 */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">질문 개수</label>
              <input
                type="number"
                min="1"
                max="20"
                className="form-input w-full bg-gray-700/50 border-gray-600 text-white"
                value={questionCount}
                onChange={e => setQuestionCount(Number(e.target.value))}
                placeholder="1-20개"
              />
              <p className="text-xs text-gray-500 mt-1">1~20개까지 생성 가능합니다</p>
            </div>

            <div className="flex justify-end space-x-2 pt-4">
              <button 
                className="btn bg-gray-600 text-white" 
                onClick={() => {
                  setShowModal(false);
                  setCompanyId('');
                  setPositionId('');
                  setQuestionCount(5);
                  setSelectedDifficulty(3);
                }} 
                disabled={modalBusy}
              >
                취소
              </button>
              <button 
                className="btn bg-purple-600 hover:bg-purple-700 text-white"
                onClick={generateQuestions} 
                disabled={modalBusy || !positionId}
              >
                {modalBusy ? '생성 중...' : `${selectedDifficulty}단계 질문 생성`}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Questions;