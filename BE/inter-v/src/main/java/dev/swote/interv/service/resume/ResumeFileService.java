package dev.swote.interv.service.resume;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeFileService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String storeFile(MultipartFile file) throws IOException {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String key = "resumes/" + UUID.randomUUID() + "." + extension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(new PutObjectRequest(
                bucketName,
                key,
                file.getInputStream(),
                metadata
        ));

        return amazonS3.getUrl(bucketName, key).toString();
    }

    public String extractContent(MultipartFile file) throws IOException {
        // In a real application, you'd use libraries like Apache PDFBox for PDFs,
        // Apache POI for Word docs, etc. to extract text
        // For simplicity, let's assume we can extract the text directly

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        if ("pdf".equalsIgnoreCase(extension)) {
            // Use PDFBox to extract text
            return "Extracted PDF content"; // Placeholder
        } else if ("docx".equalsIgnoreCase(extension)) {
            // Use Apache POI to extract text
            return "Extracted DOCX content"; // Placeholder
        } else {
            throw new UnsupportedOperationException("Unsupported file format: " + extension);
        }
    }
}