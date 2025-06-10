import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';

function Resume() {

  const navigate = useNavigate();
  const location = useLocation();
  const [userId, setUserId] = useState('');
  const storedUserId = localStorage.getItem('userId');
  const token = localStorage.getItem('token');
  const [existingResume, setExistingResume] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);

  const [formData, setFormData] = useState({
    title: '',
    content: '',
    objective: '',
    skills: [''],
    projects: [{
      projectName: '',
      description: '',
      startDate: '',
      endDate: '',
      inProgress: false
    }],
    certifications: [{
      certificationName: '',
      issuingOrganization: '',
      acquiredDate: '',
      expiryDate: '',
      noExpiry: false
    }],
    workExperiences: [{
      companyName: '',
      position: '',
      department: '',
      location: '',
      startDate: '',
      endDate: '',
      currentlyWorking: false,
      responsibilities: '',
      achievements: ''
    }],
    educations: [{
      schoolType: '',
      schoolName: '',
      location: '',
      major: '',
      enrollmentDate: '',
      graduationDate: '',
      inProgress: false,
      gpa: ''
    }]
  });

  useEffect(() => {
    const fetchExistingResume = async () => {
      try {
        const id = Number(storedUserId);
        if (!id || isNaN(id)) {
          setIsLoading(false);
          return;
        }

        // First, get the list of resumes to find the most recent one
        const listResponse = await fetch(`https://api.interv.swote.dev/api/resumes?id=${id}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'accept': '*/*',
            'Authorization': `Bearer ${token}`
          },
        });

        if (listResponse.ok) {
          const listData = await listResponse.json();
          if (listData.data && listData.data.content && listData.data.content.length > 0) {
            // Get the most recent resume
            const mostRecentResume = listData.data.content[0];
            const resumeId = mostRecentResume.id;

            // Now fetch the detailed resume using the new endpoint
            const detailResponse = await fetch(`https://api.interv.swote.dev/api/resumes/${resumeId}`, {
              method: 'GET',
              headers: {
                'Content-Type': 'application/json',
                'accept': '*/*',
                'Authorization': `Bearer ${token}`
              },
            });

            if (detailResponse.ok) {
              const detailData = await detailResponse.json();
              if (detailData.data) {
                const resume = detailData.data;
                console.log('Resume Detail:', resume);

                // 날짜 배열을 YYYY-MM-DD 형식의 문자열로 변환하는 함수
                const formatDateArray = (dateArray) => {
                  if (!dateArray || !Array.isArray(dateArray) || dateArray.length < 3) return '';
                  const [year, month, day] = dateArray;
                  // 월과 일이 한 자리 수인 경우 앞에 0을 붙임
                  const formattedMonth = String(month).padStart(2, '0');
                  const formattedDay = String(day).padStart(2, '0');
                  return `${year}-${formattedMonth}-${formattedDay}`;
                };

                // Set the existing resume data
                setExistingResume(resume);
                setFormData({
                  title: resume.title || '',
                  content: resume.content || '',
                  objective: resume.objective || '',
                  skills: resume.skills || [],
                  projects: (resume.projects || []).map(project => ({
                    id: project.id,
                    projectName: project.projectName || '',
                    description: project.description || '',
                    startDate: formatDateArray(project.startDate),
                    endDate: formatDateArray(project.endDate),
                    inProgress: project.inProgress || false
                  })),
                  certifications: (resume.certifications || []).map(cert => ({
                    id: cert.id,
                    certificationName: cert.certificationName || '',
                    issuingOrganization: cert.issuingOrganization || '',
                    acquiredDate: formatDateArray(cert.acquiredDate),
                    expiryDate: formatDateArray(cert.expiryDate),
                    noExpiry: cert.noExpiry || false
                  })),
                  workExperiences: (resume.workExperiences || []).map(work => ({
                    id: work.id,
                    companyName: work.companyName || '',
                    position: work.position || '',
                    department: work.department || '',
                    location: work.location || '',
                    startDate: formatDateArray(work.startDate),
                    endDate: formatDateArray(work.endDate),
                    currentlyWorking: work.currentlyWorking || false,
                    responsibilities: work.responsibilities || '',
                    achievements: work.achievements || ''
                  })),
                  educations: (resume.educations || []).map(edu => ({
                    id: edu.id,
                    schoolType: edu.schoolType || '',
                    schoolName: edu.schoolName || '',
                    location: edu.location || '',
                    major: edu.major || '',
                    enrollmentDate: formatDateArray(edu.enrollmentDate),
                    graduationDate: formatDateArray(edu.graduationDate),
                    inProgress: edu.inProgress || false,
                    gpa: edu.gpa || ''
                  }))
                });
              }
            } else {
              console.error('이력서 상세 조회 실패:', detailResponse.status);
              const errorText = await detailResponse.text();
              console.error('오류 상세:', errorText);
              setExistingResume(null);
            }
          } else {
            console.log('등록된 이력서가 없습니다.');
            setExistingResume(null);
          }
        } else {
          console.error('이력서 목록 조회 실패:', listResponse.status);
          const errorText = await listResponse.text();
          console.error('오류 상세:', errorText);
          setExistingResume(null);
        }
      } catch (error) {
        console.error('이력서 조회 중 오류 발생:', error);
        setExistingResume(null);
      } finally {
        setIsLoading(false);
      }
    };

    fetchExistingResume();
  }, [storedUserId, token]);

  const handleInputChange = (e, arrayName, index) => {
    const { name, value, type, checked } = e.target;

    if (arrayName === 'skills') {
      // 기술 스택 배열 처리
      setFormData(prev => ({
        ...prev,
        skills: prev.skills.map((skill, i) => {
          if (i === index) {
            return value;
          }
          return skill;
        })
      }));
    } else if (arrayName) {
      setFormData(prev => ({
        ...prev,
        [arrayName]: prev[arrayName].map((item, i) => {
          if (i === index) {
            if (type === 'checkbox') {
              return { ...item, [name]: checked };
            }
            return { ...item, [name]: value };
          }
          return item;
        })
      }));
    } else {
      setFormData(prev => ({
        ...prev,
        [name]: type === 'checkbox' ? checked : value
      }));
    }
  };

  const addArrayItem = (section) => {
    const emptyItem = {
      projects: {
        projectName: '',
        description: '',
        startDate: '',
        endDate: '',
        inProgress: false
      },
      certifications: {
        certificationName: '',
        issuingOrganization: '',
        acquiredDate: '',
        expiryDate: '',
        noExpiry: false
      },
      workExperiences: {
        companyName: '',
        position: '',
        department: '',
        location: '',
        startDate: '',
        endDate: '',
        currentlyWorking: false,
        responsibilities: '',
        achievements: ''
      },
      educations: {
        schoolType: '',
        schoolName: '',
        location: '',
        major: '',
        enrollmentDate: '',
        graduationDate: '',
        inProgress: false,
        gpa: ''
      }
    };

    setFormData({
      ...formData,
      [section]: [...formData[section], emptyItem[section]]
    });
  };

  const removeArrayItem = (section, index) => {
    const newArray = formData[section].filter((_, i) => i !== index);
    setFormData({
      ...formData,
      [section]: newArray
    });
  };

  const addSkill = () => {
    setFormData({
      ...formData,
      skills: [...formData.skills, '']
    });
  };

  const removeSkill = (index) => {
    const newSkills = formData.skills.filter((_, i) => i !== index);
    setFormData({
      ...formData,
      skills: newSkills
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      let id = storedUserId;
      if (!id) {
        const userStr = localStorage.getItem('user');
        if (userStr) {
          try {
            const userObj = JSON.parse(userStr);
            id = userObj.id;
          } catch { }
        }
      }

      if (!id || isNaN(Number(id))) {
        alert('로그인이 필요합니다. (userId가 없습니다)');
        return;
      }

      id = Number(id);

      if (!id || isNaN(id)) {
        alert('userId가 올바르지 않습니다. 다시 로그인 해주세요.');
        return;
      }

      // Create a clean resume data object without any existing IDs
      const resumeData = {
        title: formData.title,
        content: formData.content,
        objective: formData.objective,
        skills: formData.skills.filter(skill => skill.trim() !== ''), // 빈 문자열 제거
        projects: formData.projects.map(project => ({
          projectName: project.projectName,
          description: project.description,
          startDate: project.startDate,
          endDate: project.inProgress ? null : project.endDate,
          inProgress: project.inProgress
        })),
        certifications: formData.certifications.map(cert => ({
          certificationName: cert.certificationName,
          issuingOrganization: cert.issuingOrganization,
          acquiredDate: cert.acquiredDate,
          expiryDate: cert.noExpiry ? null : cert.expiryDate,
          noExpiry: cert.noExpiry
        })),
        workExperiences: formData.workExperiences.map(work => ({
          companyName: work.companyName,
          position: work.position,
          department: work.department || '',
          location: work.location || '',
          startDate: work.startDate,
          endDate: work.currentlyWorking ? null : work.endDate,
          currentlyWorking: work.currentlyWorking,
          responsibilities: work.responsibilities,
          achievements: work.achievements || ''
        })),
        educations: formData.educations.map(edu => ({
          schoolType: edu.schoolType,
          schoolName: edu.schoolName,
          location: edu.location || '',
          major: edu.major || '',
          enrollmentDate: edu.enrollmentDate,
          graduationDate: edu.inProgress ? null : edu.graduationDate,
          inProgress: edu.inProgress,
          gpa: edu.gpa || ''
        }))
      };

      console.log('Submitting resume data:', resumeData); // 요청 데이터 로깅

      let response;
      if (existingResume) {
        console.log('Updating existing resume with ID:', existingResume.id); // 수정 중인 이력서 ID 로깅

        const putData = {
          ...resumeData,
          id: existingResume.id, // ID 명시적 포함
          status: 'ACTIVE' // 상태 명시적 포함
        };

        console.log('hi')
        console.log('PUT 요청 데이터:', putData); // PUT 요청 데이터를 보기 좋게 출력

        // 기존 이력서 수정 - PUT 요청으로 완전히 새로운 데이터로 덮어쓰기
        response = await fetch(`https://api.interv.swote.dev/api/resumes/${existingResume.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'accept': '*/*',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify(putData)
        });

        // 응답 로깅
        const responseText = await response.text();
        console.log('서버 응답:', responseText);

        if (!response.ok) {
          throw new Error(responseText || '이력서 수정에 실패했습니다.');
        }
      } else {
        // 새 이력서 생성
        response = await fetch(`https://api.interv.swote.dev/api/resumes?id=${id}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'accept': '*/*',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            ...resumeData,
            status: 'ACTIVE' // 상태 명시적 포함
          })
        });

        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(errorText || '이력서 제출에 실패했습니다.');
        }
      }

      alert(existingResume ? '이력서가 성공적으로 수정되었습니다.' : '이력서가 성공적으로 제출되었습니다.');

      // 수정 후 페이지 새로고침
      window.location.reload();
    } catch (error) {
      console.error('이력서 제출 중 오류 발생:', error);
      alert(error.message || '이력서 제출 중 오류가 발생했습니다.');
    }
  };

  return (
    <div className="flex flex-col min-h-screen overflow-hidden">
      <Header />

      <main className="grow">
        <section className="relative">
          <div className="max-w-6xl mx-auto px-4 sm:px-6">
            <div className="pt-32 pb-12 md:pt-40 md:pb-20">
              <div className="max-w-3xl mx-auto">
                <h1 className="h2 mb-8">이력서 {existingResume ? '보기' : '작성'}</h1>

                {isLoading ? (
                  <div className="text-center text-gray-300">로딩 중...</div>
                ) : existingResume && !isEditing ? (
                  <div className="space-y-12">
                    {/* 기본 정보 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <h2 className="text-xl font-medium mb-4 text-white">기본 정보</h2>
                      <div className="space-y-4">
                        <div>
                          <label className="block text-sm font-medium mb-2 text-gray-300">제목</label>
                          <div className="text-white">{existingResume.title}</div>
                        </div>
                        <div>
                          <label className="block text-sm font-medium mb-2 text-gray-300">내용</label>
                          <div className="text-white whitespace-pre-wrap">{existingResume.content}</div>
                        </div>
                        <div>
                          <label className="block text-sm font-medium mb-2 text-gray-300">목표</label>
                          <div className="text-white">{existingResume.objective}</div>
                        </div>
                      </div>
                    </div>

                    {/* 기술 스택 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <h2 className="text-xl font-medium mb-4 text-white">기술 스택</h2>
                      <div className="flex flex-wrap gap-2">
                        {existingResume.skills.map((skill, index) => (
                          <span key={index} className="px-3 py-1 bg-purple-600 text-white rounded-full text-sm">
                            {skill}
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* 프로젝트 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <h2 className="text-xl font-medium mb-4 text-white">프로젝트</h2>
                      <div className="space-y-6">
                        {existingResume.projects.map((project, index) => (
                          <div key={index} className="border-b border-gray-700 last:border-0 pb-4 last:pb-0">
                            <h3 className="text-lg font-medium text-white mb-2">{project.projectName}</h3>
                            <p className="text-gray-300 mb-2">{project.description}</p>
                            <div className="text-sm text-gray-400">
                              {project.startDate} ~ {project.inProgress ? '진행 중' : project.endDate}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* 자격증 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <h2 className="text-xl font-medium mb-4 text-white">자격증</h2>
                      <div className="space-y-4">
                        {existingResume.certifications.map((cert, index) => (
                          <div key={index} className="border-b border-gray-700 last:border-0 pb-4 last:pb-0">
                            <h3 className="text-lg font-medium text-white mb-1">{cert.certificationName}</h3>
                            <p className="text-gray-300 mb-1">{cert.issuingOrganization}</p>
                            <div className="text-sm text-gray-400">
                              취득일: {cert.acquiredDate}
                              {!cert.noExpiry && ` / 만료일: ${cert.expiryDate}`}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* 경력사항 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <h2 className="text-xl font-medium mb-4 text-white">경력사항</h2>
                      <div className="space-y-6">
                        {existingResume.workExperiences.map((work, index) => (
                          <div key={index} className="border-b border-gray-700 last:border-0 pb-4 last:pb-0">
                            <h3 className="text-lg font-medium text-white mb-1">{work.companyName}</h3>
                            <p className="text-gray-300 mb-1">{work.position}</p>
                            {work.department && <p className="text-gray-300 mb-1">{work.department}</p>}
                            {work.location && <p className="text-gray-300 mb-1">{work.location}</p>}
                            <div className="text-sm text-gray-400 mb-2">
                              {work.startDate} ~ {work.currentlyWorking ? '현재' : work.endDate}
                            </div>
                            <div className="text-gray-300 mb-2">
                              <h4 className="font-medium mb-1">주요 업무</h4>
                              <p className="whitespace-pre-wrap">{work.responsibilities}</p>
                            </div>
                            {work.achievements && (
                              <div className="text-gray-300">
                                <h4 className="font-medium mb-1">주요 성과</h4>
                                <p className="whitespace-pre-wrap">{work.achievements}</p>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* 학력사항 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <h2 className="text-xl font-medium mb-4 text-white">학력사항</h2>
                      <div className="space-y-4">
                        {existingResume.educations.map((edu, index) => (
                          <div key={index} className="border-b border-gray-700 last:border-0 pb-4 last:pb-0">
                            <h3 className="text-lg font-medium text-white mb-1">{edu.schoolName}</h3>
                            <p className="text-gray-300 mb-1">{edu.schoolType}</p>
                            {edu.major && <p className="text-gray-300 mb-1">{edu.major}</p>}
                            {edu.location && <p className="text-gray-300 mb-1">{edu.location}</p>}
                            <div className="text-sm text-gray-400">
                              {edu.enrollmentDate} ~ {edu.inProgress ? '재학 중' : edu.graduationDate}
                              {edu.gpa && ` / 학점: ${edu.gpa}`}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="flex justify-end">
                      <button
                        onClick={() => setIsEditing(true)}
                        className="btn text-white bg-purple-600 hover:bg-purple-700"
                      >
                        이력서 수정
                      </button>
                    </div>
                  </div>
                ) : (
                  <form onSubmit={handleSubmit} className="space-y-12">
                    {/* 기본 정보 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <h2 className="text-xl font-medium mb-4 text-white">기본 정보</h2>
                      <div className="space-y-4">
                        <div>
                          <label className="block text-sm font-medium mb-2 text-gray-300">제목</label>
                          <input
                            type="text"
                            name="title"
                            value={formData.title}
                            onChange={(e) => handleInputChange(e)}
                            className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                            required
                          />
                        </div>
                        <div>
                          <label className="block text-sm font-medium mb-2 text-gray-300">내용</label>
                          <textarea
                            name="content"
                            value={formData.content}
                            onChange={(e) => handleInputChange(e)}
                            className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                            rows="4"
                            required
                          />
                        </div>
                        <div>
                          <label className="block text-sm font-medium mb-2 text-gray-300">목표</label>
                          <textarea
                            name="objective"
                            value={formData.objective}
                            onChange={(e) => handleInputChange(e)}
                            className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                            rows="2"
                            required
                          />
                        </div>
                      </div>
                    </div>

                    {/* 기술 스택 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-medium text-white">기술 스택</h2>
                        <button
                          type="button"
                          onClick={addSkill}
                          className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                        >
                          + 기술 추가
                        </button>
                      </div>
                      {formData.skills.map((skill, index) => (
                        <div key={index} className="flex gap-2 mb-2">
                          <input
                            type="text"
                            value={skill}
                            onChange={(e) => handleInputChange(e, 'skills', index)}
                            className="form-input flex-1 bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                            required
                          />
                          <button
                            type="button"
                            onClick={() => removeSkill(index)}
                            className="btn-sm text-white bg-gray-600 hover:bg-gray-700"
                          >
                            삭제
                          </button>
                        </div>
                      ))}
                    </div>

                    {/* 프로젝트 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-medium text-white">프로젝트</h2>
                        <button
                          type="button"
                          onClick={() => addArrayItem('projects')}
                          className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                        >
                          + 프로젝트 추가
                        </button>
                      </div>
                      {formData.projects.map((project, index) => (
                        <div key={index} className="mb-6 last:mb-0">
                          <div className="flex justify-between items-center mb-3">
                            <h3 className="text-lg font-medium text-gray-300">프로젝트 {index + 1}</h3>
                            <button
                              type="button"
                              onClick={() => removeArrayItem('projects', index)}
                              className="text-gray-400 hover:text-white"
                            >
                              삭제
                            </button>
                          </div>
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">프로젝트명</label>
                              <input
                                type="text"
                                name="projectName"
                                value={project.projectName}
                                onChange={(e) => handleInputChange(e, 'projects', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">설명</label>
                              <input
                                type="text"
                                name="description"
                                value={project.description}
                                onChange={(e) => handleInputChange(e, 'projects', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">시작일</label>
                              <input
                                type="date"
                                name="startDate"
                                value={project.startDate}
                                onChange={(e) => handleInputChange(e, 'projects', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">종료일</label>
                              <input
                                type="date"
                                name="endDate"
                                value={project.endDate}
                                onChange={(e) => handleInputChange(e, 'projects', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                disabled={project.inProgress}
                              />
                            </div>
                          </div>
                          <div className="mt-4">
                            <label className="flex items-center space-x-2 text-gray-300">
                              <input
                                type="checkbox"
                                name="inProgress"
                                checked={project.inProgress}
                                onChange={(e) => handleInputChange(e, 'projects', index)}
                                className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                              />
                              <span className="text-sm">진행 중</span>
                            </label>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* 자격증 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-medium text-white">자격증</h2>
                        <button
                          type="button"
                          onClick={() => addArrayItem('certifications')}
                          className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                        >
                          + 자격증 추가
                        </button>
                      </div>
                      {formData.certifications.map((cert, index) => (
                        <div key={index} className="mb-6 last:mb-0">
                          <div className="flex justify-between items-center mb-3">
                            <h3 className="text-lg font-medium text-gray-300">자격증 {index + 1}</h3>
                            <button
                              type="button"
                              onClick={() => removeArrayItem('certifications', index)}
                              className="text-gray-400 hover:text-white"
                            >
                              삭제
                            </button>
                          </div>
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">자격증명</label>
                              <input
                                type="text"
                                name="certificationName"
                                value={cert.certificationName}
                                onChange={(e) => handleInputChange(e, 'certifications', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">발급기관</label>
                              <input
                                type="text"
                                name="issuingOrganization"
                                value={cert.issuingOrganization}
                                onChange={(e) => handleInputChange(e, 'certifications', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">취득일</label>
                              <input
                                type="date"
                                name="acquiredDate"
                                value={cert.acquiredDate}
                                onChange={(e) => handleInputChange(e, 'certifications', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">만료일</label>
                              <input
                                type="date"
                                name="expiryDate"
                                value={cert.expiryDate}
                                onChange={(e) => handleInputChange(e, 'certifications', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                disabled={cert.noExpiry}
                              />
                            </div>
                          </div>
                          <div className="mt-4">
                            <label className="flex items-center space-x-2 text-gray-300">
                              <input
                                type="checkbox"
                                name="noExpiry"
                                checked={cert.noExpiry}
                                onChange={(e) => handleInputChange(e, 'certifications', index)}
                                className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                              />
                              <span className="text-sm">만료일 없음</span>
                            </label>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* 경력사항 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-medium text-white">경력사항</h2>
                        <button
                          type="button"
                          onClick={() => addArrayItem('workExperiences')}
                          className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                        >
                          + 경력 추가
                        </button>
                      </div>
                      {formData.workExperiences.map((work, index) => (
                        <div key={index} className="mb-6 last:mb-0">
                          <div className="flex justify-between items-center mb-3">
                            <h3 className="text-lg font-medium text-gray-300">경력 {index + 1}</h3>
                            <button
                              type="button"
                              onClick={() => removeArrayItem('workExperiences', index)}
                              className="text-gray-400 hover:text-white"
                            >
                              삭제
                            </button>
                          </div>
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">회사명</label>
                              <input
                                type="text"
                                name="companyName"
                                value={work.companyName}
                                onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">직위</label>
                              <input
                                type="text"
                                name="position"
                                value={work.position}
                                onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">부서</label>
                              <input
                                type="text"
                                name="department"
                                value={work.department}
                                onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">근무지</label>
                              <input
                                type="text"
                                name="location"
                                value={work.location}
                                onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">시작일</label>
                              <input
                                type="date"
                                name="startDate"
                                value={work.startDate}
                                onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">종료일</label>
                              <input
                                type="date"
                                name="endDate"
                                value={work.endDate}
                                onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                disabled={work.currentlyWorking}
                              />
                            </div>
                          </div>
                          <div className="mt-4">
                            <label className="flex items-center space-x-2 text-gray-300">
                              <input
                                type="checkbox"
                                name="currentlyWorking"
                                checked={work.currentlyWorking}
                                onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                                className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                              />
                              <span className="text-sm">현재 근무 중</span>
                            </label>
                          </div>
                          <div className="mt-4">
                            <label className="block text-sm font-medium mb-2 text-gray-300">주요 업무</label>
                            <textarea
                              name="responsibilities"
                              value={work.responsibilities}
                              onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                              className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              rows="3"
                              required
                            />
                          </div>
                          <div className="mt-4">
                            <label className="block text-sm font-medium mb-2 text-gray-300">주요 성과</label>
                            <textarea
                              name="achievements"
                              value={work.achievements}
                              onChange={(e) => handleInputChange(e, 'workExperiences', index)}
                              className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              rows="3"
                            />
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* 학력사항 */}
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                      <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-medium text-white">학력사항</h2>
                        <button
                          type="button"
                          onClick={() => addArrayItem('educations')}
                          className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                        >
                          + 학력 추가
                        </button>
                      </div>
                      {formData.educations.map((edu, index) => (
                        <div key={index} className="mb-6 last:mb-0">
                          <div className="flex justify-between items-center mb-3">
                            <h3 className="text-lg font-medium text-gray-300">학력 {index + 1}</h3>
                            <button
                              type="button"
                              onClick={() => removeArrayItem('educations', index)}
                              className="text-gray-400 hover:text-white"
                            >
                              삭제
                            </button>
                          </div>
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">학교 구분</label>
                              <select
                                name="schoolType"
                                value={edu.schoolType}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              >
                                <option value="">선택하세요</option>
                                <option value="고등학교">고등학교</option>
                                <option value="전문대학">전문대학</option>
                                <option value="대학교">대학교</option>
                                <option value="대학원">대학원</option>
                              </select>
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">학교명</label>
                              <input
                                type="text"
                                name="schoolName"
                                value={edu.schoolName}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">위치</label>
                              <input
                                type="text"
                                name="location"
                                value={edu.location}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">전공</label>
                              <input
                                type="text"
                                name="major"
                                value={edu.major}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">입학일</label>
                              <input
                                type="date"
                                name="enrollmentDate"
                                value={edu.enrollmentDate}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                required
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">졸업일</label>
                              <input
                                type="date"
                                name="graduationDate"
                                value={edu.graduationDate}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                disabled={edu.inProgress}
                              />
                            </div>
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">학점</label>
                              <input
                                type="text"
                                name="gpa"
                                value={edu.gpa}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                placeholder="예: 4.0/4.5"
                              />
                            </div>
                          </div>
                          <div className="mt-4">
                            <label className="flex items-center space-x-2 text-gray-300">
                              <input
                                type="checkbox"
                                name="inProgress"
                                checked={edu.inProgress}
                                onChange={(e) => handleInputChange(e, 'educations', index)}
                                className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                              />
                              <span className="text-sm">재학 중</span>
                            </label>
                          </div>
                        </div>
                      ))}
                    </div>

                    <div className="flex justify-end space-x-4">
                      <button
                        type="submit"
                        className="btn text-white bg-purple-600 hover:bg-purple-700"
                      >
                        이력서 제출
                      </button>
                    </div>
                  </form>
                )}
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}

export default Resume;