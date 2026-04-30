package com.importer.fileimporter.dto.integration.iol;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IolOperationResponse {
    private Long numero;
    private String fechaOrden;
    private String tipo;
    private String estado;
    private String simbolo;
    private BigDecimal cantidad;
    private BigDecimal precio;
    private BigDecimal monto;
    private String modalidad;
    
    // Placeholder for currency conversion
    private BigDecimal montoUsd;
    private BigDecimal exchangeRate;
}
