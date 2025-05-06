import React, { useState } from 'react';
import Header from '../partials/Header';
import Footer from '../partials/Footer';
import { useNavigate } from 'react-router-dom';

function Questions() {
  const navigate = useNavigate();
  const [questions, setQuestions] = useState({
    // 기본 정보 관련 질문
    basic: [
      {
        id: 1,
        question: "자기소개를 해주세요.",
        category: "basic",
        difficulty: "easy",
        isFavorite: false
      },
      {
        id: 2,
        question: "지원 동기를 말씀해주세요.",
        category: "basic",
        difficulty: "easy",
        isFavorite: false
      },
      {
        id: 3,
        question: "앞으로의 커리어 목표는 무엇인가요?",
        category: "basic",
        difficulty: "easy",
        isFavorite: false
      },
      {
        id: 4,
        question: "우리 회사에 대해 어떻게 알고 계신가요?",
        category: "basic",
        difficulty: "easy",
        isFavorite: false
      },
      {
        id: 5,
        question: "본인의 강점과 약점은 무엇인가요?",
        category: "basic",
        difficulty: "medium",
        isFavorite: false
      }
    ],
    // 경력 관련 질문
    experience: [
      {
        id: 6,
        question: "이전 직장에서 가장 큰 성과는 무엇이었나요?",
        category: "experience",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 7,
        question: "팀 프로젝트에서 갈등이 있었을 때 어떻게 해결했나요?",
        category: "experience",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 8,
        question: "이전 직장을 그만두게 된 이유는 무엇인가요?",
        category: "experience",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 9,
        question: "리더십을 발휘했던 경험이 있다면 말씀해주세요.",
        category: "experience",
        difficulty: "hard",
        isFavorite: false
      },
      {
        id: 10,
        question: "실패했던 프로젝트 경험이 있다면, 그로부터 배운 점은 무엇인가요?",
        category: "experience",
        difficulty: "hard",
        isFavorite: false
      }
    ],
    // 기술 관련 질문
    technical: [
      {
        id: 11,
        question: "REST API의 특징과 장단점에 대해 설명해주세요.",
        category: "technical",
        difficulty: "hard",
        isFavorite: false
      },
      {
        id: 12,
        question: "마이크로서비스 아키텍처의 장단점은 무엇인가요?",
        category: "technical",
        difficulty: "hard",
        isFavorite: false
      },
      {
        id: 13,
        question: "JWT와 Session 기반 인증의 차이점은 무엇인가요?",
        category: "technical",
        difficulty: "hard",
        isFavorite: false
      },
      {
        id: 14,
        question: "React의 Virtual DOM에 대해 설명해주세요.",
        category: "technical",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 15,
        question: "데이터베이스 인덱싱의 장단점은 무엇인가요?",
        category: "technical",
        difficulty: "hard",
        isFavorite: false
      }
    ],
    // 프로젝트 관련 질문
    project: [
      {
        id: 16,
        question: "가장 기억에 남는 프로젝트는 무엇인가요?",
        category: "project",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 17,
        question: "프로젝트에서 가장 큰 도전과제는 무엇이었나요?",
        category: "project",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 18,
        question: "프로젝트 진행 중 발생한 기술적 문제를 어떻게 해결했나요?",
        category: "project",
        difficulty: "hard",
        isFavorite: false
      },
      {
        id: 19,
        question: "프로젝트 일정이 지연되었을 때 어떻게 대처했나요?",
        category: "project",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 20,
        question: "프로젝트에서 본인의 역할과 기여도는 어땠나요?",
        category: "project",
        difficulty: "medium",
        isFavorite: false
      }
    ],
    // 상황 대처 관련 질문
    situation: [
      {
        id: 21,
        question: "데드라인을 지키지 못할 것 같을 때 어떻게 대처하시나요?",
        category: "situation",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 22,
        question: "팀원과 의견이 충돌할 때 어떻게 해결하시나요?",
        category: "situation",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 23,
        question: "업무 중 예상치 못한 문제가 발생했을 때 어떻게 대처하시나요?",
        category: "situation",
        difficulty: "medium",
        isFavorite: false
      },
      {
        id: 24,
        question: "스트레스 상황에서 어떻게 극복하시나요?",
        category: "situation",
        difficulty: "easy",
        isFavorite: false
      },
      {
        id: 25,
        question: "새로운 기술을 배워야 할 때 어떻게 학습하시나요?",
        category: "situation",
        difficulty: "medium",
        isFavorite: false
      }
    ]
  });

  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedDifficulty, setSelectedDifficulty] = useState('all');
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('id');

  const categories = [
    { id: 'all', label: '전체' },
    { id: 'basic', label: '기본 정보' },
    { id: 'experience', label: '경력' },
    { id: 'technical', label: '기술' },
    { id: 'project', label: '프로젝트' },
    { id: 'situation', label: '상황 대처' }
  ];

  const difficulties = [
    { id: 'all', label: '전체' },
    { id: 'easy', label: '쉬움' },
    { id: 'medium', label: '보통' },
    { id: 'hard', label: '어려움' }
  ];

  const sortOptions = [
    { id: 'id', label: '기본 순서' },
    { id: 'difficulty', label: '난이도 순' },
    { id: 'category', label: '카테고리 순' }
  ];

  const toggleFavorite = (questionId) => {
    setQuestions(prev => {
      const newQuestions = { ...prev };
      Object.keys(newQuestions).forEach(category => {
        newQuestions[category] = newQuestions[category].map(q => 
          q.id === questionId ? { ...q, isFavorite: !q.isFavorite } : q
        );
      });
      return newQuestions;
    });
  };

  const getFilteredQuestions = () => {
    let filtered = [];
    Object.values(questions).forEach(categoryQuestions => {
      filtered = [...filtered, ...categoryQuestions];
    });

    // 검색어 필터링
    if (searchQuery) {
      filtered = filtered.filter(q => 
        q.question.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    // 카테고리 필터링
    if (selectedCategory !== 'all') {
      filtered = filtered.filter(q => q.category === selectedCategory);
    }

    // 난이도 필터링
    if (selectedDifficulty !== 'all') {
      filtered = filtered.filter(q => q.difficulty === selectedDifficulty);
    }

    // 즐겨찾기 필터링
    if (showFavoritesOnly) {
      filtered = filtered.filter(q => q.isFavorite);
    }

    // 정렬
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'difficulty':
          const difficultyOrder = { easy: 1, medium: 2, hard: 3 };
          return difficultyOrder[a.difficulty] - difficultyOrder[b.difficulty];
        case 'category':
          return a.category.localeCompare(b.category);
        default:
          return a.id - b.id;
      }
    });

    return filtered;
  };

  const getDifficultyColor = (difficulty) => {
    switch (difficulty) {
      case 'easy':
        return 'text-green-400';
      case 'medium':
        return 'text-yellow-400';
      case 'hard':
        return 'text-red-400';
      default:
        return 'text-gray-400';
    }
  };

  const handlePractice = (questionId) => {
    navigate(`/interview?question=${questionId}`);
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

                {/* 검색 및 필터 섹션 */}
                <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg mb-8">
                  <div className="space-y-4">
                    {/* 검색창 */}
                    <div>
                      <label className="block text-sm font-medium mb-2 text-gray-300">
                        질문 검색
                      </label>
                      <input
                        type="text"
                        className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                        placeholder="검색어를 입력하세요"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                      />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          카테고리
                        </label>
                        <select
                          className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={selectedCategory}
                          onChange={(e) => setSelectedCategory(e.target.value)}
                        >
                          {categories.map(category => (
                            <option key={category.id} value={category.id}>
                              {category.label}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          난이도
                        </label>
                        <select
                          className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={selectedDifficulty}
                          onChange={(e) => setSelectedDifficulty(e.target.value)}
                        >
                          {difficulties.map(difficulty => (
                            <option key={difficulty.id} value={difficulty.id}>
                              {difficulty.label}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          정렬
                        </label>
                        <select
                          className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={sortBy}
                          onChange={(e) => setSortBy(e.target.value)}
                        >
                          {sortOptions.map(option => (
                            <option key={option.id} value={option.id}>
                              {option.label}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="flex items-end">
                        <label className="flex items-center space-x-2 text-gray-300">
                          <input
                            type="checkbox"
                            className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                            checked={showFavoritesOnly}
                            onChange={(e) => setShowFavoritesOnly(e.target.checked)}
                          />
                          <span>즐겨찾기만 보기</span>
                        </label>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 질문 리스트 */}
                <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                  <div className="space-y-4">
                    {getFilteredQuestions().map(question => (
                      <div
                        key={question.id}
                        className="bg-gray-700/50 rounded-lg p-4 border border-gray-600"
                      >
                        <div className="flex justify-between items-start">
                          <div className="flex-1">
                            <p className="text-white mb-2">{question.question}</p>
                            <div className="flex items-center space-x-4">
                              <span className={`text-sm ${getDifficultyColor(question.difficulty)}`}>
                                {difficulties.find(d => d.id === question.difficulty)?.label}
                              </span>
                              <span className="text-sm text-gray-400">
                                {categories.find(c => c.id === question.category)?.label}
                              </span>
                            </div>
                          </div>
                          <div className="flex items-center space-x-2">
                            <button
                              onClick={() => handlePractice(question.id)}
                              className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                            >
                              답변 연습
                            </button>
                            <button
                              onClick={() => toggleFavorite(question.id)}
                              className={`p-2 rounded-full hover:bg-gray-600 transition-colors ${
                                question.isFavorite ? 'text-yellow-400' : 'text-gray-400'
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
                  </div>
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