package com.importer.fileimporter.client;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IolFeignConfig {

    @Bean
    public ErrorDecoder iolErrorDecoder() {
        return new IolErrorDecoder();
    }
}
