package dev.swote.interv.service.audio;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechToTextService {

//    private final AmazonTranscribe amazonTranscribe;
//    private final AmazonS3 amazonS3;
//    private final ObjectMapper objectMapper;
//
//    @Value("${aws.s3.bucket}")
//    private String bucketName;
//
//    public String startTranscriptionJob(String audioFileUrl) {
//        try {
//            String jobName = "transcribe-" + UUID.randomUUID().toString();
//
//            StartTranscriptionJobRequest request = new StartTranscriptionJobRequest()
//                    .withTranscriptionJobName(jobName)
//                    .withMedia(new Media().withMediaFileUri(audioFileUrl))
//                    .withMediaFormat("wav")
//                    .withLanguageCode(LanguageCode.KoKR) // Korean language code
//                    .withSettings(new Settings()
//                            .withShowSpeakerLabels(false)
//                            .withMaxSpeakerLabels(1));
//
//            amazonTranscribe.startTranscriptionJob(request);
//            return jobName;
//        } catch (Exception e) {
//            log.error("Failed to start transcription job", e);
//            throw new RuntimeException("Failed to start transcription job", e);
//        }
//    }
//
//    public String getTranscriptionResult(String jobName) {
//        try {
//            GetTranscriptionJobRequest request = new GetTranscriptionJobRequest()
//                    .withTranscriptionJobName(jobName);
//
//            GetTranscriptionJobResult result;
//            do {
//                result = amazonTranscribe.getTranscriptionJob(request);
//                Thread.sleep(1000);
//            } while (result.getTranscriptionJob().getTranscriptionJobStatus()
//                    .equals(TranscriptionJobStatus.IN_PROGRESS.toString()));
//
//            if (result.getTranscriptionJob().getTranscriptionJobStatus()
//                    .equals(TranscriptionJobStatus.COMPLETED.toString())) {
//                return result.getTranscriptionJob().getTranscript().getTranscriptFileUri();
//            } else {
//                throw new RuntimeException("Transcription job failed: " +
//                        result.getTranscriptionJob().getFailureReason());
//            }
//        } catch (Exception e) {
//            log.error("Failed to get transcription result", e);
//            throw new RuntimeException("Failed to get transcription result", e);
//        }
//    }
//
//    public String extractTranscribedText(String transcriptionUrl) {
//        try {
//            URL url = new URL(transcriptionUrl);
//            JsonNode rootNode = objectMapper.readTree(url);
//            return rootNode.path("results").path("transcripts")
//                    .get(0).path("transcript").asText();
//        } catch (IOException e) {
//            log.error("Failed to extract transcribed text", e);
//            throw new RuntimeException("Failed to extract transcribed text", e);
//        }
//    }
}