import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';

import Header from '../partials/Header';
import PageIllustration from '../partials/PageIllustration';
import Banner from '../partials/Banner';

function SignUp() {
  const [formData, setFormData] = useState({
    userName: '',
    nickname: '',
    email: '',
    password: '',
    phoneNumber: '',
    birthDate: ''
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    console.log('전송할 데이터:', formData);

    try {
      console.log('API 요청 시작...');
      const response = await axios.post(
        'https://api.interv.swote.dev/api/users/register',
        formData,
        {
          headers: {
            'Content-Type': 'application/json',
          },

        }
      );

      console.log('응답 받음:', response.status, response.statusText);
      console.log('응답 데이터:', response.data);

      if (response.status === 200) {
        alert('회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.');
        setTimeout(() => {
          window.location.href = '/signin';
        }, 2000);
      } else {
        const errorMessage = response.data?.message || `서버 오류: ${response.status}`;
        setError(errorMessage);
        console.error('회원가입 실패:', errorMessage);
      }
    } catch (err) {
      const errorMessage = `네트워크 오류: ${err.message}`;
      setError(errorMessage);
      console.error('Registration error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      {/*  Site header */}
      <Header />

      {/*  Page content */}
      <main className="grow">
        {/*  Page illustration */}
        <div className="relative max-w-6xl mx-auto h-0 pointer-events-none" aria-hidden="true">
          <PageIllustration />
        </div>

        <section className="relative">
          <div className="w-[38%] mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">
              {/* Page header */}
              <div className="max-w-3xl mx-auto text-left pb-12 md:pb-20">
                <h1 className="h2">반가워요! <br /> INTERV를 시작해볼까요?</h1>
              </div>

              {/* Form */}
              <div className="[30%] mx-auto">
                {/* Error Message */}
                {error && (
                  <div className="mb-4 p-3 bg-red-600/10 border border-red-600/20 rounded-lg">
                    <p className="text-red-400 text-sm">{error}</p>
                  </div>
                )}

                <form onSubmit={handleSubmit}>
                  {/* 이름 (userName) */}
                  <div className="flex flex-wrap -mx-3 mb-4">
                    <div className="w-full px-3">
                      <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="userName">
                        이름<span className="text-red-600">*</span>
                      </label>
                      <input
                        id="userName"
                        name="userName"
                        type="text"
                        className="form-input w-full text-gray-300"
                        placeholder="이름"
                        value={formData.userName}
                        onChange={handleInputChange}
                        required
                      />
                    </div>
                  </div>

                  {/* 닉네임 */}
                  <div className="flex flex-wrap -mx-3 mb-4">
                    <div className="w-full px-3">
                      <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="nickname">
                        닉네임<span className="text-red-600">*</span>
                      </label>
                      <input
                        id="nickname"
                        name="nickname"
                        type="text"
                        className="form-input w-full text-gray-300"
                        placeholder="닉네임"
                        value={formData.nickname}
                        onChange={handleInputChange}
                        required
                      />
                    </div>
                  </div>

                  {/* 이메일 */}
                  <div className="flex flex-wrap -mx-3 mb-4">
                    <div className="w-full px-3">
                      <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="email">
                        이메일 주소<span className="text-red-600">*</span>
                      </label>
                      <input
                        id="email"
                        name="email"
                        type="email"
                        className="form-input w-full text-gray-300"
                        placeholder="이메일 주소"
                        value={formData.email}
                        onChange={handleInputChange}
                        required
                      />
                    </div>
                  </div>

                  {/* 비밀번호 */}
                  <div className="flex flex-wrap -mx-3 mb-4">
                    <div className="w-full px-3">
                      <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="password">
                        비밀번호<span className="text-red-600">*</span>
                      </label>
                      <input
                        id="password"
                        name="password"
                        type="password"
                        className="form-input w-full text-gray-300"
                        placeholder="비밀번호"
                        value={formData.password}
                        onChange={handleInputChange}
                        required
                      />
                    </div>
                  </div>

                  {/* 전화번호 */}
                  <div className="flex flex-wrap -mx-3 mb-4">
                    <div className="w-full px-3">
                      <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="phoneNumber">
                        전화번호<span className="text-red-600">*</span>
                      </label>
                      <input
                        id="phoneNumber"
                        name="phoneNumber"
                        type="tel"
                        className="form-input w-full text-gray-300"
                        placeholder="전화번호 (예: +821012345678)"
                        value={formData.phoneNumber}
                        onChange={handleInputChange}
                        required
                      />
                    </div>
                  </div>

                  {/* 생년월일 */}
                  <div className="flex flex-wrap -mx-3 mb-4">
                    <div className="w-full px-3">
                      <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="birthDate">
                        생년월일<span className="text-red-600">*</span>
                      </label>
                      <input
                        id="birthDate"
                        name="birthDate"
                        type="date"
                        className="form-input w-full text-gray-300"
                        value={formData.birthDate}
                        onChange={handleInputChange}
                        required
                      />
                    </div>
                  </div>

                  <div className="text-sm text-gray-500 text-center">
                    가입 약관에 동의합니다 &nbsp;&nbsp;
                    <Link to="#" className="underline text-gray-400 hover:text-gray-200 hover:no-underline transition duration-150 ease-in-out">
                      Privacy Policy
                    </Link>
                  </div>

                  <div className="flex flex-wrap -mx-3 mt-6">
                    <div className="w-full px-3">
                      <button
                        type="submit"
                        disabled={isLoading}
                        className="btn text-white bg-purple-600 hover:bg-purple-700 disabled:bg-purple-800 disabled:cursor-not-allowed w-full"
                      >
                        {isLoading ? '가입 중...' : '가입하기'}
                      </button>
                    </div>
                  </div>
                </form>

                <div className="text-gray-400 text-center mt-6">
                  이미 계정이 있으신가요? <Link to="signin" className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">로그인</Link>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

export default SignUp;