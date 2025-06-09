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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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
    public Page<ResumeListResponse> getUserResumes(Integer userId, Pageable pageable) {
        log.info("사용자 {}의 이력서 목록 조회 - 페이지: {}, 크기: {}", userId, pageable.getPageNumber(), pageable.getPageSize());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Page<Resume> resumePage = resumeRepository.findByUser(user, pageable);

        List<ResumeListResponse> responseList = resumePage.getContent().stream()
                .map(resumeMapper::toListResponse)
                .collect(Collectors.toList());

        log.info("사용자 {}의 이력서 {}개 조회 완료", userId, responseList.size());
        return new PageImpl<>(responseList, pageable, resumePage.getTotalElements());
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

    @Transactional
    public ResumeResponse createResume(Integer userId, CreateResumeRequest request) {
        log.info("이력서 생성 - 사용자 ID: {}, 제목: {}", userId, request.getTitle());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

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
    public ResumeResponse updateResume(Integer resumeId, UpdateResumeRequest request) {
        log.info("이력서 수정 - ID: {}, 제목: {}", resumeId, request.getTitle());

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        // Resume 기본 정보 업데이트
        resumeMapper.updateEntity(resume, request);
        resume = resumeRepository.save(resume);

        // 기존 자식 엔티티들 삭제
        deleteChildEntities(resumeId);

        // 새로운 자식 엔티티들 생성
        createChildEntities(resume, request);

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

    // 자식 엔티티들 생성
    private void createChildEntities(Resume resume, CreateResumeRequest request) {
        createChildEntities(resume,
                request.getProjects(),
                request.getCertifications(),
                request.getWorkExperiences(),
                request.getEducations());
    }

    private void createChildEntities(Resume resume, UpdateResumeRequest request) {
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

    // 기존 자식 엔티티들 삭제
    private void deleteChildEntities(Integer resumeId) {
        log.debug("기존 자식 엔티티들 삭제 시작 - Resume ID: {}", resumeId);

        // 기존 프로젝트 삭제
        List<ResumeProject> existingProjects = projectRepository.findByResumeId(resumeId);
        if (!existingProjects.isEmpty()) {
            projectRepository.deleteAll(existingProjects);
            log.debug("기존 프로젝트 {}개 삭제", existingProjects.size());
        }

        // 기존 자격증 삭제
        List<ResumeCertification> existingCerts = certificationRepository.findByResumeId(resumeId);
        if (!existingCerts.isEmpty()) {
            certificationRepository.deleteAll(existingCerts);
            log.debug("기존 자격증 {}개 삭제", existingCerts.size());
        }

        // 기존 경력 삭제
        List<ResumeWorkExperience> existingExps = workExperienceRepository.findByResumeId(resumeId);
        if (!existingExps.isEmpty()) {
            workExperienceRepository.deleteAll(existingExps);
            log.debug("기존 경력 {}개 삭제", existingExps.size());
        }

        // 기존 학력 삭제
        List<ResumeEducation> existingEdus = educationRepository.findByResumeId(resumeId);
        if (!existingEdus.isEmpty()) {
            educationRepository.deleteAll(existingEdus);
            log.debug("기존 학력 {}개 삭제", existingEdus.size());
        }

        log.debug("기존 자식 엔티티들 삭제 완료 - Resume ID: {}", resumeId);
    }
}