package com.importer.fileimporter.dto.integration.iol;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class IolAccountStatementResponse {
    private List<Cuenta> cuentas;
    private BigDecimal totalPesos;
    private BigDecimal totalDolares;
    
    // Placeholder for currency conversion
    private BigDecimal totalConvertedUsd;
    private BigDecimal exchangeRate;

    @Data
    public static class Cuenta {
        private String numero;
        private String tipo;
        private String moneda;
        private BigDecimal disponible;
        private BigDecimal comprometido;
        private BigDecimal saldo;
        private BigDecimal titulosValorizados;
        private BigDecimal total;
    }
}
