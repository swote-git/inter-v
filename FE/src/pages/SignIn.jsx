import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import Header from '../partials/Header';
import PageIllustration from '../partials/PageIllustration';
import Banner from '../partials/Banner';

function SignIn() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userEmail, setUserEmail] = useState('');
  const [showLogout, setShowLogout] = useState(false);
  const navigate = useNavigate();
  const userRef = useRef(null);

  // 로그인 상태 확인 (새로고침 시에도 유지)
  useEffect(() => {
    // 로그인 후 localStorage에 저장된 이메일로 로그인 상태 유지
    const storedEmail = localStorage.getItem('userEmail');
    if (storedEmail) {
      setIsLoggedIn(true);
      setUserEmail(storedEmail);
    }
  }, []);

  // 바깥 클릭 시 로그아웃 메뉴 닫기
  useEffect(() => {
    function handleClickOutside(event) {
      if (userRef.current && !userRef.current.contains(event.target)) {
        setShowLogout(false);
      }
    }
    if (showLogout) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showLogout]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const res = await fetch('https://api.interv.swote.dev/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
        credentials: 'include',
      });
      if (!res.ok) throw new Error('로그인 실패');
      setIsLoggedIn(true);
      setUserEmail(email);
      localStorage.setItem('userEmail', email);
      setError('');
      navigate('/');
    } catch (err) {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    }
  };

  const handleLogout = async () => {
    try {
      const res = await fetch('https://api.interv.swote.dev/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'accept': '*/*',
        },
      });
      if (res.ok) {
        setIsLoggedIn(false);
        setUserEmail('');
        localStorage.removeItem('userEmail');
        setShowLogout(false);
        alert('로그아웃 되었습니다.');
        navigate('/signin');
      }
    } catch (err) {
      alert('로그아웃에 실패했습니다.');
    }
  };

  return (
    <div className="flex flex-col min-h-screen overflow-hidden">

      {/*  Site header */}
      <Header
        isLoggedIn={isLoggedIn}
        userEmail={userEmail}
        onLogout={handleLogout}
      />

      {/*  Page content */}
      <main className="grow">

        {/*  Page illustration */}
        <div className="relative max-w-6xl mx-auto h-0 pointer-events-none" aria-hidden="true">
          <PageIllustration />
        </div>

        <section className="relative">
          <div className=" mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">

              {/* Page header */}
              <div className="w-[38%] mx-auto text-left pb-12 md:pb-20">
                <h1 className="h2">로그인 한 번으로 <br /> INTERV의 서비스를 사용해보세요</h1>
              </div>

              {/* Form or User Info */}
              <div className="w-[30%] mx-auto">
                {!isLoggedIn ? (
                  <form onSubmit={handleSubmit}>
                    <div className="flex flex-wrap -mx-3 mb-4">
                      <div className="w-full px-3">
                        <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="email">Email</label>
                        <input
                          id="email"
                          type="email"
                          className="form-input w-full text-gray-300"
                          placeholder="이메일 주소"
                          required
                          value={email}
                          onChange={e => setEmail(e.target.value)}
                        />
                      </div>
                    </div>
                    <div className="flex flex-wrap -mx-3 mb-4">
                      <div className="w-full px-3">
                        <label className="block text-gray-300 text-sm font-medium mb-1" htmlFor="password">비밀번호</label>
                        <input
                          id="password"
                          type="password"
                          className="form-input w-full text-gray-300"
                          placeholder="비밀번호"
                          required
                          value={password}
                          onChange={e => setPassword(e.target.value)}
                        />
                      </div>
                    </div>
                    <div className="flex flex-wrap -mx-3 mb-4">
                      <div className="w-full px-3">
                        <div className="flex justify-between">
                          <label className="flex items-center">
                            <input type="checkbox" className="form-checkbox" />
                            <span className="text-gray-400 ml-2">로그인 유지</span>
                          </label>
                          <Link to="/reset-password" className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">Forgot Password?</Link>
                        </div>
                      </div>
                    </div>
                    {error && (
                      <div className="text-red-500 text-sm mb-2">{error}</div>
                    )}
                    <div className="flex flex-wrap -mx-3 mt-6">
                      <div className="w-full px-3">
                        <button className="btn text-white bg-purple-600 hover:bg-purple-700 w-full" type="submit">로그인</button>
                      </div>
                    </div>
                  </form>
                ) : (
                  <div className="flex flex-col items-center">
                    <div
                      ref={userRef}
                      className="relative cursor-pointer text-white bg-purple-600 rounded px-4 py-2 w-full text-center"
                      onClick={() => setShowLogout((prev) => !prev)}
                    >
                      {userEmail}
                      {showLogout && (
                        <div className="absolute left-1/2 -translate-x-1/2 mt-2 bg-white border rounded shadow-lg z-10 w-32">
                          <button
                            className="block w-full px-4 py-2 text-gray-800 hover:bg-gray-100 text-center"
                            onClick={handleLogout}
                          >
                            로그아웃
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                )}
                {!isLoggedIn && (
                  <div className="text-gray-400 text-center mt-6">
                    계정이 없으세요? <Link to="/signup" className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">회원가입</Link>
                  </div>
                )}
              </div>

            </div>
          </div>
        </section>

      </main>

    </div>
  );
}

export default SignIn;