package dev.swote.interv.service.audio;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextToSpeechService {
//
//    private final AmazonPolly amazonPolly;
//    private final AudioStorageService audioStorageService;
//
//    @Value("${aws.s3.bucket}")
//    private String bucketName;
//
//    public String convertTextToSpeech(String text) {
//        try {
//            String outputKey = "interviews/tts/" + UUID.randomUUID() + ".mp3";
//
//            SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
//                    .withOutputFormat(OutputFormat.Mp3)
//                    .withVoiceId(VoiceId.Joanna)
//                    .withText(text)
//                    .withEngine(Engine.Neural);
//
//            SynthesizeSpeechResult synthesizeSpeechResult =
//                    amazonPolly.synthesizeSpeech(synthesizeSpeechRequest);
//
//            // TODO(FIX this)
//            return String.valueOf(amazonPolly.synthesizeSpeech(
//                    synthesizeSpeechRequest
//            ));
//        } catch (Exception e) {
//            log.error("Failed to convert text to speech", e);
//            throw new RuntimeException("Failed to convert text to speech", e);
//        }
//    }
}