package core;

/** Estrategia de control de errores que usa el enlace. */
public enum ModoEnlace {
    /** Solo detección: CRC-8. Si falla, la trama se descarta (ARQ). */
    SOLO_CRC,
    /** Corrección hacia adelante (FEC) con Hamming (12,8) + verificación con CRC-8. */
    CRC_HAMMING
}
