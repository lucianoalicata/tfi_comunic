package com.mycompany.tfi_comunic;

/*Contiene las operaciones del protocolo de la aplicación VLC:
construcción de tramas, conversión binaria y cálculo CRC-8*/
public final class ProtocoloVlc {
    private static final byte DELIMITADOR_INICIO = 0x7E;

    private ProtocoloVlc(){
    }

    //Construye una trama: inicio (1 byte), longitud (1), datos y CRC (1).
    public static byte[] crearTrama(byte[] datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos no pueden ser null.");
        }
        if (datos.length > 255) {
            throw new IllegalArgumentException("La carga útil no puede superar 255 bytes.");
        }

        byte[] cuerpo = new byte[datos.length + 1];
        cuerpo[0] = (byte) datos.length;
        System.arraycopy(datos, 0, cuerpo, 1, datos.length);

        byte[] trama = new byte[cuerpo.length + 2];
        trama[0] = DELIMITADOR_INICIO;
        System.arraycopy(cuerpo, 0, trama, 1, cuerpo.length);
        trama[trama.length - 1] = calcularCRC8(cuerpo);
        return trama;
    }

    //Calcula CRC-8 con valor inicial 0 y polinomio 0x07.
    public static byte calcularCRC8(byte[] datos) {
        int crc = 0;
        for (byte dato : datos) {
            crc ^= dato & 0xFF;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = ((crc << 1) ^ 0x07) & 0xFF;
                } else {
                    crc = (crc << 1) & 0xFF;
                }
            }
        }
        return (byte) crc;
    }

    //Devuelve siempre una representación binaria de 8 bits.
    public static String aBinario8(int valor) {
        return String.format("%8s", Integer.toBinaryString(valor & 0xFF)).replace(' ', '0');
    }
}
