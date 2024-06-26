package com.importer.fileimporter.config.security.jwt

import com.importer.fileimporter.config.security.jwt.JwtUtils
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import spock.lang.Specification

class AuthTokenFilterSpec extends Specification {

    @MockBean
    JwtUtils jwtUtils

    @MockBean
    UserDetailsService userDetailsService

    def "should validate JWT and set user authentication"() {
        given: "a valid JWT token"
        def jwt = "valid.jwt.token.here"
        def username = "testUser"
        def userDetails = Mock(UserDetails)

        when: "the JWT token is validated and user details are fetched"
        jwtUtils.validateJwtToken(jwt) >> true
        jwtUtils.getUserNameFromJwtToken(jwt) >> username
        userDetailsService.loadUserByUsername(username) >> userDetails

        then: "the user is authenticated"
        // Add your assertions and interactions here
    }
}
