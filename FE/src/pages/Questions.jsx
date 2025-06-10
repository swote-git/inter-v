import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';
import api from '../utils/api';

const categories = [
  { id: 'ALL', label: '전체' },
  { id: 'PERSONALITY', label: '기본 정보' },
  { id: 'EXPERIENCE',  label: '경력' },
  { id: 'TECHNICAL',   label: '기술' },
  { id: 'PROJECT',     label: '프로젝트' },
  { id: 'SITUATION',   label: '상황 대처' },
];

const difficulties = [
  { id: 'ALL', label: '전체' },
  { id: 1,     label: '쉬움' },
  { id: 3,     label: '보통' },
  { id: 5,     label: '어려움' },
];

function Questions() {
  const navigate = useNavigate();

  /* ───────── 상태 ───────── */
  const [list,              setList]              = useState([]);
  const [loading,           setLoading]           = useState(false);
  const [selectedCategory,  setSelectedCategory]  = useState('ALL');
  const [selectedDiff,      setSelectedDiff]      = useState('ALL');
  const [searchQuery,       setSearchQuery]       = useState('');
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false);
  const [favorites,         setFavorites]         = useState(new Set());

  /* ───────── 데이터 로드 ───────── */
  const fetchQuestions = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/api/interviews/questions/search', {
        params: {
          page: 0,
          size: 100,
          keyword: searchQuery || undefined,
          category:
            selectedCategory === 'ALL' ? undefined : selectedCategory,
          difficultyLevel:
            selectedDiff === 'ALL' ? undefined : selectedDiff,
        },
      });
      setList(data.data.content);
    } catch (e) {
      console.error(e);
      alert('질문을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  /* 첫 로드 & 필터 변경 시 재검색 */
  useEffect(() => {
    fetchQuestions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCategory, selectedDiff]);

  /* ───────── 로컬 즐겨찾기 ───────── */
  const toggleFavorite = id =>
    setFavorites(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  /* ───────── 필터링(검색·즐겨찾기) ───────── */
  const filtered = list
    .filter(q =>
      searchQuery
        ? q.content.toLowerCase().includes(searchQuery.toLowerCase())
        : true,
    )
    .filter(q => (showFavoritesOnly ? favorites.has(q.id) : true));

  /* ───────── 난이도 컬러 헬퍼 ───────── */
  const diffColor = lvl => {
    if (lvl <= 2) return 'text-green-400';
    if (lvl <= 4) return 'text-yellow-400';
    return 'text-red-400';
  };

  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />

      <main className="grow">
        <section className="relative">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">
              <div className="max-w-3xl mx-auto">
                <h1 className="h2 mb-8">예상 질문 리스트</h1>

                {/* ───────── 검색/필터 ───────── */}
                <div className="bg-gray-800/50 rounded-xl p-6 mb-8">
                  <div className="space-y-4">
                    {/* 검색창 */}
                    <div>
                      <label className="block text-sm mb-2 text-gray-300">
                        질문 검색
                      </label>
                      <input
                        type="text"
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                        className="form-input w-full bg-gray-700/50 border-gray-600 text-white"
                        placeholder="키워드를 입력하세요"
                      />
                    </div>

                    {/* 필터 셀렉트 */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <select
                        className="form-select bg-gray-700/50 border-gray-600 text-white"
                        value={selectedCategory}
                        onChange={e => setSelectedCategory(e.target.value)}
                      >
                        {categories.map(c => (
                          <option key={c.id} value={c.id}>
                            {c.label}
                          </option>
                        ))}
                      </select>

                      <select
                        className="form-select bg-gray-700/50 border-gray-600 text-white"
                        value={selectedDiff}
                        onChange={e => setSelectedDiff(e.target.value)}
                      >
                        {difficulties.map(d => (
                          <option key={d.id} value={d.id}>
                            {d.label}
                          </option>
                        ))}
                      </select>

                      <label className="flex items-center text-gray-300 space-x-2">
                        <input
                          type="checkbox"
                          className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500"
                          checked={showFavoritesOnly}
                          onChange={e => setShowFavoritesOnly(e.target.checked)}
                        />
                        <span>즐겨찾기만 보기</span>
                      </label>
                    </div>
                  </div>
                </div>

                {/* ───────── 질문 카드 ───────── */}
                <div className="bg-gray-800/50 rounded-xl p-6">
                  {loading ? (
                    <p className="text-center">로딩 중...</p>
                  ) : (
                    <div className="space-y-4">
                      {filtered.map(q => (
                        <div
                          key={q.id}
                          className="bg-gray-700/50 rounded-lg p-4 border border-gray-600"
                        >
                          <div className="flex justify-between">
                            <div>
                              <p className="text-white mb-1">{q.content}</p>
                              <div className="text-sm flex space-x-4">
                                <span className={diffColor(q.difficultyLevel)}>
                                  난이도&nbsp;{q.difficultyLevel}
                                </span>
                                <span className="text-gray-400">
                                  {
                                    categories.find(
                                      c => c.id === q.category,
                                    )?.label
                                  }
                                </span>
                              </div>
                            </div>

                            <div className="flex items-center space-x-2">
                              <button
                                className="btn-sm bg-purple-600 text-white"
                                onClick={() =>
                                  navigate(`/interview?question=${q.id}`)
                                }
                              >
                                연습
                              </button>
                              <button
                                onClick={() => toggleFavorite(q.id)}
                                className={`p-2 rounded-full hover:bg-gray-600 ${
                                  favorites.has(q.id)
                                    ? 'text-yellow-400'
                                    : 'text-gray-400'
                                }`}
                              >
                                <svg
                                  className="w-5 h-5"
                                  fill="currentColor"
                                  viewBox="0 0 20 20"
                                >
                                  <path
                                    fillRule="evenodd"
                                    d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"
                                    clipRule="evenodd"
                                  />
                                </svg>
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}

                      {!loading && filtered.length === 0 && (
                        <p className="text-center text-gray-400">
                          조건에 맞는 질문이 없습니다.
                        </p>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}

export default Questions;
