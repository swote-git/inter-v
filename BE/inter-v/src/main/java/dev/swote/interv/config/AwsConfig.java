package dev.swote.interv.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
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
import org.springframework.context.annotation.Profile;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    /**
     * EC2에서 IAM Role을 사용하는 운영 환경용 S3 클라이언트
     */
    @Bean
    @Profile({"prod", "staging"})
    public AmazonS3 amazonS3Prod() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * 로컬 개발 환경용 S3 클라이언트 (AWS 프로파일 또는 환경 변수 사용)
     */
    @Bean
    @Profile({"local", "dev"})
    public AmazonS3 amazonS3Local() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * EC2에서 IAM Role을 사용하는 운영 환경용 Polly 클라이언트
     */
    @Bean
    @Profile({"prod", "staging"})
    public AmazonPolly amazonPollyProd() {
        return AmazonPollyClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * 로컬 개발 환경용 Polly 클라이언트
     */
    @Bean
    @Profile({"local", "dev"})
    public AmazonPolly amazonPollyLocal() {
        return AmazonPollyClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * EC2에서 IAM Role을 사용하는 운영 환경용 Transcribe 클라이언티
     */
    @Bean
    @Profile({"prod", "staging"})
    public AmazonTranscribe amazonTranscribeProd() {
        return AmazonTranscribeClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * 로컬 개발 환경용 Transcribe 클라이언트
     */
    @Bean
    @Profile({"local", "dev"})
    public AmazonTranscribe amazonTranscribeLocal() {
        return AmazonTranscribeClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }
}