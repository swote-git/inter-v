package dev.swote.interv.domain.resume.mapper;

import dev.swote.interv.domain.resume.dto.*;
import dev.swote.interv.domain.resume.entity.*;
import dev.swote.interv.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ResumeMapper {

    // Entity -> Response DTO 변환
    public ResumeResponse toResponse(Resume resume) {
        if (resume == null) {
            return null;
        }

        return ResumeResponse.builder()
                .id(resume.getId())
                .user(toUserSimpleResponse(resume.getUser()))
                .title(resume.getTitle())
                .content(resume.getContent())
                .objective(resume.getObjective())
                .filePath(resume.getFilePath())
                .status(resume.getStatus())
                .skills(resume.getSkills())
                .projects(toProjectResponseList(resume.getProjects()))
                .certifications(toCertificationResponseList(resume.getCertifications()))
                .workExperiences(toWorkExperienceResponseList(resume.getWorkExperiences()))
                .educations(toEducationResponseList(resume.getEducations()))
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    public ResumeListResponse toListResponse(Resume resume) {
        if (resume == null) {
            return null;
        }

        return ResumeListResponse.builder()
                .id(resume.getId())
                .user(toUserSimpleResponse(resume.getUser()))
                .title(resume.getTitle())
                .objective(resume.getObjective())
                .status(resume.getStatus())
                .skills(resume.getSkills())
                .projectCount(resume.getProjects() != null ? resume.getProjects().size() : 0)
                .certificationCount(resume.getCertifications() != null ? resume.getCertifications().size() : 0)
                .workExperienceCount(resume.getWorkExperiences() != null ? resume.getWorkExperiences().size() : 0)
                .educationCount(resume.getEducations() != null ? resume.getEducations().size() : 0)
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    private UserSimpleResponse toUserSimpleResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserSimpleResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .nickname(user.getNickname())
                .birthDate(user.getBirthDate())
                .build();
    }

    private List<ResumeProjectResponse> toProjectResponseList(List<ResumeProject> projects) {
        if (projects == null) {
            return null;
        }

        return projects.stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList());
    }

    private ResumeProjectResponse toProjectResponse(ResumeProject project) {
        if (project == null) {
            return null;
        }

        return ResumeProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .inProgress(project.getInProgress())
                .build();
    }

    private List<ResumeCertificationResponse> toCertificationResponseList(List<ResumeCertification> certifications) {
        if (certifications == null) {
            return null;
        }

        return certifications.stream()
                .map(this::toCertificationResponse)
                .collect(Collectors.toList());
    }

    private ResumeCertificationResponse toCertificationResponse(ResumeCertification certification) {
        if (certification == null) {
            return null;
        }

        return ResumeCertificationResponse.builder()
                .id(certification.getId())
                .certificationName(certification.getCertificationName())
                .issuingOrganization(certification.getIssuingOrganization())
                .acquiredDate(certification.getAcquiredDate())
                .expiryDate(certification.getExpiryDate())
                .noExpiry(certification.getNoExpiry())
                .build();
    }

    private List<ResumeWorkExperienceResponse> toWorkExperienceResponseList(List<ResumeWorkExperience> workExperiences) {
        if (workExperiences == null) {
            return null;
        }

        return workExperiences.stream()
                .map(this::toWorkExperienceResponse)
                .collect(Collectors.toList());
    }

    private ResumeWorkExperienceResponse toWorkExperienceResponse(ResumeWorkExperience workExperience) {
        if (workExperience == null) {
            return null;
        }

        return ResumeWorkExperienceResponse.builder()
                .id(workExperience.getId())
                .companyName(workExperience.getCompanyName())
                .position(workExperience.getPosition())
                .department(workExperience.getDepartment())
                .location(workExperience.getLocation())
                .startDate(workExperience.getStartDate())
                .endDate(workExperience.getEndDate())
                .currentlyWorking(workExperience.getCurrentlyWorking())
                .responsibilities(workExperience.getResponsibilities())
                .achievements(workExperience.getAchievements())
                .build();
    }

    private List<ResumeEducationResponse> toEducationResponseList(List<ResumeEducation> educations) {
        if (educations == null) {
            return null;
        }

        return educations.stream()
                .map(this::toEducationResponse)
                .collect(Collectors.toList());
    }

    private ResumeEducationResponse toEducationResponse(ResumeEducation education) {
        if (education == null) {
            return null;
        }

        return ResumeEducationResponse.builder()
                .id(education.getId())
                .schoolType(education.getSchoolType())
                .schoolName(education.getSchoolName())
                .location(education.getLocation())
                .major(education.getMajor())
                .enrollmentDate(education.getEnrollmentDate())
                .graduationDate(education.getGraduationDate())
                .inProgress(education.getInProgress())
                .gpa(education.getGpa())
                .build();
    }

    // Request DTO -> Entity 변환 메서드들
    public Resume toEntity(CreateResumeRequest request, User user) {
        if (request == null) {
            return null;
        }

        return Resume.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .objective(request.getObjective())
                .status(ResumeStatus.ACTIVE)
                .skills(request.getSkills())
                .build();
    }

    public void updateEntity(Resume resume, UpdateResumeRequest request) {
        if (resume == null || request == null) {
            return;
        }

        resume.setTitle(request.getTitle());
        resume.setContent(request.getContent());
        resume.setObjective(request.getObjective());

        if (request.getSkills() != null) {
            resume.getSkills().clear();
            resume.getSkills().addAll(request.getSkills());
        }
    }

    // 새로운 생성 메서드들
    public ResumeProject toProjectEntity(ResumeProjectRequest request, Resume resume) {
        if (request == null) {
            return null;
        }

        return ResumeProject.builder()
                .resume(resume)
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .inProgress(request.getInProgress())
                .build();
    }

    public ResumeCertification toCertificationEntity(ResumeCertificationRequest request, Resume resume) {
        if (request == null) {
            return null;
        }

        return ResumeCertification.builder()
                .resume(resume)
                .certificationName(request.getCertificationName())
                .issuingOrganization(request.getIssuingOrganization())
                .acquiredDate(request.getAcquiredDate())
                .expiryDate(request.getExpiryDate())
                .noExpiry(request.getNoExpiry())
                .build();
    }

    public ResumeWorkExperience toWorkExperienceEntity(ResumeWorkExperienceRequest request, Resume resume) {
        if (request == null) {
            return null;
        }

        return ResumeWorkExperience.builder()
                .resume(resume)
                .companyName(request.getCompanyName())
                .position(request.getPosition())
                .department(request.getDepartment())
                .location(request.getLocation())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .currentlyWorking(request.getCurrentlyWorking())
                .responsibilities(request.getResponsibilities())
                .achievements(request.getAchievements())
                .build();
    }

    public ResumeEducation toEducationEntity(ResumeEducationRequest request, Resume resume) {
        if (request == null) {
            return null;
        }

        return ResumeEducation.builder()
                .resume(resume)
                .schoolType(request.getSchoolType())
                .schoolName(request.getSchoolName())
                .location(request.getLocation())
                .major(request.getMajor())
                .enrollmentDate(request.getEnrollmentDate())
                .graduationDate(request.getGraduationDate())
                .inProgress(request.getInProgress())
                .gpa(request.getGpa())
                .build();
    }

    // 새로운 업데이트 메서드들
    public void updateProjectEntity(ResumeProject project, ResumeProjectRequest request) {
        if (project == null || request == null) {
            return;
        }

        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setInProgress(request.getInProgress());
    }

    public void updateCertificationEntity(ResumeCertification certification, ResumeCertificationRequest request) {
        if (certification == null || request == null) {
            return;
        }

        certification.setCertificationName(request.getCertificationName());
        certification.setIssuingOrganization(request.getIssuingOrganization());
        certification.setAcquiredDate(request.getAcquiredDate());
        certification.setExpiryDate(request.getExpiryDate());
        certification.setNoExpiry(request.getNoExpiry());
    }

    public void updateWorkExperienceEntity(ResumeWorkExperience workExperience, ResumeWorkExperienceRequest request) {
        if (workExperience == null || request == null) {
            return;
        }

        workExperience.setCompanyName(request.getCompanyName());
        workExperience.setPosition(request.getPosition());
        workExperience.setDepartment(request.getDepartment());
        workExperience.setLocation(request.getLocation());
        workExperience.setStartDate(request.getStartDate());
        workExperience.setEndDate(request.getEndDate());
        workExperience.setCurrentlyWorking(request.getCurrentlyWorking());
        workExperience.setResponsibilities(request.getResponsibilities());
        workExperience.setAchievements(request.getAchievements());
    }

    public void updateEducationEntity(ResumeEducation education, ResumeEducationRequest request) {
        if (education == null || request == null) {
            return;
        }

        education.setSchoolType(request.getSchoolType());
        education.setSchoolName(request.getSchoolName());
        education.setLocation(request.getLocation());
        education.setMajor(request.getMajor());
        education.setEnrollmentDate(request.getEnrollmentDate());
        education.setGraduationDate(request.getGraduationDate());
        education.setInProgress(request.getInProgress());
        education.setGpa(request.getGpa());
    }
}