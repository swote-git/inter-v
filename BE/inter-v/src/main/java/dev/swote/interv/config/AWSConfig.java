package dev.swote.interv.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

//    @Value("${aws.credentials.accessKey}")
//    private String accessKey;
//
//    @Value("${aws.credentials.secretKey}")
//    private String secretKey;
//
//    @Value("${aws.region}")
//    private String region;
//
//    @Bean
//    public AmazonS3 amazonS3() {
//        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
//        return AmazonS3ClientBuilder.standard()
//                .withCredentials(new AWSStaticCredentialsProvider(credentials))
//                .withRegion(Regions.fromName(region))
//                .build();
//    }
//
//    @Bean
//    public AmazonPolly amazonPolly() {
//        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
//        return AmazonPollyClientBuilder.standard()
//                .withCredentials(new AWSStaticCredentialsProvider(credentials))
//                .withRegion(Regions.fromName(region))
//                .build();
//    }
//
//    @Bean
//    public AmazonTranscribe amazonTranscribe() {
//        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
//        return AmazonTranscribeClientBuilder.standard()
//                .withCredentials(new AWSStaticCredentialsProvider(credentials))
//                .withRegion(Regions.fromName(region))
//                .build();
//    }
}