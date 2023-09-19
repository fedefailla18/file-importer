package com.importer.fileimporter.controller;

import com.importer.fileimporter.service.FileImporterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/hello")
public class WebController {

    @Autowired
    private FileImporterService fileImporterService;

    @GetMapping
    public List<Map<?, ?>> salutation(MultipartFile file) {
        try {
            return fileImporterService.getRows(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}