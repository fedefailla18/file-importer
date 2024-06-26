package com.importer.fileimporter.controller

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.config.security.jwt.AuthTokenFilter
import com.importer.fileimporter.config.security.jwt.JwtUtils
import com.importer.fileimporter.config.security.services.UserDetailsImpl
import com.importer.fileimporter.config.security.services.UserDetailsServiceImpl
import com.importer.fileimporter.dto.FileInformationResponse
import com.importer.fileimporter.service.ProcessFile
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class WebControllerIntegrationSpec extends BaseIntegrationSpec {

    @MockBean
    ProcessFile processFile

    def jwtUtils = Mock(JwtUtils)

    @MockBean
    UserDetailsServiceImpl userDetailsService

    def authTokenFilter = new AuthTokenFilter(jwtUtils, userDetailsService)


    @WithMockUser
    def "should return processed file information"() {
        given: "a valid file and JWT token"
        def mockFile = new MockMultipartFile(
                "file",
                "sample.csv",
                MediaType.TEXT_PLAIN_VALUE,
                new ClassPathResource("integrationTest/sample_transactions.csv").getInputStream()
        )

        def response = new FileInformationResponse()
        def jwtToken = "Bearer valid.jwt.token.here"

        when: "the processFile method is mocked"
        processFile.processFile(_) >> response

        and: "a file is uploaded to the endpoint"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/hello")
                        .file(mockFile)
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andReturn()

        then: "the response should contain the file information"
        result.response.status == 200

        and:
        1 * jwtUtils.validateJwtToken("valid.jwt.token.here") >> true
        1 * jwtUtils.getUserNameFromJwtToken("valid.jwt.token.here") >> "testUser"
        1 * userDetailsService.loadUserByUsername("testUser") >> Mock(UserDetailsImpl)
    }

    @WithMockUser(username="admin",roles = ["USER","ADMIN"])
    def "should return null for empty file"() {
        given: "an empty file and JWT token"
        def mockFile = new MockMultipartFile(
                "file",
                "empty.csv",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        )

        def jwtToken = "Bearer valid.jwt.token.here"

        when: "the file is uploaded to the endpoint"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/hello")
                        .file(mockFile)
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andReturn()

        then: "the response should be null"
        result.response.status == 200
        result.response.contentAsString.isEmpty()

        and:
        1 * jwtUtils.validateJwtToken("valid.jwt.token.here") >> true
        1 * jwtUtils.getUserNameFromJwtToken("valid.jwt.token.here") >> "testUser"
        1 * userDetailsService.loadUserByUsername("testUser") >> Mock(UserDetailsImpl)
    }
}
