// src/pages/Interview.jsx
import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';
import api from '../utils/api';

/* ------------------------------------------------------------------ */
/*  helpers                                                           */
/* ------------------------------------------------------------------ */
const fmtTime = (sec) =>
  `${String(Math.floor(sec / 60)).padStart(2, '0')}:${String(sec % 60).padStart(2, '0')}`;

/* ------------------------------------------------------------------ */
/*  Interview page                                                    */
/* ------------------------------------------------------------------ */
export default function Interview() {
  const navigate = useNavigate();

  /* ───────────────────────────────── 기본 데이터 ────────────────────────────────── */
  const [resumeId, setResumeId]       = useState(null);
  const [companies, setCompanies]     = useState([]);
  const [companyId, setCompanyId]     = useState('');
  const [positions, setPositions]     = useState([]);
  const [positionId, setPositionId]   = useState('');

  /* 설정 */
  const [interviewMode, setInterviewMode] = useState('practice'); // practice | real
  const [questionCount, setQuestionCount] = useState(5);

  /* 인터뷰 런타임 */
  const [interviewId, setInterviewId]   = useState(null);
  const [currentIdx, setCurrentIdx]     = useState(0);
  const [currentQuestion, setCurrentQuestion] = useState(null);
  const [ended, setEnded]               = useState(false);

  /* 타이머 */
  const [elapsedSec, setElapsedSec] = useState(0);
  const timerRef = useRef(null);
  const startTimer = () => {
    clearInterval(timerRef.current);
    timerRef.current = setInterval(() => setElapsedSec((s) => s + 1), 1_000);
  };
  const stopTimer = () => clearInterval(timerRef.current);

  /* ─────────────────────────────── 이력서 / 회사 목록 ─────────────────────────────── */
  useEffect(() => {
    (async () => {
      try {
        const [{ data: resumeRes }, { data: companyRes }] = await Promise.all([
          api.get('/api/resume'),
          api.get('/api/companies'),
        ]);
        if (!resumeRes?.data) throw new Error('no resume');
        setResumeId(resumeRes.data.id);
        setCompanies(companyRes.data);
      } catch {
        alert('먼저 이력서를 등록하세요.');
        navigate('/resume');
      }
    })();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /* 회사 → 포지션 */
  useEffect(() => {
    if (!companyId) return setPositions([]);
    (async () => {
      try {
        const { data } = await api.get(`/api/companies/${companyId}/positions`);
        setPositions(data.data || []);
      } catch (e) {
        console.error(e);
        alert('포지션 목록을 불러오지 못했습니다.');
      }
    })();
  }, [companyId]);

  /* ─────────────────────────────── 인터뷰 세션 생성 ──────────────────────────────── */
  const handleStart = async () => {
    if (!resumeId) return alert('이력서를 찾지 못했습니다.');
    if (!positionId) return alert('포지션을 선택하세요.');

    try {
      /* 기본값 일부는 백엔드가 optional 로 처리하지만, 500 방지를 위해 모두 채운다 */
      const payload = {
        resumeId,
        positionId: Number(positionId),
        title: `Practice Interview - ${new Date().toLocaleDateString()}`,
        description: '',
        type: 'TEXT',
        mode: interviewMode.toUpperCase(), // PRACTICE | REAL
        useAI: true,
        questionCount,
        expectedDurationMinutes: questionCount * 5,
        difficultyLevel: 3,
        categoryFilter: null,
        isPublic: false,
      };

      const { data } = await api.post('/api/interviews', payload);
      const newInterviewId = data.data.id;
      setInterviewId(newInterviewId);

      /* 첫 질문 로드 */
      const { data: qRes } = await api.get(`/api/interviews/${newInterviewId}/next-question`);
      setCurrentQuestion(qRes);
      setCurrentIdx(1);

      startTimer();
    } catch (e) {
      console.error(e);
      alert(e.response?.data?.message || '면접 세션 생성에 실패했습니다.');
    }
  };

  /* ─────────────────────────────── 다음 질문 ──────────────────────────────── */
  const fetchNext = async () => {
    try {
      const { data } = await api.get(`/api/interviews/${interviewId}/next-question`);
      if (data.data) {
        setCurrentQuestion(data.data);
        setCurrentIdx((idx) => idx + 1);
        setElapsedSec(0);
      } else {
        setEnded(true);
        stopTimer();
      }
    } catch (e) {
      if (e.response?.status === 410) {
        setEnded(true);
        stopTimer();
      } else {
        console.error(e);
        alert('다음 질문을 가져오지 못했습니다.');
      }
    }
  };

  /* ───────────────────────────────   UI   ──────────────────────────────── */
  if (!interviewId) {
    /* 1️⃣ 사전 설정 화면 */
    return (
      <div className="flex flex-col min-h-screen">
        <Header />
        <main className="grow">
          <section className="pt-32 pb-20 max-w-xl mx-auto px-4 space-y-8">
            <h1 className="h2">모의 면접 설정</h1>

            {/* 이력서 */}
            <p className="text-sm text-gray-300">
              {resumeId ? `이력서 ID : ${resumeId}` : '이력서를 불러오는 중...'}
            </p>

            {/* 회사 & 포지션 */}
            <div className="space-y-4">
              <select
                className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                value={companyId}
                onChange={(e) => setCompanyId(e.target.value)}
              >
                <option value="">회사 선택</option>
                {companies.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              <select
                className="form-select w-full bg-gray-700/50 border-gray-600 text-white"
                value={positionId}
                onChange={(e) => setPositionId(e.target.value)}
                disabled={!positions.length}
              >
                <option value="">포지션 선택</option>
                {positions.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.title || p.name}
                  </option>
                ))}
              </select>
            </div>

            {/* 옵션 */}
            <div className="grid grid-cols-2 gap-4">
              <select
                className="form-select bg-gray-700/50 border-gray-600 text-white"
                value={interviewMode}
                onChange={(e) => setInterviewMode(e.target.value)}
              >
                <option value="practice">연습</option>
                <option value="real">실전</option>
              </select>
              <select
                className="form-select bg-gray-700/50 border-gray-600 text-white"
                value={questionCount}
                onChange={(e) => setQuestionCount(Number(e.target.value))}
              >
                {[3, 5, 7, 10].map((n) => (
                  <option key={n} value={n}>{`${n} 질문`}</option>
                ))}
              </select>
            </div>

            <button
              className="btn w-full bg-purple-600 hover:bg-purple-700 text-white disabled:opacity-40"
              onClick={handleStart}
              disabled={!resumeId || !positionId}
            >
              면접 시작하기
            </button>
          </section>
        </main>
        <Footer />
      </div>
    );
  }

  /* 2️⃣ 진행 화면 */
  return (
    <div className="flex flex-col min-h-screen">
      <Header />
      <main className="grow">
        <section className="pt-32 pb-20 max-w-3xl mx-auto px-4 space-y-6">
          <div className="text-center text-3xl text-white font-mono">
            {fmtTime(elapsedSec)}
          </div>

          {/* 질문 카드 */}
          <div className="bg-gray-800/50 p-6 rounded-xl min-h-[140px] flex items-center justify-center text-lg text-white">
            {currentQuestion?.content || '질문을 불러오는 중...'}
          </div>

          <button
            className="btn w-full bg-purple-600 hover:bg-purple-700 text-white disabled:opacity-40"
            onClick={fetchNext}
            disabled={ended}
          >
            {ended ? '면접 종료' : '다음 질문'}
          </button>
        </section>
      </main>
      <Footer />
    </div>
  );
}
