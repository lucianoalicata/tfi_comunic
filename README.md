# Sistema VLC — Control de errores en la capa de enlace mediante CRC-8

Aplicación de escritorio en **Java Swing** para enviar y recibir datos por puerto serie, visualizar bytes en binario y comprobar la integridad de las tramas mediante **CRC-8**.

La organización separa interfaz, transporte y protocolo en clases independientes para facilitar la lectura y el mantenimiento del código.

## 🎯 Objetivo

Mostrar cómo se encapsulan datos en una trama y cómo un código de redundancia cíclica puede detectar alteraciones accidentales durante la transmisión.

> CRC-8 es un mecanismo de **detección**, no de corrección de errores ni de autenticación.

## ✨ Funcionalidades

- Interfaz gráfica con Java Swing.
- Detección y apertura de un puerto serie mediante jSerialComm.
- Comunicación configurada a **9600 baudios, 8N1**.
- Armado y envío de tramas con delimitador, longitud, datos y CRC-8.
- Recepción de bytes en un hilo de escucha.
- Representación de bytes como cadenas binarias de ocho bits.
- Presentación de estados y resultados de la comprobación en la interfaz.

## 🧰 Tecnologías

| Tecnología | Función |
|---|---|
| Java | Lenguaje de programación |
| Java Swing | Interfaz gráfica |
| jSerialComm | Acceso al puerto serie |
| UART / serial | Transporte de bytes |
| CRC-8 | Detección de errores |

## 🗂️ Estructura del proyecto

Todos los archivos pertenecen al paquete `com.mycompany.tfi_comunic`.

```text
src/main/java/com/mycompany/tfi_comunic/
├── VlcApp.java
├── ComunicacionSerie.java
└── ProtocoloVlc.java
```

### `VlcApp.java` — Interfaz y coordinación

Construye la ventana, presenta datos y estados, coordina el envío y muestra los resultados recibidos y de la verificación. Mantiene la lógica de presentación separada de las operaciones específicas del protocolo y del puerto serie.

### `ComunicacionSerie.java` — Transporte

Se encarga de buscar puertos serie, seleccionar el primero detectado, configurar 9600 baudios, abrir la conexión, enviar bytes y escuchar datos recibidos. La lectura se realiza en un hilo separado y los bytes se entregan a la aplicación mediante un `Consumer<byte[]>`.

Esta clase gestiona el transporte; no calcula el CRC ni construye la trama.

### `ProtocoloVlc.java` — Protocolo y algoritmos

Contiene las operaciones reutilizables del protocolo:

- `crearTrama(byte[] datos)`: construye una trama completa.
- `calcularCRC8(byte[] datos)`: calcula el CRC-8.
- `aBinario8(int valor)`: devuelve una representación binaria de ocho bits.

Esta separación hace que la lógica del protocolo pueda revisarse y probarse sin depender directamente de la interfaz gráfica.

## 📦 Formato de la trama

```text
┌─────────────┬────────────┬─────────────────┬─────────────┐
│ Delimitador │  Longitud  │  Carga útil     │    CRC-8    │
│   1 byte    │  1 byte    │  0–255 bytes    │   1 byte    │
└─────────────┴────────────┴─────────────────┴─────────────┘
```

| Campo | Tamaño | Descripción |
|---|---:|---|
| Delimitador | 1 byte | Valor fijo `0x7E`, marca el inicio |
| Longitud | 1 byte | Cantidad de bytes de la carga útil |
| Carga útil | 0–255 bytes | Datos que se transmiten |
| CRC-8 | 1 byte | Valor calculado sobre longitud + carga útil |

La estructura general es:

```text
TRAMA = 0x7E + LONGITUD + DATOS + CRC8(LONGITUD + DATOS)
```

**Detalle importante:** el delimitador `0x7E` no forma parte de los datos usados para calcular el CRC. El cálculo se realiza sobre el byte de longitud seguido de los bytes de la carga útil.

Por ejemplo, el texto `HOLA`, codificado como cuatro bytes ASCII, lleva longitud `0x04`. El CRC se calcula sobre `0x04` y los cuatro bytes del texto.

## 🧮 Algoritmos utilizados

### 1. Construcción de tramas

El método `crearTrama()`:

1. Valida que el arreglo no sea `null` y que no supere 255 bytes.
2. Crea el cuerpo con la longitud y la carga útil.
3. Calcula el CRC-8 del cuerpo.
4. Ensambla el delimitador `0x7E`, el cuerpo y el CRC final.

### 2. CRC-8

Parámetros utilizados por el código:

| Parámetro | Valor |
|---|---|
| Ancho | 8 bits |
| Polinomio | `0x07` |
| Valor inicial | `0x00` |
| Orden de procesamiento | MSB-first |
| XOR final adicional | No se aplica |

El algoritmo comienza con CRC igual a cero. Para cada byte, combina el byte con el registro CRC mediante XOR. Luego procesa ocho bits: si el bit más significativo está activo, desplaza el registro a la izquierda y aplica XOR con `0x07`; si no, solo desplaza. Después de cada desplazamiento conserva únicamente ocho bits.

Pseudocódigo:

```text
crc = 0
para cada byte:
    crc = crc XOR byte
    repetir 8 veces:
        si el bit más significativo de crc es 1:
            crc = (crc << 1) XOR 0x07
        si no:
            crc = crc << 1
        conservar solo 8 bits
devolver crc
```

Para verificar una trama, el receptor debe recalcular el CRC usando los mismos campos y parámetros y compararlo con el byte recibido. Una diferencia indica que la comprobación falló; una coincidencia no garantiza detectar todos los posibles patrones de error.

### 3. Conversión binaria

`aBinario8()` convierte un valor en una cadena de exactamente ocho bits. Los ceros iniciales se conservan, lo que facilita inspeccionar cada byte de la trama.

### 4. Recepción asíncrona

La lectura del puerto se ejecuta en un hilo separado para evitar que la espera de datos bloquee directamente la interfaz. Las lecturas serie pueden llegar en fragmentos de tamaño variable; por eso, la lógica que interpreta las tramas debe acumular los bytes hasta disponer de una trama completa, utilizando el delimitador y la longitud.

## 🔄 Flujo general

```text
Usuario
  ↓
VlcApp.java
  ↓ solicita datos para enviar
ProtocoloVlc.java
  ↓ genera: 0x7E + longitud + datos + CRC
ComunicacionSerie.java
  ↓ transmite por el puerto serie
Dispositivo externo
  ↓ envía datos de respuesta
ComunicacionSerie.java
  ↓ entrega bytes recibidos
VlcApp.java
  ↓ interpreta y presenta resultados
Comprobación y visualización del CRC
```

## ⚙️ Requisitos

- JDK compatible con la configuración del proyecto.
- Apache NetBeans u otro IDE Java.
- Dependencia **jSerialComm** instalada en el proyecto.
- Puerto serie disponible.
- Dispositivo compatible conectado para validar la comunicación real.

## ▶️ Ejecución en NetBeans

1. Realiza una copia de seguridad del proyecto original.
2. Abre el proyecto en NetBeans.
3. En `Source Packages`, abre el paquete `com.mycompany.tfi_comunic`.
4. Coloca allí los tres archivos Java.
5. Comprueba que jSerialComm siga agregado a las dependencias.
6. Compila el proyecto.
7. Conecta el dispositivo y ejecuta `VlcApp`.
8. Comprueba el puerto seleccionado y realiza una prueba de envío y recepción.

> **Atención:** la implementación selecciona el primer puerto devuelto por jSerialComm. Si hay varios puertos disponibles, verifica que corresponda al dispositivo correcto.

## 🧪 Pruebas recomendadas

1. **Sin dispositivo:** comprobar el mensaje de ausencia de puertos.
2. **Conexión:** verificar el puerto correcto y los parámetros 9600, 8N1.
3. **Mensaje simple:** enviar `HOLA` y revisar longitud, bytes y CRC.
4. **Detección de alteraciones:** en un entorno de prueba, modificar un byte o el CRC y verificar que se informe la discrepancia.
5. **Varias lecturas:** probar mensajes que puedan recibirse en fragmentos.
6. **Caracteres acentuados:** comprobar que ambos extremos usan la misma codificación; algunos caracteres ocupan más de un byte en UTF-8.

## ⚠️ Limitaciones y consideraciones

- El intercambio con el microcontrolador **no fue probado con hardware real** en este entorno; debe validarse con el dispositivo y la biblioteca jSerialComm del proyecto.
- La selección del primer puerto puede no corresponder al dispositivo esperado.
- CRC-8 detecta errores, pero no los corrige ni autentica los datos.
- El delimitador `0x7E` podría aparecer dentro de la carga útil. La recepción debe respetar el campo de longitud para no confundir datos con el inicio de una trama.
- La longitud se representa en un byte, por lo que la carga útil está limitada a 255 bytes.
- El dispositivo externo debe implementar exactamente el mismo formato de trama y los mismos parámetros CRC.

## 📄 Licencia

Esta versión no declara una licencia. Añade la licencia correspondiente si el proyecto se distribuirá públicamente.

---

**Sistema VLC — Control de errores en la capa de enlace mediante CRC-8**  
Proyecto académico para estudiar comunicación serial, encapsulado de tramas y detección de errores.
