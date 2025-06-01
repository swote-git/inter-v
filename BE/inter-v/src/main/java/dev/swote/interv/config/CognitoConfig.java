package dev.swote.interv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;

@Configuration
public class CognitoConfig {

    @Value("${aws.cognito.region}")
    private String region;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Bean
    public String userPoolId() {
        return userPoolId;
    }

    /**
     * EC2에서 IAM Role을 사용하는 운영 환경용 Cognito 클라이언트
     */
    @Bean
    @Profile({"prod", "staging"})
    public AWSCognitoIdentityProvider cognitoIdentityProviderProd() {
        return AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * 로컬 개발 환경용 Cognito 클라이언트 (AWS 프로파일 또는 환경 변수 사용)
     */
    @Bean
    @Profile({"local", "dev"})
    public AWSCognitoIdentityProvider cognitoIdentityProviderLocal() {
        return AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(Regions.fromName(region))
                .build();
    }
}