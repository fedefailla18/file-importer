package com.importer.fileimporter.dto.integration.iol;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class IolPortfolioResponse {
    private List<Activo> activos;

    @Data
    public static class Activo {
        private String simbolo;
        private String descripcion;
        private BigDecimal cantidad;
        private BigDecimal valorizado;
        private BigDecimal ultimoPrecio;
        private BigDecimal variacion;
        private BigDecimal ppc;
        private BigDecimal gananciaPorcentaje;
        private BigDecimal gananciaDinero;
        private String tipo;
        
        // Placeholder for currency conversion
        private BigDecimal valorizadoUsd;
        private BigDecimal exchangeRate;
    }
}
