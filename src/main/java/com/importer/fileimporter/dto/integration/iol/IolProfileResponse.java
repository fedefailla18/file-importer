package com.importer.fileimporter.dto.integration.iol;

import lombok.Data;

@Data
public class IolProfileResponse {
    private String nombreUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String cuit;
    private String perfilInversor;
    private String cuentaComitente;
    private String estado;
}
