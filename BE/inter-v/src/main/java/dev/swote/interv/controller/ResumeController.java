package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.resume.ResumeService;
import dev.swote.interv.service.resume.ResumeService.CreateResumeRequest;
import dev.swote.interv.service.resume.ResumeService.UpdateResumeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<CommonResponse<Page<Resume>>> getMyResumes(
            CurrentUser currentUser,
            Pageable pageable
    ) {
        Page<Resume> resumes = resumeService.getUserResumes(currentUser.id(), pageable);
        return ResponseEntity.ok(CommonResponse.ok(resumes));
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<CommonResponse<Resume>> getResume(
            @PathVariable Integer resumeId
    ) {
        Resume resume = resumeService.getResumeById(resumeId);
        return ResponseEntity.ok(CommonResponse.ok(resume));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Resume>> createResume(
            CurrentUser currentUser,
            @RequestBody CreateResumeRequest request
    ) {
        Resume resume = resumeService.createResume(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(resume));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Resume>> uploadResume(
            CurrentUser currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title
    ) throws IOException {
        Resume resume = resumeService.uploadResumeFile(
                currentUser.id(),
                file,
                title
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(resume));
    }

    @PutMapping("/{resumeId}")
    public ResponseEntity<CommonResponse<Resume>> updateResume(
            @PathVariable Integer resumeId,
            @RequestBody UpdateResumeRequest request
    ) {
        Resume resume = resumeService.updateResume(resumeId, request);
        return ResponseEntity.ok(CommonResponse.ok(resume));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Integer resumeId
    ) {
        resumeService.deleteResume(resumeId);
        return ResponseEntity.noContent().build();
    }
}