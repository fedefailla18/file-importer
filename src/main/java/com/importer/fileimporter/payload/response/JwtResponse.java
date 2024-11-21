package com.importer.fileimporter.payload.response;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Data
public class JwtResponse {

    private final String jwt;

    private final Long id;

    private final String username;

    private final String email;

    private final List<String> roles;

    public JwtResponse(String jwt) {
        this.jwt = jwt;
        id = null;
        username = null;
        email = null;
        roles = null;
    }
}
