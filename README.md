# Sistema de Comunicación por Luz Visible (VLC) con CRC-8

**Trabajo Final Integrador de la materia Comunicación de Datos**

## Descripción

Implementación de un enlace punto a punto de transmisión de texto
mediante luz visible (*Visible Light Communication*). El proyecto
demuestra de forma concreta la interacción entre la **Capa Física (Capa
1)** y la **Capa de Enlace (Capa 2)** del modelo OSI:

-   **Capa 1:** modulación OOK sobre un LED y detección con una
    fotorresistencia LDR.
-   **Capa 2:** entramado (*framing*), cálculo y verificación de CRC-8,
    detección de errores y análisis diferencial bit a bit entre lo
    transmitido y lo recibido.

## Objetivos

1.  Implementar un canal óptico punto a punto de bajo costo usando un
    Arduino Uno, un LED de alta luminosidad y una LDR.
2.  Definir e implementar un protocolo de trama con delimitador de
    inicio, longitud, carga útil y CRC-8.
3.  Detectar errores de transmisión mediante CRC-8 con polinomio
    generador estándar `0x07` ((x\^8 + x\^2 + x + 1)).
4.  Exponer visualmente la matriz diferencial XOR entre bits
    transmitidos y recibidos, para evidenciar la propagación de errores
    en el medio físico.
5.  Aplicar buenas prácticas de ingeniería de software: separación por
    capas, principios SOLID, patrón de responsabilidad única y código
    legible.

## Integrantes

-   Alicata, Luciano Antonio
-   Leal Alvarado, Lautaro
-   Leyva, María Belén

## Arquitectura

### Hardware (Capa Física)

  -----------------------------------------------------------------------
  Componente                          Rol
  ----------------------------------- -----------------------------------
  Arduino Uno                         Transmisor y receptor;
                                      temporización y muestreo

  LED blanco                          Emisor OOK (pin digital 8)

  LDR + divisor                       Detección óptica (pin analógico A0,
                                      lógica activo-bajo)

  Tubo opaco                          Aislamiento del canal de la luz
                                      ambiental
  -----------------------------------------------------------------------

**Parámetros del canal:**

-   **Tiempo de bit:** 100--150 ms, con muestreo centrado en
    `BIT_MS / 2`.
-   **Umbral dinámico:** calibración automática en `setup()` promediando
    la lectura con LED apagado y LED encendido.
-   **Modulación:** OOK (LED encendido = 1, apagado = 0).

### Software (Capa de Enlace + Aplicación)

-   **Tecnologías:** Java 21 + Swing + Maven.
-   **Comunicación serie:** `jSerialComm` (9600 bps, 8N1).

**Estructura de paquetes:**

``` text
src/main/java
├── com/mycompany/tfi_comunic/Application.java → entrada
├── core/                       → protocolo puro (CRC-8, trama, Hamming)
├── io/                         → transporte serie
├── service/                    → orquestadores de envío/recepción
└── view/                       → UI Swing (paneles + tema)
```

## Estructura de la trama

  Campo              Tamaño Descripción
  ---------- -------------- -----------------------------------------------
  START              8 bits Delimitador fijo `01111110` (`0x7E`)
  LONGITUD           8 bits Cantidad `N` de caracteres de payload (1--16)
  PAYLOAD      `8 × N` bits Caracteres del mensaje en ASCII/UTF-8
  CRC-8              8 bits CRC calculado sobre `[LONGITUD + PAYLOAD]`

El CRC-8 usa valor inicial `0x00` y polinomio generador `0x07`.

## Corrección de errores: FEC Hamming (12,8)

Además de detectar con CRC-8, el enlace puede **corregir hacia adelante**
con un código de Hamming (esquema de la cátedra, Unidad 6, punto 12):

-   `n = 8` bits de datos y `p = 4` de paridad (`2^p >= n + p + 1`,
    `16 >= 13`), paridad **par**.
-   Los bits de paridad ocupan las posiciones 1, 2, 4 y 8; los de datos,
    las posiciones 3, 5, 6, 7, 9, 10, 11 y 12.
-   Cada byte del cuerpo de la trama (`LONGITUD`, `PAYLOAD`, `CRC-8`) se
    transmite como una palabra de 12 bits dentro de 2 bytes
    (`0000 + 12 bits`). `START` viaja sin codificar.
-   En recepción, los 4 controles de paridad forman el **síndrome**; su
    valor decimal es la posición del bit erróneo (0 = sin error).
-   Después de corregir se recalcula el CRC-8 sobre la trama corregida:
    si coincide, el mensaje se entrega; si no, se descarta (ARQ).

Hamming corrige **1 bit por palabra**. Con 2 o más errores en la misma
palabra (por ejemplo, una ráfaga de luz) puede corregir mal o marcarla
como incorregible; en ese caso el CRC-8 detecta el problema.

La interfaz tiene un casillero **FEC Hamming (12,8)** (activado por
defecto). Desactivado, el enlace funciona solo con CRC-8 (detecta y
descarta). La trama enviada con FEC activado ocupa casi el doble de
bytes, por lo que la transmisión tarda proporcionalmente más.

## Requisitos

-   JDK 21 o superior.
-   Maven 3.8 o superior.
-   Arduino IDE (para cargar el sketch del microcontrolador).
-   Biblioteca `jSerialComm` (se resuelve vía Maven, ver `pom.xml`).
-   Placa Arduino Uno con el firmware VLC cargado.
-   Cable USB para conectar la placa a la PC.

## Cómo levantar el proyecto

1.  Clonar el repositorio.

2.  Conectar el Arduino al puerto USB y cargar el sketch
    correspondiente.

3.  Compilar el proyecto:

    ``` bash
    mvn clean package
    ```

4.  Ejecutar la aplicación:

    ``` bash
    mvn exec:java -Dexec.mainClass="com.mycompany.tfi_comunic.Application"
    ```

    También se puede ejecutar desde el IDE mediante
    `Application.main()`.

## Cómo probarlo

1.  Verificar que la placa esté conectada y que el puerto serie esté
    libre.
2.  Al iniciar la aplicación, se intentará abrir el primer puerto
    disponible; el *badge* superior mostrará el estado de conexión.
3.  Escribir un mensaje de 1 a 16 caracteres en el campo inferior y
    presionar **TRANSMITIR TRAMA VLC**.
4.  Observar en el panel superior izquierdo (TX) el desglose binario
    nominal y, en el panel superior derecho (RX), el binario recibido y
    la matriz diferencial XOR.
5.  En el panel inferior izquierdo se muestra el procedimiento de Hamming
    (tabla de codificación, controles de paridad, síndrome y bit
    corregido) y en el inferior derecho el resultado del CRC-8:
    -   **Enlace íntegro:** los CRC coinciden y no hay bits alterados.
    -   **Corregida por Hamming:** se corrigieron bits y el CRC-8 lo
        confirma; se muestra el mensaje corregido.
    -   **Trama descartada:** el CRC-8 sigue sin coincidir tras la
        corrección (o el FEC está desactivado); no se entrega el mensaje.
6.  Para forzar errores, basta con tapar parcialmente el LED o
    introducir interferencia lumínica durante la transmisión.

## Notas sobre el diseño

El código está organizado por capas:

-   El protocolo (`core`) no conoce Swing.
-   La interfaz gráfica (`view`) no conoce `SerialPort`.
-   La capa `io` no conoce el protocolo.

El CRC-8 detecta errores pero no los corrige; la corrección la hace
Hamming usando únicamente los bits recibidos. La trama transmitida solo
se usa para mostrar la matriz diferencial XOR y la verificación de
laboratorio.

**Principios SOLID aplicados:**

-   **SRP (Single Responsibility Principle):** cada clase tiene una sola
    responsabilidad.
-   **OCP (Open/Closed Principle):** paneles extensibles.
-   **LSP (Liskov Substitution Principle), ISP (Interface Segregation
    Principle) y DIP (Dependency Inversion Principle):** aplicados
    mediante las interfaces `ListenerRecepcion`, `ListenerValidacion` y
    los *records* de resultado.
