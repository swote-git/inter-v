package dev.swote.interv.service.resume;

import dev.swote.interv.domain.resume.entity.*;
import dev.swote.interv.domain.resume.repository.*;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional(readOnly = true)
    public List<Resume> getUserResumes(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return resumeRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Page<Resume> getUserResumes(Integer userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return resumeRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public Resume getResumeById(Integer resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
    }

    @Transactional
    public Resume createResume(Integer userId, CreateResumeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = Resume.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .objective(request.getObjective())
                .status(ResumeStatus.ACTIVE)
                .skills(new HashSet<>(request.getSkills()))
                .build();

        resume = resumeRepository.save(resume);

        // Add projects
        if (request.getProjects() != null) {
            for (ProjectRequest projectRequest : request.getProjects()) {
                ResumeProject project = ResumeProject.builder()
                        .resume(resume)
                        .projectName(projectRequest.getProjectName())
                        .description(projectRequest.getDescription())
                        .startDate(projectRequest.getStartDate())
                        .endDate(projectRequest.getEndDate())
                        .inProgress(projectRequest.getInProgress())
                        .build();
                projectRepository.save(project);
            }
        }

        // Add certifications
        if (request.getCertifications() != null) {
            for (CertificationRequest certRequest : request.getCertifications()) {
                ResumeCertification certification = ResumeCertification.builder()
                        .resume(resume)
                        .certificationName(certRequest.getCertificationName())
                        .issuingOrganization(certRequest.getIssuingOrganization())
                        .acquiredDate(certRequest.getAcquiredDate())
                        .expiryDate(certRequest.getExpiryDate())
                        .noExpiry(certRequest.getNoExpiry())
                        .build();
                certificationRepository.save(certification);
            }
        }

        // Add work experiences
        if (request.getWorkExperiences() != null) {
            for (WorkExperienceRequest workRequest : request.getWorkExperiences()) {
                ResumeWorkExperience workExperience = ResumeWorkExperience.builder()
                        .resume(resume)
                        .companyName(workRequest.getCompanyName())
                        .position(workRequest.getPosition())
                        .department(workRequest.getDepartment())
                        .location(workRequest.getLocation())
                        .startDate(workRequest.getStartDate())
                        .endDate(workRequest.getEndDate())
                        .currentlyWorking(workRequest.getCurrentlyWorking())
                        .responsibilities(workRequest.getResponsibilities())
                        .achievements(workRequest.getAchievements())
                        .build();
                workExperienceRepository.save(workExperience);
            }
        }

        // Add education
        if (request.getEducations() != null) {
            for (EducationRequest eduRequest : request.getEducations()) {
                ResumeEducation education = ResumeEducation.builder()
                        .resume(resume)
                        .schoolType(eduRequest.getSchoolType())
                        .schoolName(eduRequest.getSchoolName())
                        .location(eduRequest.getLocation())
                        .major(eduRequest.getMajor())
                        .enrollmentDate(eduRequest.getEnrollmentDate())
                        .graduationDate(eduRequest.getGraduationDate())
                        .inProgress(eduRequest.getInProgress())
                        .gpa(eduRequest.getGpa())
                        .build();
                educationRepository.save(education);
            }
        }

        return resume;
    }

    @Transactional
    public Resume uploadResumeFile(Integer userId, MultipartFile file, String title) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // TODO(FIX IT)

        String content = resumeFileService.extractContent(file);
        String filePath = resumeFileService.storeFile(file);

        Resume resume = Resume.builder()
                .user(user)
                .title(title)
                .content(content)
                .filePath(filePath)
                .status(ResumeStatus.ACTIVE)
                .build();

        return resumeRepository.save(resume);
    }

    @Transactional
    public Resume updateResume(Integer resumeId, UpdateResumeRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        resume.setTitle(request.getTitle());
        resume.setContent(request.getContent());
        resume.setObjective(request.getObjective());

        // Update skills
        if (request.getSkills() != null) {
            resume.getSkills().clear();
            resume.getSkills().addAll(request.getSkills());
        }

        Resume savedResume = resumeRepository.save(resume);

        // Update projects
        if (request.getProjects() != null) {
            // Delete existing projects
            List<ResumeProject> existingProjects = projectRepository.findByResumeId(resumeId);
            projectRepository.deleteAll(existingProjects);

            // Add new projects
            for (ProjectRequest projectRequest : request.getProjects()) {
                ResumeProject project = ResumeProject.builder()
                        .resume(savedResume)
                        .projectName(projectRequest.getProjectName())
                        .description(projectRequest.getDescription())
                        .startDate(projectRequest.getStartDate())
                        .endDate(projectRequest.getEndDate())
                        .inProgress(projectRequest.getInProgress())
                        .build();
                projectRepository.save(project);
            }
        }

        // Update certifications
        if (request.getCertifications() != null) {
            // Delete existing certifications
            List<ResumeCertification> existingCerts = certificationRepository.findByResumeId(resumeId);
            certificationRepository.deleteAll(existingCerts);

            // Add new certifications
            for (CertificationRequest certRequest : request.getCertifications()) {
                ResumeCertification certification = ResumeCertification.builder()
                        .resume(savedResume)
                        .certificationName(certRequest.getCertificationName())
                        .issuingOrganization(certRequest.getIssuingOrganization())
                        .acquiredDate(certRequest.getAcquiredDate())
                        .expiryDate(certRequest.getExpiryDate())
                        .noExpiry(certRequest.getNoExpiry())
                        .build();
                certificationRepository.save(certification);
            }
        }

        // Update work experiences
        if (request.getWorkExperiences() != null) {
            // Delete existing work experiences
            List<ResumeWorkExperience> existingExps = workExperienceRepository.findByResumeId(resumeId);
            workExperienceRepository.deleteAll(existingExps);

            // Add new work experiences
            for (WorkExperienceRequest workRequest : request.getWorkExperiences()) {
                ResumeWorkExperience workExperience = ResumeWorkExperience.builder()
                        .resume(savedResume)
                        .companyName(workRequest.getCompanyName())
                        .position(workRequest.getPosition())
                        .department(workRequest.getDepartment())
                        .location(workRequest.getLocation())
                        .startDate(workRequest.getStartDate())
                        .endDate(workRequest.getEndDate())
                        .currentlyWorking(workRequest.getCurrentlyWorking())
                        .responsibilities(workRequest.getResponsibilities())
                        .achievements(workRequest.getAchievements())
                        .build();
                workExperienceRepository.save(workExperience);
            }
        }

        // Update educations
        if (request.getEducations() != null) {
            // Delete existing educations
            List<ResumeEducation> existingEdus = educationRepository.findByResumeId(resumeId);
            educationRepository.deleteAll(existingEdus);

            // Add new educations
            for (EducationRequest eduRequest : request.getEducations()) {
                ResumeEducation education = ResumeEducation.builder()
                        .resume(savedResume)
                        .schoolType(eduRequest.getSchoolType())
                        .schoolName(eduRequest.getSchoolName())
                        .location(eduRequest.getLocation())
                        .major(eduRequest.getMajor())
                        .enrollmentDate(eduRequest.getEnrollmentDate())
                        .graduationDate(eduRequest.getGraduationDate())
                        .inProgress(eduRequest.getInProgress())
                        .gpa(eduRequest.getGpa())
                        .build();
                educationRepository.save(education);
            }
        }

        return savedResume;
    }

    @Transactional
    public void deleteResume(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        resume.delete();
        resumeRepository.save(resume);
    }

    // Request classes for creating and updating resumes
    public static class CreateResumeRequest {
        private String title;
        private String content;
        private String objective;
        private List<String> skills;
        private List<ProjectRequest> projects;
        private List<CertificationRequest> certifications;
        private List<WorkExperienceRequest> workExperiences;
        private List<EducationRequest> educations;

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getObjective() { return objective; }
        public void setObjective(String objective) { this.objective = objective; }

        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }

        public List<ProjectRequest> getProjects() { return projects; }
        public void setProjects(List<ProjectRequest> projects) { this.projects = projects; }

        public List<CertificationRequest> getCertifications() { return certifications; }
        public void setCertifications(List<CertificationRequest> certifications) { this.certifications = certifications; }

        public List<WorkExperienceRequest> getWorkExperiences() { return workExperiences; }
        public void setWorkExperiences(List<WorkExperienceRequest> workExperiences) { this.workExperiences = workExperiences; }

        public List<EducationRequest> getEducations() { return educations; }
        public void setEducations(List<EducationRequest> educations) { this.educations = educations; }
    }

    public static class UpdateResumeRequest extends CreateResumeRequest {
        // Inherits all fields from CreateResumeRequest
    }

    public static class ProjectRequest {
        private String projectName;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean inProgress;

        // Getters and setters
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public Boolean getInProgress() { return inProgress; }
        public void setInProgress(Boolean inProgress) { this.inProgress = inProgress; }
    }

    public static class CertificationRequest {
        private String certificationName;
        private String issuingOrganization;
        private LocalDate acquiredDate;
        private LocalDate expiryDate;
        private Boolean noExpiry;

        // Getters and setters
        public String getCertificationName() { return certificationName; }
        public void setCertificationName(String certificationName) { this.certificationName = certificationName; }

        public String getIssuingOrganization() { return issuingOrganization; }
        public void setIssuingOrganization(String issuingOrganization) { this.issuingOrganization = issuingOrganization; }

        public LocalDate getAcquiredDate() { return acquiredDate; }
        public void setAcquiredDate(LocalDate acquiredDate) { this.acquiredDate = acquiredDate; }

        public LocalDate getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

        public Boolean getNoExpiry() { return noExpiry; }
        public void setNoExpiry(Boolean noExpiry) { this.noExpiry = noExpiry; }
    }

    public static class WorkExperienceRequest {
        private String companyName;
        private String position;
        private String department;
        private String location;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean currentlyWorking;
        private String responsibilities;
        private String achievements;

        // Getters and setters
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }

        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public Boolean getCurrentlyWorking() { return currentlyWorking; }
        public void setCurrentlyWorking(Boolean currentlyWorking) { this.currentlyWorking = currentlyWorking; }

        public String getResponsibilities() { return responsibilities; }
        public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }

        public String getAchievements() { return achievements; }
        public void setAchievements(String achievements) { this.achievements = achievements; }
    }

    public static class EducationRequest {
        private String schoolType;
        private String schoolName;
        private String location;
        private String major;
        private LocalDate enrollmentDate;
        private LocalDate graduationDate;
        private Boolean inProgress;
        private String gpa;

        // Getters and setters
        public String getSchoolType() { return schoolType; }
        public void setSchoolType(String schoolType) { this.schoolType = schoolType; }

        public String getSchoolName() { return schoolName; }
        public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getMajor() { return major; }
        public void setMajor(String major) { this.major = major; }

        public LocalDate getEnrollmentDate() { return enrollmentDate; }
        public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

        public LocalDate getGraduationDate() { return graduationDate; }
        public void setGraduationDate(LocalDate graduationDate) { this.graduationDate = graduationDate; }

        public Boolean getInProgress() { return inProgress; }
        public void setInProgress(Boolean inProgress) { this.inProgress = inProgress; }

        public String getGpa() { return gpa; }
        public void setGpa(String gpa) { this.gpa = gpa; }
    }
}