
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
