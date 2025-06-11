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
  { id: 1, label: '1' },
  { id: 2, label: '2' },
  { id: 3, label: '3' },
  { id: 4, label: '4' },
  { id: 5, label: '5' },
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
  const [keyword, setKeyword]       = useState('');
  const [category, setCategory]     = useState('all');
  const [difficulty, setDifficulty] = useState('all');
  const [qType, setQType]           = useState('all');
  const [onlyFav, setOnlyFav]       = useState(false);

  /* ==== 데이터 ==== */
  const [questions, setQuestions]   = useState([]);
  const [loading, setLoading]       = useState(true);

  /* ==== 모달 상태 ==== */
  const [showModal, setShowModal]           = useState(false);
  const [companies, setCompanies]           = useState([]);
  const [positions, setPositions]           = useState([]);
  const [companyId, setCompanyId]           = useState('');
  const [positionId, setPositionId]         = useState('');
  const [interviewId, setInterviewId]       = useState('');
  const [questionCount, setQuestionCount]   = useState(5);
  const [modalBusy, setModalBusy]           = useState(false);

  /* ── 질문 검색 ── */
  const fetchQuestions = async () => {
    setLoading(true);
    try {
      const params = {
        keyword: keyword || undefined,
        category: category !== 'all' ? category : undefined,
        difficultyLevel: difficulty !== 'all' ? difficulty : undefined,
        type: qType !== 'all' ? qType : undefined,
        size: 100,
      };
      const { data } = await api.get('/api/interviews/questions/search', { params });
      setQuestions(data.data?.content || []);
    } catch (e) {
      console.error(e);
      alert('질문을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { fetchQuestions(); }, [keyword, category, difficulty, qType]); // eslint-disable-line

  /* ── 즐겨찾기 ── */
  const toggleFav = async (id) => {
    try {
      await api.post(`/api/interviews/questions/${id}/favorite`);
      setQuestions(prev => prev.map(q => (q.id === id ? { ...q, isFavorite: !q.isFavorite } : q)));
    } catch (e) {
      console.error(e);
      alert('즐겨찾기 변경 실패');
    }
  };

  /* ── 모달 열릴 때 회사 목록 1회 로드 ── */
  const openModal = async () => {
    setShowModal(true);
    if (companies.length) return;
    try {
      const { data } = await api.get('/api/companies');
      setCompanies(data.data || []);
    } catch (e) {
      console.error(e);
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
      console.error(e);
      alert('포지션 목록을 가져올 수 없습니다.');
    }
  };

  /* ── AI 질문 생성 ── */
  const generateQuestions = async () => {
    if (!interviewId) return alert('인터뷰 ID를 입력하세요.');
    if (!positionId)   return alert('포지션을 선택하세요.');

    setModalBusy(true);
    try {
      /* 1) 내 이력서 내용 */
      const r = await api.get('/api/resume');
      const resumeContent = r.data.data.content || '';

      /* 2) 선택된 포지션 이름 구하기 */
      const pos = positions.find(p => p.id === Number(positionId));
      const positionName = pos?.title || pos?.name || '';

      /* 3) API 호출 */
      const { data } = await api.post(
        `/api/interviews/${interviewId}/questions/generate`,
        {
          resumeContent,
          position: positionName,
          questionCount: Math.min(Math.max(questionCount, 1), 20),
        }
      );
      /* 4) 리스트 추가 & 모달 닫기 */
      setQuestions(prev => [...data.data, ...prev]);
      alert(`${data.data.length}개 질문 생성 완료`);
      setShowModal(false);
      setCompanyId(''); setPositionId('');
    } catch (e) {
      console.error(e);
      alert('AI 질문 생성 실패');
    } finally {
      setModalBusy(false);
    }
  };

  /* ── 화면에 보여줄 질문 after fav 필터 ── */
  const list = questions.filter(q => (onlyFav ? q.isFavorite : true));

  /* ─────── JSX ─────── */
  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />

      <main className="grow">
        <section className="pt-32 pb-20 max-w-5xl mx-auto px-4 space-y-8">
          <h1 className="h2">예상 질문 리스트</h1>

          {/* 필터 */}
          <div className="bg-gray-800/50 p-6 rounded-xl space-y-4">
            <input
              className="form-input w-full bg-gray-700/50 border-gray-600 text-white"
              placeholder="키워드 검색"
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
            />
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
                <button className="btn bg-purple-600 hover:bg-purple-700 text-white"
                  onClick={openModal}>AI 질문 생성하기</button>
              </div>
            )}

            {list.map(q => (
              <div key={q.id} className="bg-gray-700/50 p-4 rounded-xl flex justify-between">
                <div>
                  <p className="text-white mb-1">{q.content}</p>
                  <span className="text-sm text-gray-400 mr-4">{q.category}</span>
                  <span className="text-sm text-gray-400">난이도 {q.difficultyLevel}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <button onClick={() => toggleFav(q.id)}
                    className={`p-2 rounded-full hover:bg-gray-600 ${q.isFavorite ? 'text-yellow-400' : 'text-gray-400'}`}>
                    ★
                  </button>
                  <button className="btn-sm bg-purple-600 hover:bg-purple-700 text-white"
                    onClick={() => nav(`/interview?question=${q.id}`)}>연습</button>
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>

      <Footer />

      {/* ───── AI 질문 생성 모달 ───── */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-gray-800 w-full max-w-lg rounded-xl p-6 space-y-4">
            <h2 className="text-xl text-white">AI 질문 생성</h2>

            <input
              className="form-input w-full bg-gray-700/50 border-gray-600 text-white"
              placeholder="인터뷰 ID"
              value={interviewId}
              onChange={e => setInterviewId(e.target.value)}
            />

            {/* 회사 선택 */}
            <select className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
              value={companyId} onChange={e => handleCompanyChange(e.target.value)}>
              <option value="">회사 선택</option>
              {companies.map(c => (
                <option value={c.id} key={c.id}>{c.name}</option>
              ))}
            </select>

            {/* 포지션 선택 */}
            <select className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
              value={positionId} onChange={e => setPositionId(e.target.value)} disabled={!companyId}>
              <option value="">포지션 선택</option>
              {positions.map(p => (
                <option value={p.id} key={p.id}>{p.title || p.name}</option>
              ))}
            </select>

            {/* 질문 개수 */}
            <input
              type="number"
              min="1"
              max="20"
              className="form-input w-full bg-gray-700/50 border-gray-600 text-white"
              value={questionCount}
              onChange={e => setQuestionCount(e.target.value)}
              placeholder="질문 개수 (1-20)"
            />

            <div className="flex justify-end space-x-2">
              <button className="btn bg-gray-600 text-white" onClick={() => setShowModal(false)} disabled={modalBusy}>
                취소
              </button>
              <button className="btn bg-purple-600 hover:bg-purple-700 text-white"
                onClick={generateQuestions} disabled={modalBusy}>
                {modalBusy ? '생성 중...' : '생성'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Questions;
