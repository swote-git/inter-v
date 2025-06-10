package dev.swote.interv.service.resume;

import dev.swote.interv.domain.resume.dto.*;
import dev.swote.interv.domain.resume.entity.*;
import dev.swote.interv.domain.resume.mapper.ResumeMapper;
import dev.swote.interv.domain.resume.repository.*;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
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
            throw new IllegalStateException("Resume already exists for user id: " + userId);
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
            throw new IllegalStateException("Resume already exists for user id: " + userId);
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
        resume = resumeRepository.save(resume);

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

    // 업데이트 메서드들 (기존과 동일)
    private void updateChildEntities(Resume resume, UpdateResumeRequest request) {
        log.debug("자식 엔티티들 업데이트 시작 - Resume ID: {}", resume.getId());

        updateProjects(resume.getId(), request.getProjects());
        updateCertifications(resume.getId(), request.getCertifications());
        updateWorkExperiences(resume.getId(), request.getWorkExperiences());
        updateEducations(resume.getId(), request.getEducations());

        log.debug("자식 엔티티들 업데이트 완료 - Resume ID: {}", resume.getId());
    }

    private void updateProjects(Integer resumeId, List<ResumeProjectRequest> projectRequests) {
        List<ResumeProject> existingProjects = projectRepository.findByResumeId(resumeId);
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
                    Resume resume = resumeRepository.findById(resumeId).orElseThrow();
                    ResumeProject newProject = resumeMapper.toProjectEntity(request, resume);
                    toSave.add(newProject);
                    log.debug("새 프로젝트 생성 - 이름: {}", request.getProjectName());
                }
            }
        }

        toDelete = existingProjects.stream()
                .filter(project -> !processedIds.contains(project.getId()))
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            projectRepository.saveAll(toSave);
            log.debug("프로젝트 {}개 저장 완료", toSave.size());
        }
        if (!toDelete.isEmpty()) {
            projectRepository.deleteAll(toDelete);
            log.debug("프로젝트 {}개 삭제 완료", toDelete.size());
        }
    }

    private void updateCertifications(Integer resumeId, List<ResumeCertificationRequest> certificationRequests) {
        List<ResumeCertification> existingCertifications = certificationRepository.findByResumeId(resumeId);
        Map<Integer, ResumeCertification> existingCertificationMap = existingCertifications.stream()
                .collect(Collectors.toMap(ResumeCertification::getId, cert -> cert));

        List<ResumeCertification> toSave = new ArrayList<>();
        List<ResumeCertification> toDelete = new ArrayList<>();
        Set<Integer> processedIds = new HashSet<>();

        if (certificationRequests != null) {
            for (ResumeCertificationRequest request : certificationRequests) {
                if (request.getId() != null && existingCertificationMap.containsKey(request.getId())) {
                    ResumeCertification existingCertification = existingCertificationMap.get(request.getId());
                    resumeMapper.updateCertificationEntity(existingCertification, request);
                    toSave.add(existingCertification);
                    processedIds.add(request.getId());
                    log.debug("자격증 업데이트 - ID: {}, 이름: {}", request.getId(), request.getCertificationName());
                } else {
                    Resume resume = resumeRepository.findById(resumeId).orElseThrow();
                    ResumeCertification newCertification = resumeMapper.toCertificationEntity(request, resume);
                    toSave.add(newCertification);
                    log.debug("새 자격증 생성 - 이름: {}", request.getCertificationName());
                }
            }
        }

        toDelete = existingCertifications.stream()
                .filter(certification -> !processedIds.contains(certification.getId()))
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            certificationRepository.saveAll(toSave);
            log.debug("자격증 {}개 저장 완료", toSave.size());
        }
        if (!toDelete.isEmpty()) {
            certificationRepository.deleteAll(toDelete);
            log.debug("자격증 {}개 삭제 완료", toDelete.size());
        }
    }

    private void updateWorkExperiences(Integer resumeId, List<ResumeWorkExperienceRequest> workExperienceRequests) {
        List<ResumeWorkExperience> existingWorkExperiences = workExperienceRepository.findByResumeId(resumeId);
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
                    Resume resume = resumeRepository.findById(resumeId).orElseThrow();
                    ResumeWorkExperience newWorkExperience = resumeMapper.toWorkExperienceEntity(request, resume);
                    toSave.add(newWorkExperience);
                    log.debug("새 경력 생성 - 회사: {}", request.getCompanyName());
                }
            }
        }

        toDelete = existingWorkExperiences.stream()
                .filter(workExperience -> !processedIds.contains(workExperience.getId()))
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            workExperienceRepository.saveAll(toSave);
            log.debug("경력 {}개 저장 완료", toSave.size());
        }
        if (!toDelete.isEmpty()) {
            workExperienceRepository.deleteAll(toDelete);
            log.debug("경력 {}개 삭제 완료", toDelete.size());
        }
    }

    private void updateEducations(Integer resumeId, List<ResumeEducationRequest> educationRequests) {
        List<ResumeEducation> existingEducations = educationRepository.findByResumeId(resumeId);
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
                    Resume resume = resumeRepository.findById(resumeId).orElseThrow();
                    ResumeEducation newEducation = resumeMapper.toEducationEntity(request, resume);
                    toSave.add(newEducation);
                    log.debug("새 학력 생성 - 학교: {}", request.getSchoolName());
                }
            }
        }

        toDelete = existingEducations.stream()
                .filter(education -> !processedIds.contains(education.getId()))
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            educationRepository.saveAll(toSave);
            log.debug("학력 {}개 저장 완료", toSave.size());
        }
        if (!toDelete.isEmpty()) {
            educationRepository.deleteAll(toDelete);
            log.debug("학력 {}개 삭제 완료", toDelete.size());
        }
    }
}