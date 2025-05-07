package dev.swote.interv.service.resume;

import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.resume.entity.ResumeStatus;
import dev.swote.interv.domain.resume.repository.ResumeRepository;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
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
    public Resume createResume(Integer userId, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = Resume.builder()
                .user(user)
                .title(title)
                .content(content)
                .status(ResumeStatus.ACTIVE)
                .build();

        return resumeRepository.save(resume);
    }

    @Transactional
    public Resume uploadResumeFile(Integer userId, MultipartFile file, String title) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
    public Resume updateResume(Integer resumeId, String title, String content) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        resume.setTitle(title);
        resume.setContent(content);

        return resumeRepository.save(resume);
    }

    @Transactional
    public void deleteResume(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        resume.delete();
        resumeRepository.save(resume);
    }
}