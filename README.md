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
