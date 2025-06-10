package dev.swote.interv.service.resume;

import dev.swote.interv.domain.resume.dto.*;
import dev.swote.interv.domain.resume.entity.*;
import dev.swote.interv.domain.resume.mapper.ResumeMapper;
import dev.swote.interv.domain.resume.repository.*;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.exception.DuplicateResourceException;
import dev.swote.interv.exception.ResourceNotFoundException;
import dev.swote.interv.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeProjectRepository projectRepository;
    private final ResumeCertificationRepository certificationRepository;
    private final ResumeWorkExperienceRepository workExperienceRepository;
    private final ResumeEducationRepository educationRepository;
    private final UserRepository userRepository;
    private final ResumeFileService resumeFileService;
    private final ResumeMapper resumeMapper;

    @Transactional(readOnly = true)
    public ResumeResponse getUserResume(Integer userId) {
        log.info("사용자 {}의 이력서 조회", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for user id: " + userId));

        ResumeResponse response = resumeMapper.toResponse(resume);
        log.info("사용자 {}의 이력서 조회 완료 - 제목: {}", userId, response.getTitle());

        return response;
    }

    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(Integer resumeId) {
        log.info("이력서 상세 조회 - ID: {}", resumeId);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        ResumeResponse response = resumeMapper.toResponse(resume);
        log.info("이력서 상세 조회 완료 - ID: {}, 제목: {}", resumeId, response.getTitle());

        return response;
    }

    @Transactional(readOnly = true)
    public boolean existsUserResume(Integer userId) {
        log.debug("사용자 {}의 이력서 존재 여부 확인", userId);
        return resumeRepository.existsByUserId(userId);
    }

    @Transactional
    public ResumeResponse createResume(Integer userId, CreateResumeRequest request) {
        log.info("이력서 생성 - 사용자 ID: {}, 제목: {}", userId, request.getTitle());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // 이미 이력서가 존재하는지 확인
        if (resumeRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Resume already exists for user id: " + userId);
        }

        // Resume 엔티티 생성
        Resume resume = resumeMapper.toEntity(request, user);
        resume = resumeRepository.save(resume);

        // 자식 엔티티들 생성
        createChildEntities(resume, request);

        // 응답 생성
        ResumeResponse response = resumeMapper.toResponse(resume);
        log.info("이력서 생성 완료 - ID: {}, 제목: {}", response.getId(), response.getTitle());

        return response;
    }

    @Transactional
    public ResumeResponse uploadResumeFile(Integer userId, MultipartFile file, String title) throws IOException {
        log.info("이력서 파일 업로드 - 사용자 ID: {}, 파일명: {}, 제목: {}", userId, file.getOriginalFilename(), title);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // 이미 이력서가 존재하는지 확인
        if (resumeRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Resume already exists for user id: " + userId);
        }

        try {
            String content = resumeFileService.extractContent(file);
            String filePath = resumeFileService.storeFile(file);

            Resume resume = Resume.builder()
                    .user(user)
                    .title(title)
                    .content(content)
                    .filePath(filePath)
                    .status(ResumeStatus.ACTIVE)
                    .build();

            resume = resumeRepository.save(resume);

            ResumeResponse response = resumeMapper.toResponse(resume);
            log.info("이력서 파일 업로드 완료 - ID: {}, 파일: {}", response.getId(), file.getOriginalFilename());

            return response;
        } catch (Exception e) {
            log.error("이력서 파일 업로드 실패 - 사용자 ID: {}, 파일: {}, 오류: {}", userId, file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("파일 업로드에 실패했습니다", e);
        }
    }

    @Transactional
    public ResumeResponse updateUserResume(Integer userId, UpdateResumeRequest request) {
        log.info("사용자 {}의 이력서 수정 - 제목: {}", userId, request.getTitle());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for user id: " + userId));

        // Resume 기본 정보 업데이트
        resumeMapper.updateEntity(resume, request);

        // 먼저 Resume을 저장하고 flush하여 DB에 확실히 반영
        resume = resumeRepository.saveAndFlush(resume);

        log.debug("Resume 저장 완료 - ID: {}", resume.getId());

        // 자식 엔티티들 업데이트
        updateChildEntities(resume, request);

        ResumeResponse response = resumeMapper.toResponse(resume);
        log.info("이력서 수정 완료 - 사용자 ID: {}, 제목: {}", userId, response.getTitle());

        return response;
    }

    @Transactional
    public void deleteUserResume(Integer userId) {
        log.info("사용자 {}의 이력서 삭제", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for user id: " + userId));

        resume.delete();
        resumeRepository.save(resume);

        log.info("사용자 {}의 이력서 삭제 완료", userId);
    }

    // 기존 updateResume과 deleteResume 메서드도 유지 (ID로 직접 접근하는 경우)
    @Transactional
    public ResumeResponse updateResume(Integer resumeId, UpdateResumeRequest request) {
        log.info("이력서 수정 - ID: {}, 제목: {}", resumeId, request.getTitle());

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        // Resume 기본 정보 업데이트
        resumeMapper.updateEntity(resume, request);
        resume = resumeRepository.save(resume);

        // 자식 엔티티들 업데이트
        updateChildEntities(resume, request);

        ResumeResponse response = resumeMapper.toResponse(resume);
        log.info("이력서 수정 완료 - ID: {}, 제목: {}", resumeId, response.getTitle());

        return response;
    }

    @Transactional
    public void deleteResume(Integer resumeId) {
        log.info("이력서 삭제 - ID: {}", resumeId);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        resume.delete();
        resumeRepository.save(resume);

        log.info("이력서 삭제 완료 - ID: {}", resumeId);
    }

    // 자식 엔티티들 생성 (기존 메서드 유지)
    private void createChildEntities(Resume resume, CreateResumeRequest request) {
        createChildEntities(resume,
                request.getProjects(),
                request.getCertifications(),
                request.getWorkExperiences(),
                request.getEducations());
    }

    private void createChildEntities(Resume resume,
                                     List<ResumeProjectRequest> projects,
                                     List<ResumeCertificationRequest> certifications,
                                     List<ResumeWorkExperienceRequest> workExperiences,
                                     List<ResumeEducationRequest> educations) {

        // 프로젝트 생성
        if (projects != null) {
            List<ResumeProject> projectEntities = projects.stream()
                    .map(projectRequest -> resumeMapper.toProjectEntity(projectRequest, resume))
                    .collect(Collectors.toList());
            projectRepository.saveAll(projectEntities);
            log.debug("프로젝트 {}개 생성 완료", projectEntities.size());
        }

        // 자격증 생성
        if (certifications != null) {
            List<ResumeCertification> certificationEntities = certifications.stream()
                    .map(certRequest -> resumeMapper.toCertificationEntity(certRequest, resume))
                    .collect(Collectors.toList());
            certificationRepository.saveAll(certificationEntities);
            log.debug("자격증 {}개 생성 완료", certificationEntities.size());
        }

        // 경력 생성
        if (workExperiences != null) {
            List<ResumeWorkExperience> workExperienceEntities = workExperiences.stream()
                    .map(workRequest -> resumeMapper.toWorkExperienceEntity(workRequest, resume))
                    .collect(Collectors.toList());
            workExperienceRepository.saveAll(workExperienceEntities);
            log.debug("경력 {}개 생성 완료", workExperienceEntities.size());
        }

        // 학력 생성
        if (educations != null) {
            List<ResumeEducation> educationEntities = educations.stream()
                    .map(eduRequest -> resumeMapper.toEducationEntity(eduRequest, resume))
                    .collect(Collectors.toList());
            educationRepository.saveAll(educationEntities);
            log.debug("학력 {}개 생성 완료", educationEntities.size());
        }
    }

    private void updateChildEntities(Resume resume, UpdateResumeRequest request) {
        log.debug("자식 엔티티들 업데이트 시작 - Resume ID: {}", resume.getId());

        updateProjects(resume, request.getProjects());
        updateCertifications(resume, request.getCertifications());
        updateWorkExperiences(resume, request.getWorkExperiences());
        updateEducations(resume, request.getEducations());

        log.debug("자식 엔티티들 업데이트 완료 - Resume ID: {}", resume.getId());
    }

    private void updateProjects(Resume resume, List<ResumeProjectRequest> projectRequests) {
        if (resume.getId() == null) {
            throw new IllegalStateException("Resume ID cannot be null when updating projects");
        }

        log.debug("프로젝트 업데이트 시작 - Resume ID: {}", resume.getId());

        List<ResumeProject> existingProjects = projectRepository.findByResumeId(resume.getId());
        Map<Integer, ResumeProject> existingProjectMap = existingProjects.stream()
                .collect(Collectors.toMap(ResumeProject::getId, project -> project));

        List<ResumeProject> toSave = new ArrayList<>();
        List<ResumeProject> toDelete = new ArrayList<>();
        Set<Integer> processedIds = new HashSet<>();

        if (projectRequests != null) {
            for (ResumeProjectRequest request : projectRequests) {
                if (request.getId() != null && existingProjectMap.containsKey(request.getId())) {
                    ResumeProject existingProject = existingProjectMap.get(request.getId());
                    resumeMapper.updateProjectEntity(existingProject, request);
                    toSave.add(existingProject);
                    processedIds.add(request.getId());
                    log.debug("프로젝트 업데이트 - ID: {}, 이름: {}", request.getId(), request.getProjectName());
                } else {
                    ResumeProject newProject = resumeMapper.toProjectEntity(request, resume);
                    newProject.setResume(resume); // 명시적으로 관계 설정
                    toSave.add(newProject);
                    log.debug("새 프로젝트 생성 - 이름: {}, Resume ID: {}", request.getProjectName(), resume.getId());
                }
            }
        }

        toDelete = existingProjects.stream()
                .filter(project -> !processedIds.contains(project.getId()))
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            projectRepository.deleteAll(toDelete);
            projectRepository.flush();
            log.debug("프로젝트 {}개 삭제 완료", toDelete.size());
        }

        if (!toSave.isEmpty()) {
            for (ResumeProject project : toSave) {
                if (project.getResume() == null || project.getResume().getId() == null) {
                    throw new IllegalStateException("Resume relationship not properly set for project");
                }
            }

            projectRepository.saveAll(toSave);
            projectRepository.flush();
            log.debug("프로젝트 {}개 저장 완료", toSave.size());
        }
    }


    private void updateCertifications(Resume resume, List<ResumeCertificationRequest> certificationRequests) {
        // Resume ID 검증
        if (resume.getId() == null) {
            throw new IllegalStateException("Resume ID cannot be null when updating certifications");
        }

        log.debug("자격증 업데이트 시작 - Resume ID: {}", resume.getId());

        List<ResumeCertification> existingCertifications = certificationRepository.findByResumeId(resume.getId());
        Map<Integer, ResumeCertification> existingCertificationMap = existingCertifications.stream()
                .collect(Collectors.toMap(ResumeCertification::getId, cert -> cert));

        List<ResumeCertification> toSave = new ArrayList<>();
        List<ResumeCertification> toDelete = new ArrayList<>();
        Set<Integer> processedIds = new HashSet<>();

        if (certificationRequests != null) {
            for (ResumeCertificationRequest request : certificationRequests) {
                if (request.getId() != null && existingCertificationMap.containsKey(request.getId())) {
                    // 기존 자격증 업데이트
                    ResumeCertification existingCertification = existingCertificationMap.get(request.getId());
                    resumeMapper.updateCertificationEntity(existingCertification, request);
                    toSave.add(existingCertification);
                    processedIds.add(request.getId());
                    log.debug("자격증 업데이트 - ID: {}, 이름: {}", request.getId(), request.getCertificationName());
                } else {
                    // 새 자격증 생성
                    ResumeCertification newCertification = resumeMapper.toCertificationEntity(request, resume);

                    // 명시적으로 resume 관계 설정
                    newCertification.setResume(resume);

                    toSave.add(newCertification);
                    log.debug("새 자격증 생성 - 이름: {}, Resume ID: {}", request.getCertificationName(), resume.getId());
                }
            }
        }

        // 삭제할 자격증 찾기
        toDelete = existingCertifications.stream()
                .filter(certification -> !processedIds.contains(certification.getId()))
                .collect(Collectors.toList());

        // 삭제 먼저 실행
        if (!toDelete.isEmpty()) {
            certificationRepository.deleteAll(toDelete);
            certificationRepository.flush(); // 즉시 DB에 반영
            log.debug("자격증 {}개 삭제 완료", toDelete.size());
        }

        // 저장 실행
        if (!toSave.isEmpty()) {
            // 각 엔티티의 resume 관계가 올바르게 설정되었는지 검증
            for (ResumeCertification cert : toSave) {
                if (cert.getResume() == null || cert.getResume().getId() == null) {
                    log.error("자격증의 Resume 관계가 설정되지 않음 - 자격증: {}", cert.getCertificationName());
                    throw new IllegalStateException("Resume relationship not properly set for certification");
                }
            }

            certificationRepository.saveAll(toSave);
            certificationRepository.flush(); // 즉시 DB에 반영
            log.debug("자격증 {}개 저장 완료", toSave.size());
        }
    }


    private void updateWorkExperiences(Resume resume, List<ResumeWorkExperienceRequest> workExperienceRequests) {
        if (resume.getId() == null) {
            throw new IllegalStateException("Resume ID cannot be null when updating work experiences");
        }

        log.debug("경력 업데이트 시작 - Resume ID: {}", resume.getId());

        List<ResumeWorkExperience> existingWorkExperiences = workExperienceRepository.findByResumeId(resume.getId());
        Map<Integer, ResumeWorkExperience> existingWorkExperienceMap = existingWorkExperiences.stream()
                .collect(Collectors.toMap(ResumeWorkExperience::getId, work -> work));

        List<ResumeWorkExperience> toSave = new ArrayList<>();
        List<ResumeWorkExperience> toDelete = new ArrayList<>();
        Set<Integer> processedIds = new HashSet<>();

        if (workExperienceRequests != null) {
            for (ResumeWorkExperienceRequest request : workExperienceRequests) {
                if (request.getId() != null && existingWorkExperienceMap.containsKey(request.getId())) {
                    ResumeWorkExperience existingWorkExperience = existingWorkExperienceMap.get(request.getId());
                    resumeMapper.updateWorkExperienceEntity(existingWorkExperience, request);
                    toSave.add(existingWorkExperience);
                    processedIds.add(request.getId());
                    log.debug("경력 업데이트 - ID: {}, 회사: {}", request.getId(), request.getCompanyName());
                } else {
                    ResumeWorkExperience newWorkExperience = resumeMapper.toWorkExperienceEntity(request, resume);
                    newWorkExperience.setResume(resume); // 명시적으로 관계 설정
                    toSave.add(newWorkExperience);
                    log.debug("새 경력 생성 - 회사: {}, Resume ID: {}", request.getCompanyName(), resume.getId());
                }
            }
        }

        toDelete = existingWorkExperiences.stream()
                .filter(workExperience -> !processedIds.contains(workExperience.getId()))
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            workExperienceRepository.deleteAll(toDelete);
            workExperienceRepository.flush();
            log.debug("경력 {}개 삭제 완료", toDelete.size());
        }

        if (!toSave.isEmpty()) {
            for (ResumeWorkExperience work : toSave) {
                if (work.getResume() == null || work.getResume().getId() == null) {
                    throw new IllegalStateException("Resume relationship not properly set for work experience");
                }
            }

            workExperienceRepository.saveAll(toSave);
            workExperienceRepository.flush();
            log.debug("경력 {}개 저장 완료", toSave.size());
        }
    }

    private void updateEducations(Resume resume, List<ResumeEducationRequest> educationRequests) {
        if (resume.getId() == null) {
            throw new IllegalStateException("Resume ID cannot be null when updating educations");
        }

        log.debug("학력 업데이트 시작 - Resume ID: {}", resume.getId());

        List<ResumeEducation> existingEducations = educationRepository.findByResumeId(resume.getId());
        Map<Integer, ResumeEducation> existingEducationMap = existingEducations.stream()
                .collect(Collectors.toMap(ResumeEducation::getId, edu -> edu));

        List<ResumeEducation> toSave = new ArrayList<>();
        List<ResumeEducation> toDelete = new ArrayList<>();
        Set<Integer> processedIds = new HashSet<>();

        if (educationRequests != null) {
            for (ResumeEducationRequest request : educationRequests) {
                if (request.getId() != null && existingEducationMap.containsKey(request.getId())) {
                    ResumeEducation existingEducation = existingEducationMap.get(request.getId());
                    resumeMapper.updateEducationEntity(existingEducation, request);
                    toSave.add(existingEducation);
                    processedIds.add(request.getId());
                    log.debug("학력 업데이트 - ID: {}, 학교: {}", request.getId(), request.getSchoolName());
                } else {
                    ResumeEducation newEducation = resumeMapper.toEducationEntity(request, resume);
                    newEducation.setResume(resume); // 명시적으로 관계 설정
                    toSave.add(newEducation);
                    log.debug("새 학력 생성 - 학교: {}, Resume ID: {}", request.getSchoolName(), resume.getId());
                }
            }
        }

        toDelete = existingEducations.stream()
                .filter(education -> !processedIds.contains(education.getId()))
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            educationRepository.deleteAll(toDelete);
            educationRepository.flush();
            log.debug("학력 {}개 삭제 완료", toDelete.size());
        }

        if (!toSave.isEmpty()) {
            for (ResumeEducation education : toSave) {
                if (education.getResume() == null || education.getResume().getId() == null) {
                    throw new IllegalStateException("Resume relationship not properly set for education");
                }
            }

            educationRepository.saveAll(toSave);
            educationRepository.flush();
            log.debug("학력 {}개 저장 완료", toSave.size());
        }
    }
}