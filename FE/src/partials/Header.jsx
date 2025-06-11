import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Dropdown from '../utils/Dropdown';

function Header() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userId, setUserId] = useState('');
  const [userEmail, setUserEmail] = useState('');
  const navigate = useNavigate();

  const trigger = useRef(null);
  const mobileNav = useRef(null);

  // Check login status on component mount
  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUserId = localStorage.getItem('userId');
    const storedEmail = localStorage.getItem('userEmail');
    if (token && storedUserId) {
      setIsLoggedIn(true);
      setUserId(storedUserId);
      console.log(storedUserId)
      setUserEmail(storedEmail);
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('savedQuestions');
    setIsLoggedIn(false);
    setUserId('');
    setUserEmail('');
    navigate('/');
  };

  // close the mobile menu on click outside
  useEffect(() => {
    const clickHandler = ({ target }) => {
      if (!mobileNav.current || !trigger.current) return;
      if (!mobileNavOpen || mobileNav.current.contains(target) || trigger.current.contains(target)) return;
      setMobileNavOpen(false);
    };
    document.addEventListener('click', clickHandler);
    return () => document.removeEventListener('click', clickHandler);
  });

  // close the mobile menu if the esc key is pressed
  useEffect(() => {
    const keyHandler = ({ keyCode }) => {
      if (!mobileNavOpen || keyCode !== 27) return;
      setMobileNavOpen(false);
    };
    document.addEventListener('keydown', keyHandler);
    return () => document.removeEventListener('keydown', keyHandler);
  });

  return (
    <header className="absolute w-full z-30">
      <div className="w-[85%] mx-auto px-4 sm:px-6">

        <div className="flex items-center justify-between h-20">
          {/* 사이트 로고 */}
          <Link to="/" className="flex items-center">
            <div className="shrink-0 mr-4">
              <svg className="w-8 h-8 fill-current text-purple-600" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                <path d="M31.952 14.751a260.51 260.51 0 00-4.359-4.407C23.932 6.734 20.16 3.182 16.171 0c1.634.017 3.21.28 4.692.751 3.487 3.114 6.846 6.398 10.163 9.737.493 1.346.811 2.776.926 4.262zm-1.388 7.883c-2.496-2.597-5.051-5.12-7.737-7.471-3.706-3.246-10.693-9.81-15.736-7.418-4.552 2.158-4.717 10.543-4.96 16.238A15.926 15.926 0 010 16C0 9.799 3.528 4.421 8.686 1.766c1.82.593 3.593 1.675 5.038 2.587 6.569 4.14 12.29 9.71 17.792 15.57-.237.94-.557 1.846-.952 2.711zm-4.505 5.81a56.161 56.161 0 00-1.007-.823c-2.574-2.054-6.087-4.805-9.394-4.044-3.022.695-4.264 4.267-4.97 7.52a15.945 15.945 0 01-3.665-1.85c.366-3.242.89-6.675 2.405-9.364 2.315-4.107 6.287-3.072 9.613-1.132 3.36 1.96 6.417 4.572 9.313 7.417a16.097 16.097 0 01-2.295 2.275z" />
              </svg>
            </div>
            <div className="text-2xl font-bold">INTERV</div>
          </Link>

          <nav className="hidden md:flex md:grow">
            {/* Desktop sign in links */}
            <ul className="flex grow justify-end flex-wrap items-center">
              <li>
                <Link to="/resume" className="font-medium text-gray-300 hover:text-white px-4 py-3 flex items-center transition duration-150 ease-in-out">
                  이력서 관리
                </Link>
              </li>
              <li>
                <Link to="/questions" className="font-medium text-gray-300 hover:text-white px-4 py-3 flex items-center transition duration-150 ease-in-out">
                  예상 질문 리스트
                </Link>
              </li>
              <li>
                <Link to="/interview" className="font-medium text-gray-300 hover:text-white px-4 py-3 flex items-center transition duration-150 ease-in-out">
                  모의 면접
                </Link>
              </li>
              {!isLoggedIn ? (
                <>
                  <li>
                    <Link to="/signin" className="font-medium text-purple-600 hover:text-gray-200 px-4 py-3 flex items-center transition duration-150 ease-in-out">
                      로그인
                    </Link>
                  </li>
                  <li>
                    <Link to="/signup" className="btn-sm text-white bg-purple-600 hover:bg-purple-700 ml-3">
                      회원가입
                    </Link>
                  </li>
                </>
              ) : (
                <>
                  <li>
                    <span className="font-medium text-purple-600 px-4 py-3 flex items-center">
                      {userEmail}
                    </span>
                  </li>
                  <li>
                    <button
                      onClick={handleLogout}
                      className="btn-sm text-white bg-purple-600 hover:bg-purple-700 ml-3"
                    >
                      로그아웃
                    </button>
                  </li>
                </>
              )}
            </ul>
          </nav>

          {/* Mobile menu */}
          <div className="md:hidden">
            {/* Hamburger button */}
            <button
              ref={trigger}
              className={`hamburger ${mobileNavOpen && 'active'}`}
              aria-controls="mobile-nav"
              aria-expanded={mobileNavOpen}
              onClick={() => setMobileNavOpen(!mobileNavOpen)}
            >
              <span className="sr-only">Menu</span>
              <svg className="w-6 h-6 fill-current text-gray-300 hover:text-gray-200 transition duration-150 ease-in-out" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <rect y="4" width="24" height="2" rx="1" />
                <rect y="11" width="24" height="2" rx="1" />
                <rect y="18" width="24" height="2" rx="1" />
              </svg>
            </button>

            {/* Mobile navigation */}
            <nav
              id="mobile-nav"
              ref={mobileNav}
              className="absolute top-full z-20 left-0 w-full px-4 sm:px-6 overflow-hidden transition-all duration-300 ease-in-out"
              style={mobileNavOpen ? { maxHeight: mobileNav.current.scrollHeight, opacity: 1 } : { maxHeight: 0, opacity: .8 }}
            >
              <ul className="bg-gray-800 px-4 py-2">
                <li>
                  <Link to="/resume" className="flex font-medium w-full text-gray-300 hover:text-white py-2 justify-center">
                    이력서 관리
                  </Link>
                </li>
                <li>
                  <Link to="/questions" className="flex font-medium w-full text-gray-300 hover:text-white py-2 justify-center">
                    예상 질문 리스트
                  </Link>
                </li>
                <li>
                  <Link to="/interview" className="flex font-medium w-full text-gray-300 hover:text-white py-2 justify-center">
                    모의 면접
                  </Link>
                </li>
                {!isLoggedIn ? (
                  <>
                    <li>
                      <Link to="/signin" className="flex font-medium w-full text-purple-600 hover:text-gray-200 py-2 justify-center">
                        로그인
                      </Link>
                    </li>
                    <li>
                      <Link to="/signup" className="font-medium w-full inline-flex items-center justify-center border border-transparent px-4 py-2 my-2 rounded-sm text-white bg-purple-600 hover:bg-purple-700 transition duration-150 ease-in-out">
                        회원가입
                      </Link>
                    </li>
                  </>
                ) : (
                  <>
                    <li>
                      <span className="flex font-medium w-full text-purple-600 py-2 justify-center">
                        {userEmail}
                      </span>
                    </li>
                    <li>
                      <button
                        onClick={handleLogout}
                        className="font-medium w-full inline-flex items-center justify-center border border-transparent px-4 py-2 my-2 rounded-sm text-white bg-purple-600 hover:bg-purple-700 transition duration-150 ease-in-out"
                      >
                        로그아웃
                      </button>
                    </li>
                  </>
                )}
              </ul>
            </nav>
          </div>

        </div>

        {/* <div>
          <nav className="hidden md:flex md:grow  py-5 shadow-md">
            <ul className="flex grow justify-start flex-wrap items-center">
              <li className="px-4 text-2xl">모의 면접</li>
              <li className="px-4 text-2xl">나의 이력서</li>
              <li className="px-4 text-2xl">마이페이지</li>
            </ul>
          </nav>
        </div> */}

      </div>
    </header>
  );
}

export default Header;
