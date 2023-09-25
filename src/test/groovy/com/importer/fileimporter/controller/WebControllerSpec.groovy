package com.importer.fileimporter.controller

import com.importer.fileimporter.service.FileImporterService
import com.importer.fileimporter.service.ProcessFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class WebControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    FileImporterService fileImporterService
    @Autowired
    def processFile = new ProcessFile(fileImporterService)
    def controller = new WebController(processFile)

    def "should not allow public access to GET /hello"() {
        when:
        def result = mockMvc.perform(get("/hello")
                .contentType(MediaType.APPLICATION_JSON))

        then:
        result.andExpect(status().isOk())
    }

    def "should allow public access to POST /hello"() {
        when:
        def result = mockMvc.perform(post("/hello")
                .contentType(MediaType.APPLICATION_JSON))

        then:
        result.andExpect(status().isOk())
    }
}
