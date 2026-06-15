# Plan de Proyecto para GitHub: Gestor de Portfolio Crypto

## Épica 1: Estabilización de la Plataforma (La Base)

**Objetivo:** Lograr una base de código estable con una suite de tests fiable. Sin esto, cualquier desarrollo futuro es arriesgado y costoso. Es la máxima prioridad.

### Historia de Usuario 1.1: Como equipo, necesitamos que todos los tests pasen para garantizar que la lógica existente es correcta y evitar regresiones.

- **[ ] Tarea Técnica 1.1.1:** Resolver la causa raíz de los fallos de `DataIntegrityViolationException` en los tests de integración.
  - *Contexto:* Los tests fallan porque los `Portfolio` se crean sin un `User` asociado, violando una restricción `NOT NULL` en la base de datos.
- **[ ] Tarea Técnica 1.1.2:** Corregir la cascada de fallos en los tests unitarios de `HoldingServiceSpec`.
  - *Contexto:* La lógica para actualizar los holdings (`updatePaidWithHolding`) parece estar rota, causando múltiples fallos. Es crucial para el cálculo del portfolio.
- **[ ] Tarea Técnica 1.1.3:** Estabilizar la integración con el servicio de precios en `CryptoCompareProxyIntegrationSpec`.
  - *Contexto:* Los tests del proxy que obtiene los precios de mercado fallan con `NullPointerException`. Sin precios, no se puede valorar el portfolio.
- **[ ] Tarea Técnica 1.1.4:** Solucionar los `AssertionError` en `TransactionControllerIntegrationSpec`.
  - *Contexto:* Los endpoints de la API no devuelven los resultados esperados, probablemente debido a los fallos en la lógica subyacente.
- **[ ] Tarea Técnica 1.1.5:** Una vez resueltos los bloqueos principales, revisar y corregir sistemáticamente todos los tests restantes que figuran como fallidos.

---

## Épica 2: Cálculo Preciso de Holdings (El MVP del Inversor)

**Objetivo:** Implementar la funcionalidad central que un inversor necesita: saber exactamente cuántos activos posee a partir de su historial de transacciones.

### Historia de Usuario 2.1: Como inversor, quiero subir un archivo CSV de transacciones para que el sistema calcule automáticamente mis tenencias actuales.

- **[ ] Tarea Técnica 2.1.1:** Escribir un escenario de prueba de comportamiento (BDD con Gherkin) que defina inequívocamente cómo se debe procesar un CSV simple (compras, ventas, comisiones) y cuál es el cálculo de holdings esperado.
- **[ ] Tarea Técnica 2.1.2:** Implementar la lógica de los `Adapters` (ej. `BinanceTransactionAdapter`) para que interpreten correctamente los datos del CSV según el formato definido.
- **[ ] Tarea Técnica 2.1.3:** Reforzar el `TransactionService` y el `HoldingService` para que la lógica de negocio (sumar compras, restar ventas, deducir comisiones) pase el escenario BDD.
- **[ ] Tarea Técnica 2.1.4:** Crear un endpoint de API robusto que gestione la subida del archivo CSV y dispare el proceso de cálculo.

### Historia de Usuario 2.2: Como inversor, después de procesar mis transacciones, quiero ver un resumen claro de mis tenencias totales por cada criptomoneda.

- **[ ] Tarea Técnica 2.2.1:** Diseñar y validar el DTO de respuesta (`CoinInformationResponse`) para asegurar que contenga toda la información necesaria: Nombre del activo, Cantidad total, Costo total, etc.
- **[ ] Tarea Técnica 2.2.2:** Implementar el endpoint `GET /api/portfolio/{portfolioName}/holdings` que devuelva la lista de tenencias calculadas para un portfolio específico.

---

## Épica 3: Valoración y Análisis del Portfolio (Generación de Valor)

**Objetivo:** Ir más allá de la simple cantidad de activos y proporcionar al inversor información clave sobre el valor y el rendimiento de su cartera.

### Historia de Usuario 3.1: Como inversor, quiero ver el valor actual de mercado de cada una de mis tenencias y el valor total de mi portfolio en una moneda estable (USD/USDT).

- **[ ] Tarea Técnica 3.1.1:** Implementar la lógica en el `PricingFacade` para usar el proxy de precios (una vez estabilizado) y calcular el valor de mercado actual de cada holding.
- **[ ] Tarea Técnica 3.1.2:** Añadir un mecanismo de caché para los precios de las criptomonedas para evitar llamadas excesivas a la API externa, mejorar el rendimiento y reducir costos.
- **[ ] Tarea Técnica 3.1.3:** Extender el endpoint de holdings para que incluya el valor actual en USD/USDT de cada activo y el valor total del portfolio.

### Historia de Usuario 3.2: Como inversor, quiero conocer mis ganancias o pérdidas realizadas por las ventas que he hecho, para entender el rendimiento de mis operaciones.

- **[ ] Tarea Técnica 3.2.1:** Abordar las tareas de la deuda técnica en `docs/tasks.md` relacionadas con el cálculo de PNL (Ganancias y Pérdidas), como "Add support for calculating realized and unrealized gains/losses".
- **[ ] Tarea Técnica 3.2.2:** Implementar la lógica para calcular el costo promedio de adquisición (base de costo) de los activos.
- **[ ] Tarea Técnica 3.2.3:** Al procesar una venta, calcular la ganancia o pérdida realizada comparando el precio de venta con la base de costo y mostrarlo en la respuesta de la API.
