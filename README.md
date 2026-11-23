# Sistema de Comunicación por Luz Visible (VLC) con CRC-8

Trabajo Final Integrador de la materia **Comunicación de Datos**.

## Descripción

Implementación de un enlace punto a punto de transmisión de texto mediante luz
visible (Visible Light Communication). El proyecto demuestra de forma concreta
la interacción entre la **Capa Física (Capa 1)** y la **Capa de Enlace (Capa 2)**
del modelo OSI:

- **Capa 1**: modulación OOK sobre un LED y detección con una fotorresistencia LDR.
- **Capa 2**: entramado (framing), cálculo y verificación de CRC-8, detección
  de errores y análisis diferencial bit a bit entre lo transmitido y lo recibido.

## Objetivos

1. Implementar un canal óptico punto a punto de bajo costo usando un Arduino Uno,
   un LED de alta luminosidad y una LDR.
2. Definir e implementar un protocolo de trama con delimitador de inicio,
   longitud, carga útil y CRC-8.
3. Detectar errores de transmisión mediante CRC-8 con polinomio generador
   estándar `0x07` (x⁸ + x² + x + 1).
4. Exponer visualmente la matriz diferencial XOR entre bits transmitidos y
   recibidos, para evidenciar la propagación de errores en el medio físico.
5. Aplicar buenas prácticas de ingeniería de software: separación por capas,
   principios SOLID, patrón de responsabilidad única y código legible.

## Integrantes

- Alicata, Luciano Antonio
- Leal Alvarado, Lautaro
- Leyva, María Belén

## Arquitectura

### Hardware (Capa Física)

| Componente        | Rol                                                       |
|-------------------|-----------------------------------------------------------|
| Arduino Uno       | Transmisor y receptor; temporización y muestreo           |
| LED blanco        | Emisor OOK (pin digital 8)                                |
| LDR + divisor     | Detección óptica (pin analógico A0, lógica activo-bajo)   |
| Tubo opaco        | Aislamiento del canal de la luz ambiental                 |

Parámetros del canal:

- Tiempo de bit: 100–150 ms, con muestreo centrado en `BIT_MS / 2`.
- Umbral dinámico: calibración automática en `setup()` promediando la lectura
  con LED apagado y LED encendido.
- Modulación: OOK (LED encendido = 1, apagado = 0).

### Software (Capa de Enlace + Aplicación)

- **Java 17 + Swing + Maven**.
- Comunicación serie con `jSerialComm` (9600 bps, 8N1).
- Estructura de paquetes:

com.mycompany.tfi_comunic
├── TfiComunicApplication.java → entrada
├── core/ → protocolo puro (CRC, trama)
├── io/ → transporte serie
├── service/ → orquestadores de envío/recepción
├── view/ → UI Swing (paneles + tema)
└── util/ → utilidades genéricas

text

## Estructura de la trama

| Campo      | Tamaño       | Descripción                                       |
|------------|--------------|---------------------------------------------------|
| START      | 8 bits       | Delimitador fijo `01111110` (`0x7E`)              |
| LONGITUD   | 8 bits       | Cantidad `N` de caracteres de payload (1–16)      |
| PAYLOAD    | `8 × N` bits | Caracteres del mensaje en ASCII/UTF-8             |
| CRC-8      | 8 bits       | CRC calculado sobre `[LONGITUD + PAYLOAD]`        |

El CRC-8 usa valor inicial `0x00` y polinomio generador `0x07`.

## Requisitos

- JDK 17 o superior.
- Maven 3.8 o superior.
- Arduino IDE (para cargar el sketch del microcontrolador).
- Biblioteca `jSerialComm` (se resuelve vía Maven, ver `pom.xml`).
- Placa Arduino Uno con el firmware VLC cargado.
- Cable USB para conectar la placa a la PC.

## Cómo levantar el proyecto

1. Clonar el repositorio.
2. Conectar el Arduino al puerto USB y cargar el sketch correspondiente.
3. Compilar:

   ```bash
   mvn clean package
Ejecutar:

bash
mvn exec:java -Dexec.mainClass="com.mycompany.tfi_comunic.TfiComunicApplication"
o bien, desde el IDE, ejecutar TfiComunicApplication.main().

Cómo probarlo
Verificar que la placa esté conectada y el puerto serie libre.

Al iniciar la aplicación se intentará abrir el primer puerto disponible;
el badge superior mostrará el estado de conexión.

Escribir un mensaje de 1 a 16 caracteres en el campo inferior y presionar
TRANSMITIR TRAMA VLC.

Observar en el panel superior izquierdo (TX) el desglose binario nominal
y en el panel superior derecho (RX) el binario recibido y la matriz
diferencial XOR.

En el panel inferior se muestra el resultado del CRC-8:

Enlace íntegro: los CRC coinciden y no hay bits alterados.

Trama descartada: se muestra la diferencia entre CRC recibido y
recalculado, y una reconstrucción didáctica del mensaje original.

Para forzar errores, basta con tapar parcialmente el LED o introducir
interferencia lumínica durante la transmisión.

Notas sobre el diseño
El código está organizado por capas: el protocolo (core) no conoce Swing,
la UI (view) no conoce SerialPort y la capa io no conoce el protocolo.

El CRC-8 detecta errores pero no corrige; la reconstrucción mostrada es
didáctica y usa el buffer original en memoria.

Los principios SOLID aplicados son: SRP (cada clase tiene una sola
responsabilidad), OCP (paneles extensibles), LSP/ISP/DIP (interfaces
ListenerRecepcion, ListenerValidacion, records de resultado).
