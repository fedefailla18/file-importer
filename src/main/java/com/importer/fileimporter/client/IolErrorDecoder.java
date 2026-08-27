package com.importer.fileimporter.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

@Slf4j
public class IolErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "Unknown error from IOL API";
        try {
            if (response.body() != null) {
                message = IOUtils.toString(response.body().asInputStream(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to read IOL error response body", e);
        }

        log.error("IOL API Error [{}]: {}", response.status(), message);

        HttpStatus status = HttpStatus.resolve(response.status());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseStatusException(status, "IOL API Error: " + message);
    }
}
