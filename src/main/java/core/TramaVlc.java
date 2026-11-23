package core;

import java.util.Arrays;

/**
 * Modelo inmutable de una trama VLC: START + LONGITUD + PAYLOAD + CRC.
 * Encapsula la estructura definida en el protocolo, evitando arrays sueltos
 * por todo el código (antes se accedía con trama[0], trama[1], ...).
 */
public final class TramaVlc {

    public static final byte DELIMITADOR_START = 0x7E;
    public static final int MAX_PAYLOAD = 16;

    private final byte[] payload;
    private final byte crc;

    private TramaVlc(byte[] payload, byte crc) {
        this.payload = payload.clone();
        this.crc = crc;
    }

    public static TramaVlc desdePayload(byte[] payload) {
        validarPayload(payload);
        byte[] cuerpo = construirCuerpo(payload);
        return new TramaVlc(payload, ProtocoloVlc.calcularCRC8(cuerpo));
    }

    /** Reconstruye una trama a partir de los bytes crudos leídos del medio. */
    public static TramaVlc desdeBytesCrudos(byte[] bytes) {
        if (bytes == null || bytes.length < 3) {
            throw new IllegalArgumentException("Trama demasiado corta");
        }
        int len = bytes[1] & 0xFF;
        byte[] payload = Arrays.copyOfRange(bytes, 2, 2 + len);
        return new TramaVlc(payload, bytes[bytes.length - 1]);
    }

    public byte[] getPayload() { return payload.clone(); }
    public byte getCrc()        { return crc; }
    public int getLongitud()    { return payload.length; }
    public byte getByteStart()  { return DELIMITADOR_START; }
    public byte getByteLongitud() { return (byte) payload.length; }

    /** Serializa a bytes listos para transmitir por el puerto serie. */
    public byte[] aBytesTransmision() {
        byte[] cuerpo = construirCuerpo(payload);
        byte[] trama = new byte[cuerpo.length + 2];
        trama[0] = DELIMITADOR_START;
        System.arraycopy(cuerpo, 0, trama, 1, cuerpo.length);
        trama[trama.length - 1] = crc;
        return trama;
    }

    public byte[] calcularCuerpoParaCrc() {
        return construirCuerpo(payload);
    }

    private static byte[] construirCuerpo(byte[] payload) {
        byte[] cuerpo = new byte[payload.length + 1];
        cuerpo[0] = (byte) payload.length;
        System.arraycopy(payload, 0, cuerpo, 1, payload.length);
        return cuerpo;
    }

    private static void validarPayload(byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload nulo");
        }
        if (payload.length == 0 || payload.length > MAX_PAYLOAD) {
            throw new IllegalArgumentException(
                "Longitud de payload fuera de rango [1, " + MAX_PAYLOAD + "]");
        }
    }
}