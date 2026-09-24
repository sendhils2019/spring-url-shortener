package com.example.urlshortener.config;

import com.example.urlshortener.service.TokenBucketLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(ShortenerProperties.class)
public class AppConfig {

    /** The shortener's own hostname — used to reject self-referencing URLs. */
    @Bean
    public String ownHost(ShortenerProperties props) {
        return URI.create(props.getBaseUrl()).getHost();
    }

    @Bean
    public TokenBucketLimiter tokenBucketLimiter(ShortenerProperties props) {
        return new TokenBucketLimiter(props.getCreateRatePerMinute());
    }
}
