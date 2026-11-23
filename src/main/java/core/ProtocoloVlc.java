package core;

/**
 * Operaciones puras del protocolo VLC (sin estado, sin I/O).
 * Responsabilidad única: cálculo de CRC-8 y conversión binaria.
 */
public final class ProtocoloVlc {

    private static final int POLINOMIO_CRC8 = 0x07;

    private ProtocoloVlc() { }

    /** CRC-8 con valor inicial 0 y polinomio 0x07 (x^8 + x^2 + x + 1). */
    public static byte calcularCRC8(byte[] datos) {
        int crc = 0;
        for (byte dato : datos) {
            crc ^= dato & 0xFF;
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x80) != 0)
                        ? ((crc << 1) ^ POLINOMIO_CRC8) & 0xFF
                        : (crc << 1) & 0xFF;
            }
        }
        return (byte) crc;
    }

    public static String aBinario8(int valor) {
        return String.format("%8s", Integer.toBinaryString(valor & 0xFF))
                     .replace(' ', '0');
    }
}