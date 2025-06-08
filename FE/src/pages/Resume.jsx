import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import Header from '../partials/Header';
import Footer from '../partials/Footer';

function Resume() {

  const navigate = useNavigate();
  const location = useLocation();
  const [userId, setUserId] = useState('');
  const storedUserId = localStorage.getItem('userId');
  const token = localStorage.getItem('token');

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

    console.log('토큰resume:', token);
    console.log('유저아이디resume:', storedUserId)
    try {
      // 여러 방식으로 userId를 꺼내보기
      let id = storedUserId;
      if (!id) {
        // user 객체로 저장된 경우
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

      // API 스펙에 맞게 데이터 구조 변환
      const resumeData = {
        title: formData.title,
        content: formData.content,
        objective: formData.objective,
        skills: formData.skills,
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

      // API 호출
      const response = await fetch(`https://api.interv.swote.dev/api/resumes?id=${id}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'accept': '*/*',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(resumeData)
      });
      console.log('토큰22:', token);
      // 서버 오류 처리
      if (!response.ok) {
        const errorText = await response.text();
        let errorMessage;
        try {
          const errorData = JSON.parse(errorText);
          errorMessage = errorData.message || '이력서 제출에 실패했습니다.';
        } catch {
          errorMessage = errorText || '이력서 제출에 실패했습니다.';
        }
        throw new Error(errorMessage);
      }


      console.log(localStorage.getItem('token'));
      // 정상 응답 처리
      alert('이력서가 성공적으로 제출되었습니다.');
      navigate('/resume');
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
                <h1 className="h2 mb-8">이력서 작성</h1>

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