package com.importer.fileimporter.dto;

import com.importer.fileimporter.utils.OperationUtils;

import java.math.BigDecimal;
import java.util.Map;

public class MexcTransactionAdapter extends TransactionCoinName {

    private static final String PARES_KEY = "Pares";
    private static final String TIEMPO_KEY = "Tiempo";
    private static final String TIPO_KEY = "Tipo";
    private static final String DIRECCION_KEY = "Dirección";
    private static final String PRECIO_PROMEDIO_COMPLETO_KEY = "Precio Promedio Completo";
    private static final String PRECIO_DE_ORDEN_KEY = "Precio de Orden";
    private static final String CANTIDAD_COMPLETA_KEY = "Cantidad Completa";
    private static final String CANTIDAD_DE_ORDEN_KEY = "Cantidad de Orden";
    private static final String MONTO_KEY = "Monto de Orden";
    private static final String ESTADO_KEY = "Estado";
    private static final String VENTA_STRING = "Venta";
    private static final String COMPRA_STRING = "Compra";


    private final Map<?, ?> row;

    public MexcTransactionAdapter(Map<?, ?> row) {
        super();
        this.row = row;
        this.coinName = getSymbol();
    }

    @Override
    public String getDate() {
        return row.get(TIEMPO_KEY).toString();
    }

    @Override
    public String getPair() {
        return row.get(PARES_KEY).toString();
    }

    public String getTipo() {
        return row.get(TIPO_KEY).toString();
    }

    @Override
    public String getSide() {
        String direccion = row.get(DIRECCION_KEY).toString();
        return VENTA_STRING.equalsIgnoreCase(direccion) ? OperationUtils.SELL_STRING : OperationUtils.BUY_STRING;
    }

    @Override
    public BigDecimal getPrice() {
        return new BigDecimal(row.get(PRECIO_PROMEDIO_COMPLETO_KEY).toString());
    }

    public BigDecimal getPrecioDeOrden() {
        return new BigDecimal(row.get(PRECIO_DE_ORDEN_KEY).toString());
    }

    @Override
    public BigDecimal getExecuted() {
        return new BigDecimal(row.get(CANTIDAD_COMPLETA_KEY).toString());
    }

    public BigDecimal getCantidadDeOrden() {
        return new BigDecimal(row.get(CANTIDAD_DE_ORDEN_KEY).toString());
    }

    @Override
    public BigDecimal getAmount() {
        return new BigDecimal(row.get(MONTO_KEY).toString());
    }

    @Override
    public BigDecimal getFee() {
        return BigDecimal.ZERO;
    }

    @Override
    public String getSymbol() {
        String pair = row.get(PARES_KEY).toString();
        return pair.substring(0, pair.indexOf('_'));
    }

    @Override
    public String getFeeSymbol() {
        return "";
    }

    public String getEstado() {
        return row.get(ESTADO_KEY).toString();
    }

    @Override
    public String getCoinName() {
        return this.coinName;
    }
}
