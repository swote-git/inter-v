import axios from 'axios';

const API_BASE_URL =
  import.meta.env.VITE_API_URL || 'https://api.interv.swote.dev';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300_000,
  headers: { 'Content-Type': 'application/json' },
});

/* ------------------------------------------------------------------ */
/*  향상된 인증 인터셉터                                               */
/* ------------------------------------------------------------------ */
api.interceptors.request.use((cfg) => {
  // 토큰 확인 및 자동 부착
  const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
  if (token) {
    cfg.headers.Authorization = `Bearer ${token}`;
    console.log('API 요청에 토큰 부착:', cfg.url);
  } else {
    console.warn('토큰이 없습니다:', cfg.url);
  }
  return cfg;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const originalRequest = err.config;
    
    // 401 오류 처리
    if (err.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      const refreshToken = localStorage.getItem('refreshToken');
      
      if (refreshToken) {
        try {
          console.log('토큰 갱신 시도...');
          const { data } = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
            refreshToken: refreshToken,
          });
          
          const newAccessToken = data.data.accessToken;
          localStorage.setItem('accessToken', newAccessToken);
          localStorage.setItem('token', newAccessToken); // 호환성
          
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          console.log('토큰 갱신 성공');
          return api(originalRequest);
        } catch (refreshError) {
          console.error('토큰 갱신 실패:', refreshError);
        }
      }
      
      // 토큰 갱신 실패 또는 리프레시 토큰 없음
      console.log('인증 실패 - 로그인 필요');
      localStorage.clear();
      window.location.href = '/signin';
    }
    
    return Promise.reject(err);
  },
);

/* ------------------------------------------------------------------ */
/*  인증 상태 확인 유틸리티                                             */
/* ------------------------------------------------------------------ */
export const checkAuthStatus = () => {
  const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
  const userId = localStorage.getItem('userId');
  
  return {
    isAuthenticated: !!(token && userId),
    token,
    userId,
    userEmail: localStorage.getItem('userEmail')
  };
};

export const clearAuthData = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userId');
  localStorage.removeItem('userEmail');
};

/* ------------------------------------------------------------------ */
/*  Interview API                                                     */
/* ------------------------------------------------------------------ */
export const createInterview = (payload) => {
  console.log('Creating interview with payload:', payload);
  return api.post('/api/interviews', payload);
};

export const startInterview = (interviewId) => {
  console.log('Starting interview:', interviewId);
  return api.post(`/api/interviews/${interviewId}/start`);
};

export const getInterviewQuestions = (interviewId) => {
  console.log('Getting interview questions for:', interviewId);
  return api.get(`/api/interviews/${interviewId}/questions`);
};

export const getNextQuestion = (interviewId) => {
  console.log('Getting next question for interview:', interviewId);
  return api.get(`/api/interviews/${interviewId}/next-question`);
};

export const submitAnswer = (questionId, body) => {
  console.log('Submitting answer for question:', questionId, body);
  return api.post(`/api/interviews/questions/${questionId}/answer`, body);
};

export const uploadAudioAnswer = (questionId, formData) => {
  console.log('Uploading audio answer for question:', questionId);
  return api.post(`/api/interviews/questions/${questionId}/answer/audio`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const completeInterview = (interviewId) => {
  console.log('Completing interview:', interviewId);
  return api.post(`/api/interviews/${interviewId}/complete`);
};

export const updateInterviewTime = (interviewId, timeData) => {
  console.log('Updating interview time:', interviewId, timeData);
  return api.post(`/api/interviews/${interviewId}/time`, timeData);
};

/* ------------------------------------------------------------------ */
/*  Question 검색 & 즐겨찾기 (인증 필수)                               */
/* ------------------------------------------------------------------ */
export const searchQuestions = (params) => {
  console.log('Searching questions with params:', params);
  const authStatus = checkAuthStatus();
  if (!authStatus.isAuthenticated) {
    throw new Error('Authentication required');
  }
  return api.get('/api/interviews/questions/search', { params });
};

export const toggleFavoriteQuestion = (questionId) => {
  console.log('Toggling favorite for question:', questionId);
  const authStatus = checkAuthStatus();
  if (!authStatus.isAuthenticated) {
    throw new Error('Authentication required');
  }
  return api.post(`/api/interviews/questions/${questionId}/favorite`);
};

export const getFavoriteQuestions = () => {
  console.log('Getting favorite questions');
  const authStatus = checkAuthStatus();
  if (!authStatus.isAuthenticated) {
    throw new Error('Authentication required');
  }
  return api.get('/api/interviews/questions/favorites');
};

/* ------------------------------------------------------------------ */
/*  Resume & Position                                                 */
/* ------------------------------------------------------------------ */
export const getMyResume = () => {
  console.log('Getting my resume');
  return api.get('/api/resume');
};

export const resumeExists = () => {
  console.log('Checking if resume exists');
  return api.get('/api/resume/exists');
};

export const listCompanies = () => {
  console.log('Getting companies list');
  return api.get('/api/companies');
};

export const listPositions = (companyId) => {
  console.log('Getting positions for company:', companyId);
  return api.get(`/api/companies/${companyId}/positions`);
};

/* ------------------------------------------------------------------ */
/*  Auth 관련 (대체 방법)                                              */
/* ------------------------------------------------------------------ */
export const getCurrentUser = () => {
  console.log('Getting current user via localStorage');
  const authStatus = checkAuthStatus();
  
  if (!authStatus.isAuthenticated) {
    throw new Error('User not authenticated');
  }
  
  // localStorage 기반으로 사용자 정보 반환
  return Promise.resolve({
    data: {
      data: {
        id: authStatus.userId,
        email: authStatus.userEmail,
        isAuthenticated: true
      }
    }
  });
};

// 실제 서버 API 호출 (작동하는 경우)
export const getCurrentUserFromAPI = () => {
  console.log('Getting current user from API');
  return api.get('/api/auth/me');
};

export default api;