package com.importer.fileimporter.dto;

import com.importer.fileimporter.utils.OperationUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
public class MexcTransactionAdapter extends TransactionCoinName {

    private static final String PARES_KEY = "Pairs"; //"Pares";
    private static final String TIEMPO_KEY = "Time"; //"Tiempo";
    private static final String TIPO_KEY = "Type"; //"Tipo";
    private static final String DIRECCION_KEY = "Direction"; //"Dirección";
    private static final String PRECIO_PROMEDIO_COMPLETO_KEY = "Average Filled Price"; //"Precio Promedio Completo";
    private static final String PRECIO_DE_ORDEN_KEY = "Order Price"; //"Precio de Orden";
    private static final String CANTIDAD_COMPLETA_KEY = "Filled Quantity"; //"Cantidad Completa";
    private static final String CANTIDAD_DE_ORDEN_KEY = "Order Quantity"; //"Cantidad de Orden";
    private static final String MONTO_KEY = "Order Amount"; //"Monto de Orden";
    private static final String ESTADO_KEY = "Status"; //"Estado";
    private static final String VENTA_STRING = "Sell"; //"Venta";
    private static final String COMPRA_STRING = "Buy"; //"Compra";


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
        return row.get(DIRECCION_KEY).toString();
    }

    @Override
    public BigDecimal getPrice() {
        try {
            return new BigDecimal(row.get(PRECIO_PROMEDIO_COMPLETO_KEY).toString());
        } catch (Exception e) {
            log.warn(String.format("Can't convert %s", row.get(PRECIO_DE_ORDEN_KEY).toString()), e);
            return null;
        }
    }

    public BigDecimal getPrecioDeOrden() {
        try {
            return new BigDecimal(row.get(PRECIO_DE_ORDEN_KEY).toString());
        } catch (Exception e) {
            log.warn(String.format("Can't convert %s", row.get(PRECIO_DE_ORDEN_KEY).toString()), e);
            return null;
        }
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
    public String getPaidWith() {
        return getPair().replace(getCoinName() + "_", "");
    }
}
