import React, { useState } from 'react';
import Header from '../partials/Header';
import Footer from '../partials/Footer';

function Resume() {
  const [formData, setFormData] = useState({
    // 기본 정보
    name: '',
    email: '',
    phone: '',
    github: '',
    blog: '',
    
    // 학력
    educations: [{
      id: 1,
      schoolType: '',
      schoolName: '',
      location: '',
      major: '',
      degree: '',
      startDate: '',
      endDate: '',
      gpa: '',
      isGraduated: true
    }],
    
    // 경력
    experiences: [{
      id: 1,
      companyName: '',
      position: '',
      location: '',
      startDate: '',
      endDate: '',
      isCurrent: false,
      description: '',
      achievements: '',
      skills: ''
    }],
    
    // 기술 스택
    skills: {
      languages: [],
      frameworks: [],
      databases: [],
      cloud: [],
      tools: [],
      others: []
    },
    
    // 자격증
    certificates: [{
      id: 1,
      name: '',
      issuer: '',
      date: '',
      expiryDate: '',
      credentialId: '',
      isExpired: false
    }],
    
    // 프로젝트
    projects: [{
      id: 1,
      name: '',
      startDate: '',
      endDate: '',
      isCurrent: false,
      role: '',
      description: '',
      technologies: '',
      achievements: '',
      link: ''
    }],
    
    // 자기소개
    introduction: {
      motivation: '',
      goals: '',
      strengths: '',
      personality: ''
    }
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    // TODO: API 연동
    console.log('Form submitted:', formData);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleEducationChange = (id, field, value) => {
    setFormData(prev => ({
      ...prev,
      educations: prev.educations.map(edu => 
        edu.id === id ? { ...edu, [field]: value } : edu
      )
    }));
  };

  const addEducation = () => {
    setFormData(prev => ({
      ...prev,
      educations: [
        ...prev.educations,
        {
          id: prev.educations.length + 1,
          schoolType: '',
          schoolName: '',
          location: '',
          major: '',
          degree: '',
          startDate: '',
          endDate: '',
          gpa: '',
          isGraduated: true
        }
      ]
    }));
  };

  const removeEducation = (id) => {
    setFormData(prev => ({
      ...prev,
      educations: prev.educations.filter(edu => edu.id !== id)
    }));
  };

  const handleExperienceChange = (id, field, value) => {
    setFormData(prev => ({
      ...prev,
      experiences: prev.experiences.map(exp => 
        exp.id === id ? { ...exp, [field]: value } : exp
      )
    }));
  };

  const addExperience = () => {
    setFormData(prev => ({
      ...prev,
      experiences: [
        ...prev.experiences,
        {
          id: prev.experiences.length + 1,
          companyName: '',
          position: '',
          location: '',
          startDate: '',
          endDate: '',
          isCurrent: false,
          description: '',
          achievements: '',
          skills: ''
        }
      ]
    }));
  };

  const removeExperience = (id) => {
    setFormData(prev => ({
      ...prev,
      experiences: prev.experiences.filter(exp => exp.id !== id)
    }));
  };

  const getSchoolTypeOptions = () => {
    return [
      { value: 'high', label: '고등학교' },
      { value: 'college2', label: '2년제 대학' },
      { value: 'college3', label: '3년제 대학' },
      { value: 'college4', label: '4년제 대학' },
      { value: 'master', label: '대학원(석사)' },
      { value: 'phd', label: '대학원(박사)' }
    ];
  };

  const getDegreeOptions = (schoolType) => {
    switch(schoolType) {
      case 'high':
        return [];
      case 'college2':
      case 'college3':
      case 'college4':
        return [
          { value: 'associate', label: '전문학사' },
          { value: 'bachelor', label: '학사' }
        ];
      case 'master':
        return [
          { value: 'master', label: '석사' }
        ];
      case 'phd':
        return [
          { value: 'phd', label: '박사' }
        ];
      default:
        return [];
    }
  };

  const handleSkillChange = (category, value) => {
    setFormData(prev => ({
      ...prev,
      skills: {
        ...prev.skills,
        [category]: value.split(',').map(skill => skill.trim()).filter(skill => skill)
      }
    }));
  };

  const handleCertificateChange = (id, field, value) => {
    setFormData(prev => ({
      ...prev,
      certificates: prev.certificates.map(cert => 
        cert.id === id ? { ...cert, [field]: value } : cert
      )
    }));
  };

  const addCertificate = () => {
    setFormData(prev => ({
      ...prev,
      certificates: [
        ...prev.certificates,
        {
          id: prev.certificates.length + 1,
          name: '',
          issuer: '',
          date: '',
          expiryDate: '',
          credentialId: '',
          isExpired: false
        }
      ]
    }));
  };

  const removeCertificate = (id) => {
    setFormData(prev => ({
      ...prev,
      certificates: prev.certificates.filter(cert => cert.id !== id)
    }));
  };

  const handleProjectChange = (id, field, value) => {
    setFormData(prev => ({
      ...prev,
      projects: prev.projects.map(project => 
        project.id === id ? { ...project, [field]: value } : project
      )
    }));
  };

  const addProject = () => {
    setFormData(prev => ({
      ...prev,
      projects: [
        ...prev.projects,
        {
          id: prev.projects.length + 1,
          name: '',
          startDate: '',
          endDate: '',
          isCurrent: false,
          role: '',
          description: '',
          technologies: '',
          achievements: '',
          link: ''
        }
      ]
    }));
  };

  const removeProject = (id) => {
    setFormData(prev => ({
      ...prev,
      projects: prev.projects.filter(project => project.id !== id)
    }));
  };

  const handleIntroductionChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      introduction: {
        ...prev.introduction,
        [field]: value
      }
    }));
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
                
                <form onSubmit={handleSubmit} className="space-y-8">
                  {/* 기본 정보 섹션 */}
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <h2 className="text-xl font-medium mb-4 text-white">기본 정보</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300" htmlFor="name">
                          이름
                        </label>
                        <input
                          type="text"
                          id="name"
                          name="name"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={formData.name}
                          onChange={handleChange}
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300" htmlFor="email">
                          이메일
                        </label>
                        <input
                          type="email"
                          id="email"
                          name="email"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={formData.email}
                          onChange={handleChange}
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300" htmlFor="phone">
                          연락처
                        </label>
                        <input
                          type="tel"
                          id="phone"
                          name="phone"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={formData.phone}
                          onChange={handleChange}
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300" htmlFor="github">
                          GitHub
                        </label>
                        <input
                          type="url"
                          id="github"
                          name="github"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={formData.github}
                          onChange={handleChange}
                          placeholder="https://github.com/username"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300" htmlFor="blog">
                          블로그/포트폴리오
                        </label>
                        <input
                          type="url"
                          id="blog"
                          name="blog"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          value={formData.blog}
                          onChange={handleChange}
                          placeholder="https://blog.com"
                        />
                      </div>
                    </div>
                  </div>

                  {/* 학력 섹션 */}
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <div className="flex justify-between items-center mb-4">
                      <h2 className="text-xl font-medium text-white">학력</h2>
                      <button
                        type="button"
                        onClick={addEducation}
                        className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                      >
                        + 학력 추가
                      </button>
                    </div>
                    
                    {formData.educations.map((education, index) => (
                      <div key={education.id} className="mb-6 last:mb-0">
                        <div className="flex justify-between items-center mb-3">
                          <h3 className="text-lg font-medium text-gray-300">학력 {index + 1}</h3>
                          {formData.educations.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeEducation(education.id)}
                              className="text-gray-400 hover:text-red-400"
                            >
                              삭제
                            </button>
                          )}
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              학교 종류
                            </label>
                            <select
                              className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              value={education.schoolType}
                              onChange={(e) => handleEducationChange(education.id, 'schoolType', e.target.value)}
                            >
                              <option value="">선택해주세요</option>
                              {getSchoolTypeOptions().map(option => (
                                <option key={option.value} value={option.value}>
                                  {option.label}
                                </option>
                              ))}
                            </select>
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              학교명
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="학교명을 입력해주세요"
                              value={education.schoolName}
                              onChange={(e) => handleEducationChange(education.id, 'schoolName', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              지역
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="학교 위치 (예: 서울시 강남구)"
                              value={education.location}
                              onChange={(e) => handleEducationChange(education.id, 'location', e.target.value)}
                            />
                          </div>
                          {education.schoolType !== 'high' && (
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">
                                전공
                              </label>
                              <input
                                type="text"
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                placeholder="전공을 입력해주세요"
                                value={education.major}
                                onChange={(e) => handleEducationChange(education.id, 'major', e.target.value)}
                              />
                            </div>
                          )}
                          {getDegreeOptions(education.schoolType).length > 0 && (
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">
                                학위
                              </label>
                              <select
                                className="form-select w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                value={education.degree}
                                onChange={(e) => handleEducationChange(education.id, 'degree', e.target.value)}
                              >
                                <option value="">선택해주세요</option>
                                {getDegreeOptions(education.schoolType).map(option => (
                                  <option key={option.value} value={option.value}>
                                    {option.label}
                                  </option>
                                ))}
                              </select>
                            </div>
                          )}
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              입학일
                            </label>
                            <input
                              type="date"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              value={education.startDate}
                              onChange={(e) => handleEducationChange(education.id, 'startDate', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              {education.isGraduated ? '졸업일' : '재학중'}
                            </label>
                            <div className="flex items-center space-x-2">
                              <input
                                type="date"
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                value={education.endDate}
                                onChange={(e) => handleEducationChange(education.id, 'endDate', e.target.value)}
                                disabled={!education.isGraduated}
                              />
                              <label className="flex items-center space-x-2 text-gray-300">
                                <input
                                  type="checkbox"
                                  className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                                  checked={!education.isGraduated}
                                  onChange={(e) => handleEducationChange(education.id, 'isGraduated', !e.target.checked)}
                                />
                                <span className="text-sm">재학중</span>
                              </label>
                            </div>
                          </div>
                          {education.schoolType !== 'high' && (
                            <div>
                              <label className="block text-sm font-medium mb-2 text-gray-300">
                                학점
                              </label>
                              <input
                                type="text"
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                placeholder="학점을 입력해주세요 (예: 4.0/4.5)"
                                value={education.gpa}
                                onChange={(e) => handleEducationChange(education.id, 'gpa', e.target.value)}
                              />
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* 경력 섹션 */}
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <div className="flex justify-between items-center mb-4">
                      <h2 className="text-xl font-medium text-white">경력</h2>
                      <button
                        type="button"
                        onClick={addExperience}
                        className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                      >
                        + 경력 추가
                      </button>
                    </div>
                    
                    {formData.experiences.map((experience, index) => (
                      <div key={experience.id} className="mb-6 last:mb-0">
                        <div className="flex justify-between items-center mb-3">
                          <h3 className="text-lg font-medium text-gray-300">경력 {index + 1}</h3>
                          {formData.experiences.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeExperience(experience.id)}
                              className="text-gray-400 hover:text-red-400"
                            >
                              삭제
                            </button>
                          )}
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              회사명
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="회사명을 입력해주세요"
                              value={experience.companyName}
                              onChange={(e) => handleExperienceChange(experience.id, 'companyName', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              직무
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="담당 직무를 입력해주세요"
                              value={experience.position}
                              onChange={(e) => handleExperienceChange(experience.id, 'position', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              근무지
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="근무지역을 입력해주세요"
                              value={experience.location}
                              onChange={(e) => handleExperienceChange(experience.id, 'location', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              입사일
                            </label>
                            <input
                              type="date"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              value={experience.startDate}
                              onChange={(e) => handleExperienceChange(experience.id, 'startDate', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              {experience.isCurrent ? '재직중' : '퇴사일'}
                            </label>
                            <div className="flex items-center space-x-2">
                              <input
                                type="date"
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                value={experience.endDate}
                                onChange={(e) => handleExperienceChange(experience.id, 'endDate', e.target.value)}
                                disabled={experience.isCurrent}
                              />
                              <label className="flex items-center space-x-2 text-gray-300">
                                <input
                                  type="checkbox"
                                  className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                                  checked={experience.isCurrent}
                                  onChange={(e) => handleExperienceChange(experience.id, 'isCurrent', e.target.checked)}
                                />
                                <span className="text-sm">재직중</span>
                              </label>
                            </div>
                          </div>
                          <div className="md:col-span-2">
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              주요 업무
                            </label>
                            <textarea
                              className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              rows="3"
                              placeholder="담당했던 주요 업무를 입력해주세요"
                              value={experience.description}
                              onChange={(e) => handleExperienceChange(experience.id, 'description', e.target.value)}
                            />
                          </div>
                          <div className="md:col-span-2">
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              주요 성과
                            </label>
                            <textarea
                              className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              rows="3"
                              placeholder="주요 성과나 기여도를 입력해주세요"
                              value={experience.achievements}
                              onChange={(e) => handleExperienceChange(experience.id, 'achievements', e.target.value)}
                            />
                          </div>
                          <div className="md:col-span-2">
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              사용 기술
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="사용한 기술 스택을 입력해주세요 (쉼표로 구분)"
                              value={experience.skills}
                              onChange={(e) => handleExperienceChange(experience.id, 'skills', e.target.value)}
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* 기술 스택 섹션 */}
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <h2 className="text-xl font-medium mb-4 text-white">기술 스택</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          프로그래밍 언어
                        </label>
                        <input
                          type="text"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          placeholder="사용 가능한 프로그래밍 언어 (쉼표로 구분)"
                          value={formData.skills.languages.join(', ')}
                          onChange={(e) => handleSkillChange('languages', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          프레임워크/라이브러리
                        </label>
                        <input
                          type="text"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          placeholder="사용 가능한 프레임워크/라이브러리 (쉼표로 구분)"
                          value={formData.skills.frameworks.join(', ')}
                          onChange={(e) => handleSkillChange('frameworks', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          데이터베이스
                        </label>
                        <input
                          type="text"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          placeholder="사용 가능한 데이터베이스 (쉼표로 구분)"
                          value={formData.skills.databases.join(', ')}
                          onChange={(e) => handleSkillChange('databases', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          클라우드/인프라
                        </label>
                        <input
                          type="text"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          placeholder="사용 가능한 클라우드/인프라 (쉼표로 구분)"
                          value={formData.skills.cloud.join(', ')}
                          onChange={(e) => handleSkillChange('cloud', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          개발 도구
                        </label>
                        <input
                          type="text"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          placeholder="사용 가능한 개발 도구 (쉼표로 구분)"
                          value={formData.skills.tools.join(', ')}
                          onChange={(e) => handleSkillChange('tools', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          기타 기술
                        </label>
                        <input
                          type="text"
                          className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          placeholder="기타 보유 기술 (쉼표로 구분)"
                          value={formData.skills.others.join(', ')}
                          onChange={(e) => handleSkillChange('others', e.target.value)}
                        />
                      </div>
                    </div>
                  </div>

                  {/* 자격증 섹션 */}
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <div className="flex justify-between items-center mb-4">
                      <h2 className="text-xl font-medium text-white">자격증</h2>
                      <button
                        type="button"
                        onClick={addCertificate}
                        className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                      >
                        + 자격증 추가
                      </button>
                    </div>
                    
                    {formData.certificates.map((certificate, index) => (
                      <div key={certificate.id} className="mb-6 last:mb-0">
                        <div className="flex justify-between items-center mb-3">
                          <h3 className="text-lg font-medium text-gray-300">자격증 {index + 1}</h3>
                          {formData.certificates.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeCertificate(certificate.id)}
                              className="text-gray-400 hover:text-red-400"
                            >
                              삭제
                            </button>
                          )}
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              자격증명
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="자격증명을 입력해주세요"
                              value={certificate.name}
                              onChange={(e) => handleCertificateChange(certificate.id, 'name', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              발행처
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="자격증 발행처를 입력해주세요"
                              value={certificate.issuer}
                              onChange={(e) => handleCertificateChange(certificate.id, 'issuer', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              취득일
                            </label>
                            <input
                              type="date"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              value={certificate.date}
                              onChange={(e) => handleCertificateChange(certificate.id, 'date', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              {certificate.isExpired ? '만료일' : '유효기간'}
                            </label>
                            <div className="flex items-center space-x-2">
                              <input
                                type="date"
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                value={certificate.expiryDate}
                                onChange={(e) => handleCertificateChange(certificate.id, 'expiryDate', e.target.value)}
                                disabled={!certificate.isExpired}
                              />
                              <label className="flex items-center space-x-2 text-gray-300">
                                <input
                                  type="checkbox"
                                  className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                                  checked={certificate.isExpired}
                                  onChange={(e) => handleCertificateChange(certificate.id, 'isExpired', e.target.checked)}
                                />
                                <span className="text-sm">만료됨</span>
                              </label>
                            </div>
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              자격증 번호
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="자격증 번호를 입력해주세요"
                              value={certificate.credentialId}
                              onChange={(e) => handleCertificateChange(certificate.id, 'credentialId', e.target.value)}
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* 프로젝트 섹션 */}
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <div className="flex justify-between items-center mb-4">
                      <h2 className="text-xl font-medium text-white">프로젝트</h2>
                      <button
                        type="button"
                        onClick={addProject}
                        className="btn-sm text-white bg-purple-600 hover:bg-purple-700"
                      >
                        + 프로젝트 추가
                      </button>
                    </div>
                    
                    {formData.projects.map((project, index) => (
                      <div key={project.id} className="mb-6 last:mb-0">
                        <div className="flex justify-between items-center mb-3">
                          <h3 className="text-lg font-medium text-gray-300">프로젝트 {index + 1}</h3>
                          {formData.projects.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeProject(project.id)}
                              className="text-gray-400 hover:text-red-400"
                            >
                              삭제
                            </button>
                          )}
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              프로젝트명
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="프로젝트명을 입력해주세요"
                              value={project.name}
                              onChange={(e) => handleProjectChange(project.id, 'name', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              역할
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="프로젝트에서의 역할을 입력해주세요"
                              value={project.role}
                              onChange={(e) => handleProjectChange(project.id, 'role', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              시작일
                            </label>
                            <input
                              type="date"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              value={project.startDate}
                              onChange={(e) => handleProjectChange(project.id, 'startDate', e.target.value)}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              {project.isCurrent ? '진행중' : '종료일'}
                            </label>
                            <div className="flex items-center space-x-2">
                              <input
                                type="date"
                                className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                                value={project.endDate}
                                onChange={(e) => handleProjectChange(project.id, 'endDate', e.target.value)}
                                disabled={project.isCurrent}
                              />
                              <label className="flex items-center space-x-2 text-gray-300">
                                <input
                                  type="checkbox"
                                  className="form-checkbox bg-gray-700/50 border-gray-600 text-purple-500 focus:ring-purple-500"
                                  checked={project.isCurrent}
                                  onChange={(e) => handleProjectChange(project.id, 'isCurrent', e.target.checked)}
                                />
                                <span className="text-sm">진행중</span>
                              </label>
                            </div>
                          </div>
                          <div className="md:col-span-2">
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              프로젝트 설명
                            </label>
                            <textarea
                              className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              rows="3"
                              placeholder="프로젝트에 대한 설명을 입력해주세요"
                              value={project.description}
                              onChange={(e) => handleProjectChange(project.id, 'description', e.target.value)}
                            />
                          </div>
                          <div className="md:col-span-2">
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              사용 기술
                            </label>
                            <input
                              type="text"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="사용한 기술 스택을 입력해주세요 (쉼표로 구분)"
                              value={project.technologies}
                              onChange={(e) => handleProjectChange(project.id, 'technologies', e.target.value)}
                            />
                          </div>
                          <div className="md:col-span-2">
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              주요 성과
                            </label>
                            <textarea
                              className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              rows="3"
                              placeholder="프로젝트의 주요 성과를 입력해주세요"
                              value={project.achievements}
                              onChange={(e) => handleProjectChange(project.id, 'achievements', e.target.value)}
                            />
                          </div>
                          <div className="md:col-span-2">
                            <label className="block text-sm font-medium mb-2 text-gray-300">
                              프로젝트 링크
                            </label>
                            <input
                              type="url"
                              className="form-input w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                              placeholder="GitHub, 배포 URL 등을 입력해주세요"
                              value={project.link}
                              onChange={(e) => handleProjectChange(project.id, 'link', e.target.value)}
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* 자기소개 섹션 */}
                  <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700/50 shadow-lg">
                    <h2 className="text-xl font-medium mb-4 text-white">자기소개</h2>
                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          지원 동기
                        </label>
                        <textarea
                          className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          rows="3"
                          placeholder="지원 동기를 입력해주세요"
                          value={formData.introduction.motivation}
                          onChange={(e) => handleIntroductionChange('motivation', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          목표
                        </label>
                        <textarea
                          className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          rows="3"
                          placeholder="향후 목표를 입력해주세요"
                          value={formData.introduction.goals}
                          onChange={(e) => handleIntroductionChange('goals', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          강점
                        </label>
                        <textarea
                          className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          rows="3"
                          placeholder="본인의 강점을 입력해주세요"
                          value={formData.introduction.strengths}
                          onChange={(e) => handleIntroductionChange('strengths', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2 text-gray-300">
                          성격/특징
                        </label>
                        <textarea
                          className="form-textarea w-full bg-gray-700/50 border-gray-600 text-white placeholder-gray-400 focus:border-purple-500 focus:ring-purple-500"
                          rows="3"
                          placeholder="본인의 성격이나 특징을 입력해주세요"
                          value={formData.introduction.personality}
                          onChange={(e) => handleIntroductionChange('personality', e.target.value)}
                        />
                      </div>
                    </div>
                  </div>

                  {/* 버튼 영역 */}
                  <div className="flex justify-end space-x-4">
                    <button
                      type="button"
                      className="btn text-gray-300 bg-gray-700/50 hover:bg-gray-700 border border-gray-600"
                      onClick={() => {
                        // TODO: 임시 저장 기능 구현
                        console.log('임시 저장');
                      }}
                    >
                      임시 저장
                    </button>
                    <button
                      type="submit"
                      className="btn text-white bg-purple-600 hover:bg-purple-700"
                    >
                      저장하기
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