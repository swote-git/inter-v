import axios from 'axios';

const API_BASE_URL =
  import.meta.env.VITE_API_URL || 'https://api.interv.swote.dev';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300_000,
  headers: { 'Content-Type': 'application/json' },
});

/* ------------------------------------------------------------------ */
/*  공통 인터셉터 – Access-Token 자동 부착 & 401 → refresh 로직         */
/* ------------------------------------------------------------------ */
api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('accessToken');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (
      err.response?.status === 401 &&
      !err.config._retry &&
      localStorage.getItem('refreshToken')
    ) {
      err.config._retry = true;
      try {
        const { data } = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
          refreshToken: localStorage.getItem('refreshToken'),
        });
        const accessToken = data.data.accessToken;
        localStorage.setItem('accessToken', accessToken);
        err.config.headers.Authorization = `Bearer ${accessToken}`;
        return api(err.config);
      } catch (_) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/signin';
      }
    }
    return Promise.reject(err);
  },
);

/* ------------------------------------------------------------------ */
/*  Interview                                                          */
/* ------------------------------------------------------------------ */
export const createInterview = (payload) =>
  api.post('/api/interviews', payload); // 인증 토큰으로 사용자 식별

export const startInterview = (interviewId) =>
  api.post(`/api/interviews/${interviewId}/start`);

export const getInterviewQuestions = (interviewId) =>
  api.get(`/api/interviews/${interviewId}/questions`);

export const getNextQuestion = (interviewId) =>
  api.get(`/api/interviews/${interviewId}/next-question`);

export const submitAnswer = (questionId, body) =>
  api.post(`/api/interviews/questions/${questionId}/answer`, body);

export const uploadAudioAnswer = (questionId, formData) =>
  api.post(`/api/interviews/questions/${questionId}/answer/audio`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });

export const completeInterview = (interviewId) =>
  api.post(`/api/interviews/${interviewId}/complete`);

export const updateInterviewTime = (interviewId, timeData) =>
  api.post(`/api/interviews/${interviewId}/time`, timeData);

/* ------------------------------------------------------------------ */
/*  Question 검색 & 즐겨찾기                                           */
/* ------------------------------------------------------------------ */
export const searchQuestions = (params) =>
  api.get('/api/interviews/questions/search', { params });

export const toggleFavoriteQuestion = (questionId) =>
  api.post(`/api/interviews/questions/${questionId}/favorite`);

/* ------------------------------------------------------------------ */
/*  Resume & Position (면접 생성 시 필수 ID)                           */
/* ------------------------------------------------------------------ */
export const getMyResume = () => api.get('/api/resume');          // 존재 시 200
export const resumeExists = () => api.get('/api/resume/exists');  // true / false

export const listCompanies = () => api.get('/api/companies');     // 선택지 표시용
export const listPositions = (companyId) =>
  api.get(`/api/companies/${companyId}/positions`);

export default api;