import React from 'react';

function FeaturesBlocks() {
  return (
    <section>
      {/* 요소 크기 조정 */}

      <div className="w-[85%] mx-auto px-4 sm:px-6">
        <div className="py-12 md:py-20">

          <h3 className="h3 mb-6">
            취업 공고
          </h3>


          {/* Section header */}
          {/* <div className="max-w-3xl mx-auto text-center pb-12 md:pb-20">
            <h2 className="h2 mb-4">The majority our customers do not understand their workflows.</h2>
            <p className="text-xl text-gray-400">Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
          </div> */}

          {/* 그리드 레이아웃 */}
          <div className="max-w-sm mx-auto grid gap-8 md:grid-cols-3 lg:grid-cols-4 lg:gap-16 items-start md:max-w-2xl lg:max-w-none" data-aos-id-blocks>

            {/* 1 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">한국ICT기술협회</h4>
              <p className="text-lg text-gray-400 grow">[국비무료IT취업] <br /> 자바파이썬/웹개발 외</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">관심기업 TOP100</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.15(목)</p>
              </div>
            </div>


            {/* 2 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">SK쉴더스</h4>
              <p className="text-lg text-gray-400 grow">생성형AI 활용 사이버보안<br />채용연계형 모집</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">대기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.20(화)</p>
              </div>
            </div>


            {/* 3 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">인텔코리아</h4>
              <p className="text-lg text-gray-400 grow">인공지능 응용앱 크리에이터<br /> 양성과정</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">인기있는</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.19(월)</p>
              </div>
            </div>


            {/* 4 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">현대모비스</h4>
              <p className="text-lg text-gray-400 grow">장학전환 인턴모집</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">TOP100기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>

            {/* 5 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">LG CNS</h4>
              <p className="text-lg text-gray-400 grow">2025 Global Internship</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">TOP100 기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>

            {/* 6 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">점핏</h4>
              <p className="text-lg text-gray-400 grow">인공지능 응용앱 크리에이터<br /> 양성과정</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">인기있는</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.19(월)</p>
              </div>
            </div>

            {/* 7 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">현대오토에버</h4>
              <p className="text-lg text-gray-400 grow">2025년 상반기 신입사원 채용<br /> 온라인 채용설명회 신청 중</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">대기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>


            {/* 1 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">한국ICT기술협회</h4>
              <p className="text-lg text-gray-400 grow">[국비무료IT취업] <br /> 자바파이썬/웹개발 외</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">관심기업 TOP100</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.15(목)</p>
              </div>
            </div>


            {/* 2 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">SK쉴더스</h4>
              <p className="text-lg text-gray-400 grow">생성형AI 활용 사이버보안<br />채용연계형 모집</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">대기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.20(화)</p>
              </div>
            </div>


            {/* 3 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">인텔코리아</h4>
              <p className="text-lg text-gray-400 grow">인공지능 응용앱 크리에이터<br /> 양성과정</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">인기있는</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.19(월)</p>
              </div>
            </div>


            {/* 4 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">현대모비스</h4>
              <p className="text-lg text-gray-400 grow">장학전환 인턴모집</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">TOP100기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>

            {/* 5 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">LG CNS</h4>
              <p className="text-lg text-gray-400 grow">2025 Global Internship</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">TOP100 기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>

            {/* 6 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">점핏</h4>
              <p className="text-lg text-gray-400 grow">인공지능 응용앱 크리에이터<br /> 양성과정</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">인기있는</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.19(월)</p>
              </div>
            </div>

            {/* 7 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">현대오토에버</h4>
              <p className="text-lg text-gray-400 grow">2025년 상반기 신입사원 채용<br /> 온라인 채용설명회 신청 중</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">대기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>



            {/* 1 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">한국ICT기술협회</h4>
              <p className="text-lg text-gray-400 grow">[국비무료IT취업] <br /> 자바파이썬/웹개발 외</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">관심기업 TOP100</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.15(목)</p>
              </div>
            </div>


            {/* 2 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">SK쉴더스</h4>
              <p className="text-lg text-gray-400 grow">생성형AI 활용 사이버보안<br />채용연계형 모집</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">대기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.20(화)</p>
              </div>
            </div>


            {/* 3 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">인텔코리아</h4>
              <p className="text-lg text-gray-400 grow">인공지능 응용앱 크리에이터<br /> 양성과정</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">인기있는</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.19(월)</p>
              </div>
            </div>


            {/* 4 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">현대모비스</h4>
              <p className="text-lg text-gray-400 grow">장학전환 인턴모집</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">TOP100기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>

            {/* 5 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">LG CNS</h4>
              <p className="text-lg text-gray-400 grow">2025 Global Internship</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">TOP100 기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>

            {/* 6 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">점핏</h4>
              <p className="text-lg text-gray-400 grow">인공지능 응용앱 크리에이터<br /> 양성과정</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">인기있는</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.19(월)</p>
              </div>
            </div>

            {/* 7 */}
            <div className="flex flex-col h-full p-6 bg-gray-800" data-aos="fade-up">
              <h4 className="h4 mt-4 mb-4">현대오토에버</h4>
              <p className="text-lg text-gray-400 grow">2025년 상반기 신입사원 채용<br /> 온라인 채용설명회 신청 중</p>
              <div className="flex justify-between items-center text-gray-700 font-medium mt-6 pt-5 border-t border-gray-700">
                <p className="text-gray-200 not-italic">대기업</p>
                <p className="text-purple-600 hover:text-gray-200 transition duration-150 ease-in-out">~05.12(월)</p>
              </div>
            </div>

          </div>




          {/* 그리드 레이아웃 */}
          {/* <div className="max-w-sm mx-auto grid gap-8 md:grid-cols-2 lg:grid-cols-3 lg:gap-16 items-start md:max-w-2xl lg:max-w-none" data-aos-id-blocks> */}

          {/* 1st item */}
          {/* <div className="relative flex flex-col items-center" data-aos="fade-up" data-aos-anchor="[data-aos-id-blocks]">
              <svg className="w-16 h-16 mb-4" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
                <rect className="fill-current text-purple-600" width="64" height="64" rx="32" />
                <path className="stroke-current text-purple-100" d="M30 39.313l-4.18 2.197L27 34.628l-5-4.874 6.91-1.004L32 22.49l3.09 6.26L42 29.754l-3 2.924" strokeLinecap="square" strokeWidth="2" fill="none" fillRule="evenodd" />
                <path className="stroke-current text-purple-300" d="M43 42h-9M43 37h-9" strokeLinecap="square" strokeWidth="2" />
              </svg>
              <h4 className="h4 mb-2">Instant Features</h4>
              <p className="text-lg text-gray-400 text-center">Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat.</p>
            </div> */}

          {/* 2nd item */}
          {/* <div className="relative flex flex-col items-center" data-aos="fade-up" data-aos-delay="100" data-aos-anchor="[data-aos-id-blocks]">
              <svg className="w-16 h-16 mb-4" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
                <circle className="fill-current text-purple-600" cx="32" cy="32" r="32" />
                <path className="stroke-current text-purple-100" strokeWidth="2" strokeLinecap="square" d="M21 23h22v18H21z" fill="none" fillRule="evenodd" />
                <path className="stroke-current text-purple-300" d="M26 28h12M26 32h12M26 36h5" strokeWidth="2" strokeLinecap="square" />
              </svg>
              <h4 className="h4 mb-2">Instant Features</h4>
              <p className="text-lg text-gray-400 text-center">Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat.</p>
            </div> */}

          {/* 3rd item */}
          {/* <div className="relative flex flex-col items-center" data-aos="fade-up" data-aos-delay="200" data-aos-anchor="[data-aos-id-blocks]">
              <svg className="w-16 h-16 mb-4" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
                <rect className="fill-current text-purple-600" width="64" height="64" rx="32" />
                <g transform="translate(21 21)" strokeLinecap="square" strokeWidth="2" fill="none" fillRule="evenodd">
                  <ellipse className="stroke-current text-purple-300" cx="11" cy="11" rx="5.5" ry="11" />
                  <path className="stroke-current text-purple-100" d="M11 0v22M0 11h22" />
                  <circle className="stroke-current text-purple-100" cx="11" cy="11" r="11" />
                </g>
              </svg>
              <h4 className="h4 mb-2">Instant Features</h4>
              <p className="text-lg text-gray-400 text-center">Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat.</p>
            </div> */}

          {/* 4th item */}
          {/* <div className="relative flex flex-col items-center" data-aos="fade-up" data-aos-delay="300" data-aos-anchor="[data-aos-id-blocks]">
              <svg className="w-16 h-16 mb-4" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
                <rect className="fill-current text-purple-600" width="64" height="64" rx="32" />
                <g transform="translate(22 21)" strokeLinecap="square" strokeWidth="2" fill="none" fillRule="evenodd">
                  <path className="stroke-current text-purple-100" d="M17 22v-6.3a8.97 8.97 0 003-6.569A9.1 9.1 0 0011.262 0 9 9 0 002 9v1l-2 5 2 1v4a2 2 0 002 2h4a5 5 0 005-5v-5" />
                  <circle className="stroke-current text-purple-300" cx="13" cy="9" r="3" />
                </g>
              </svg>
              <h4 className="h4 mb-2">Instant Features</h4>
              <p className="text-lg text-gray-400 text-center">Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat.</p>
            </div> */}

          {/* 5th item */}
          {/* <div className="relative flex flex-col items-center" data-aos="fade-up" data-aos-delay="400" data-aos-anchor="[data-aos-id-blocks]">
              <svg className="w-16 h-16 mb-4" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
                <rect className="fill-current text-purple-600" width="64" height="64" rx="32" />
                <g strokeLinecap="square" strokeWidth="2" fill="none" fillRule="evenodd">
                  <path className="stroke-current text-purple-100" d="M29 42h10.229a2 2 0 001.912-1.412l2.769-9A2 2 0 0042 29h-7v-4c0-2.373-1.251-3.494-2.764-3.86a1.006 1.006 0 00-1.236.979V26l-5 6" />
                  <path className="stroke-current text-purple-300" d="M22 30h4v12h-4z" />
                </g>
              </svg>
              <h4 className="h4 mb-2">Instant Features</h4>
              <p className="text-lg text-gray-400 text-center">Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat.</p>
            </div> */}

          {/* 6th item */}
          {/* <div className="relative flex flex-col items-center" data-aos="fade-up" data-aos-delay="500" data-aos-anchor="[data-aos-id-blocks]">
              <svg className="w-16 h-16 mb-4" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
                <rect className="fill-current text-purple-600" width="64" height="64" rx="32" />
                <g transform="translate(21 22)" strokeLinecap="square" strokeWidth="2" fill="none" fillRule="evenodd">
                  <path className="stroke-current text-purple-300" d="M17 2V0M19.121 2.879l1.415-1.415M20 5h2M19.121 7.121l1.415 1.415M17 8v2M14.879 7.121l-1.415 1.415M14 5h-2M14.879 2.879l-1.415-1.415" />
                  <circle className="stroke-current text-purple-300" cx="17" cy="5" r="3" />
                  <path className="stroke-current text-purple-100" d="M8.86 1.18C3.8 1.988 0 5.6 0 10c0 5 4.9 9 11 9a10.55 10.55 0 003.1-.4L20 21l-.6-5.2a9.125 9.125 0 001.991-2.948" />
                </g>
              </svg>
              <h4 className="h4 mb-2">Instant Features</h4>
              <p className="text-lg text-gray-400 text-center">Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat.</p>
            </div> */}
          {/* </div> */}

        </div>
      </div>
    </section >
  );
}

export default FeaturesBlocks;
