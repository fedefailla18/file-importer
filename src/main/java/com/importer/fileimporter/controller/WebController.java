package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.service.ProcessFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/hello")
@Slf4j
public class WebController {

    private final ProcessFile processFile;

    @GetMapping
    public FileInformationResponse salutation2(@RequestBody MultipartFile file) throws IOException {
        return processFile.processFile(file);
    }
    @PostMapping
    public FileInformationResponse salutation(@RequestBody MultipartFile file) throws IOException {
        return processFile.processFile(file);
    }

}