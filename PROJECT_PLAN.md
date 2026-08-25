# Plan de Proyecto: Portfolio Crypto Manager

Este documento traza el plan para estabilizar, completar y desplegar la aplicación.

## Fase 1: Estabilización y Definición (La Base)

*Objetivo: Crear una base de código estable y tener una definición clara e inequívoca del comportamiento principal del sistema.*

1.  **Reparar la Suite de Pruebas Actual:** No podemos avanzar con tests fallando.
    *   **Acción:** Ejecutar la suite de pruebas completa del backend (`./gradlew test` y `./gradlew integrationTest`).
    *   **Acción:** Identificar y corregir las pruebas que fallan para asegurar que la funcionalidad existente (que se conserva) esté verificada.

2.  **Definir el Caso de Uso Principal con BDD (Gherkin):** Nos centraremos en el cálculo de holdings, como sugeriste. Se creará un archivo `holdings_calculation.feature` en `src/integration-test/resources/features/`.
    *   **Acción:** Escribir el primer escenario BDD. Por ejemplo:

    ```gherkin
    # features/holdings_calculation.feature

    Feature: Cálculo de Holdings a partir de un CSV de Transacciones

      Scenario: Un usuario sube un CSV de Binance con compras y ventas
        Given un portafolio llamado "Mi-Binance" está vacío
        And un archivo CSV "transacciones_simples.csv" con el siguiente contenido:
          """
          Date(UTC),Pair,Side,Price,Executed,Amount,Fee
          2023-01-15 10:00:00,ETHUSDT,BUY,1500,0.5 ETH,750 USDT,0.0005 ETH
          2023-01-20 12:30:00,BTCUSDT,BUY,20000,0.1 BTC,2000 USDT,0.0001 BTC
          2023-02-01 15:00:00,ETHUSDT,SELL,1800,0.2 ETH,360 USDT,0.36 USDT
          """
        When el usuario sube el archivo "transacciones_simples.csv" al portafolio "Mi-Binance"
        Then el sistema procesa 3 transacciones exitosamente
        And los holdings para el portafolio "Mi-Binance" deben ser:
          | Símbolo | Cantidad Total |
          | BTC     | 0.0999         | # 0.1 (compra) - 0.0001 (fee)
          | ETH     | 0.2995         | # 0.5 (compra) - 0.0005 (fee) - 0.2 (venta)
          | USDT    | -1390.36       | # -750 (compra ETH) - 2000 (compra BTC) + 360 (venta ETH) - 0.36 (fee)
    ```

3.  **Limpieza de Código (Refactoring):**
    *   **Acción:** Utilizar herramientas de análisis estático para identificar código no utilizado (clases, métodos) en el backend y marcarlo para su eliminación.
    *   **Acción:** Simplificar la estructura si es posible, eliminando complejidad innecesaria para centrarnos en el flujo principal.

## Fase 2: Implementación del Núcleo (MVP)

*Objetivo: Tener una versión funcional y verificada del caso de uso principal.*

1.  **Implementar el Escenario BDD:**
    *   **Acción:** Conectar el escenario Gherkin a una prueba de integración real que ejecute el flujo completo.
    *   **Acción:** Ajustar los `Adapters` (ej. `BinanceTransactionAdapter`) y los servicios para que pasen la prueba del escenario BDD, asegurando que los cálculos de holdings (restando fees, sumando y restando activos) sean precisos.

2.  **Verificación End-to-End:**
    *   **Acción:** Asegurar que el frontend pueda subir el archivo y mostrar los holdings calculados por el backend, tal como se definen en la prueba BDD.

## Fase 3: Expansión de Funcionalidades

*Objetivo: Añadir valor sobre la base sólida ya construida.*

1.  **Cálculo de Ganancias y Pérdidas (PNL):**
    *   **Acción:** Definiremos un nuevo escenario BDD para calcular el PNL de una venta.
    *   **Acción:** Implementaremos la lógica (requiere conocer el costo de adquisición).

2.  **Valoración del Portafolio:**
    *   **Acción:** Definiremos un escenario BDD para obtener el valor total del portafolio en USDT/USD.
    *   **Acción:** Integraremos una API de precios de mercado (como la de CryptoCompare que vi en las pruebas) para obtener los precios actuales y valorar los holdings.

## Fase 4: Despliegue y Operación

*Objetivo: Poner la aplicación en producción.*

1.  **Configuración de CI/CD (Integración y Despliegue Continuo):**
    *   **Acción:** Crear un pipeline (ej. con GitHub Actions) que ejecute las pruebas automáticamente con cada cambio.

2.  **Contenerización y Orquestación:**
    *   **Acción:** Revisar y finalizar los `Dockerfile` y `docker-compose.yml` para asegurar que la aplicación (BE, FE, DB) se pueda levantar de forma consistente.

3.  **Despliegue en un Servidor Cloud:**
    *   **Acción:** Elegir una plataforma (ej. AWS, Google Cloud, Heroku) y desplegaremos la aplicación contenerizada.
