package dev.swote.interv.service.audio;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioStorageService {

//    private final AmazonS3 amazonS3;
//
//    @Value("${aws.s3.bucket}")
//    private String bucketName;
//
//    public String storeAudioFile(MultipartFile file) {
//        try {
//            String key = "interviews/audio/" + UUID.randomUUID() + ".wav";
//
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentLength(file.getSize());
//            metadata.setContentType(file.getContentType());
//
//            amazonS3.putObject(new PutObjectRequest(
//                    bucketName,
//                    key,
//                    file.getInputStream(),
//                    metadata
//            ));
//
//            return amazonS3.getUrl(bucketName, key).toString();
//        } catch (IOException e) {
//            log.error("Failed to store audio file", e);
//            throw new RuntimeException("Failed to store audio file", e);
//        }
//    }
}