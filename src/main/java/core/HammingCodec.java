package core;

import java.util.ArrayList;
import java.util.List;

/**
 * Código de Hamming (n+p, n) = (12, 8) con paridad PAR, siguiendo el esquema
 * de la cátedra (Unidad 6, punto 12):
 *
 *   - Se cumple 2^p >= n + p + 1  ->  p = 4, n = 8  ->  16 >= 13.
 *   - Los bits de paridad ocupan las posiciones 1, 2, 4, 8 (potencias de 2).
 *   - Los bits de datos n1..n8 ocupan 3, 5, 6, 7, 9, 10, 11, 12 (n1 = LSB).
 *   - El bit pk controla las posiciones cuyo número binario tiene el bit k en 1.
 *   - En el receptor, los p controles forman el SÍNDROME: su valor decimal es
 *     la posición del bit erróneo (0 = sin error).
 *
 * Corrige 1 error por palabra. Con 2 errores en la misma palabra puede
 * "corregir" mal o marcarla como incorregible; ahí actúa el CRC-8.
 *
 * Cada palabra de 12 bits viaja en 2 bytes: [0000 p12..p9][p8..p1].
 */
public final class HammingCodec {

    public static final int BITS_DATOS = 8;
    public static final int BITS_PARIDAD = 4;
    public static final int BITS_PALABRA = BITS_DATOS + BITS_PARIDAD;

    /** Posición (1..12) de cada bit de dato n1..n8 (índice 0 = n1 = LSB). */
    private static final int[] POS_DATO = {3, 5, 6, 7, 9, 10, 11, 12};

    public enum Estado { SIN_ERROR, CORREGIDO, INCORREGIBLE }

    /** Resultado de decodificar una palabra recibida. */
    public record Palabra(
            int palabraRx,         // 12 bits tal como llegaron
            int sindrome,          // s4 s3 s2 s1 (0..15)
            int posicionError,     // 1..12 si se corrigió, 0 si no hubo corrección
            Estado estado,
            int palabraCorregida,  // 12 bits tras corregir
            int datoRecibido,      // n sin corregir (lo que entregó el canal)
            int datoCorregido) { } // n tras corregir

    private HammingCodec() { }

    // ---------------------------------------------------------------- bits

    /** Bit en la posición {@code pos} (1..12) de la palabra. */
    public static int bit(int palabra, int pos) {
        return (palabra >> (pos - 1)) & 1;
    }

    public static boolean esPosicionParidad(int pos) {
        return (pos & (pos - 1)) == 0;
    }

    /** Etiqueta de la columna: p1..p4 y n1..n8 (numeración de la cátedra). */
    public static String etiqueta(int pos) {
        if (esPosicionParidad(pos)) {
            return "p" + (Integer.numberOfTrailingZeros(pos) + 1);
        }
        for (int i = 0; i < BITS_DATOS; i++) {
            if (POS_DATO[i] == pos) return "n" + (i + 1);
        }
        throw new IllegalArgumentException("Posición inválida: " + pos);
    }

    /** ¿El bit de paridad pk (k = 1..4) controla la posición {@code pos}? */
    public static boolean controla(int k, int pos) {
        return (pos & (1 << (k - 1))) != 0;
    }

    /** XOR (paridad par) de las posiciones controladas por pk. */
    public static int paridadGrupo(int palabra, int k) {
        int x = 0;
        for (int pos = 1; pos <= BITS_PALABRA; pos++) {
            if (controla(k, pos)) x ^= bit(palabra, pos);
        }
        return x;
    }

    // ------------------------------------------------------ una palabra

    /** Arma la palabra de 12 bits con los datos y los 4 bits de paridad. */
    public static int codificar(int dato) {
        int palabra = 0;
        for (int i = 0; i < BITS_DATOS; i++) {
            if (((dato >> i) & 1) == 1) palabra |= 1 << (POS_DATO[i] - 1);
        }
        for (int k = 1; k <= BITS_PARIDAD; k++) {
            int pos = 1 << (k - 1);
            palabra |= paridadGrupo(palabra, k) << (pos - 1);
        }
        return palabra;
    }

    public static int extraerDato(int palabra) {
        int dato = 0;
        for (int i = 0; i < BITS_DATOS; i++) {
            dato |= bit(palabra, POS_DATO[i]) << i;
        }
        return dato;
    }

    /** Síndrome: bit k-1 = paridad de las posiciones controladas por pk. */
    public static int sindrome(int palabra) {
        int s = 0;
        for (int k = 1; k <= BITS_PARIDAD; k++) {
            s |= paridadGrupo(palabra, k) << (k - 1);
        }
        return s;
    }

    public static Palabra decodificar(int palabraRx) {
        palabraRx &= (1 << BITS_PALABRA) - 1;
        int s = sindrome(palabraRx);
        Estado estado;
        int pos = 0;
        int corregida = palabraRx;
        if (s == 0) {
            estado = Estado.SIN_ERROR;
        } else if (s <= BITS_PALABRA) {
            estado = Estado.CORREGIDO;
            pos = s;
            corregida ^= 1 << (s - 1);
        } else {
            estado = Estado.INCORREGIBLE; // síndrome 13..15: no apunta a ninguna posición
        }
        return new Palabra(palabraRx, s, pos, estado, corregida,
                extraerDato(palabraRx), extraerDato(corregida));
    }

    // ------------------------------------------------------ una trama

    /** Cantidad de bytes que viajan por el canal para una trama de {@code bytesTrama}. */
    public static int bytesCodificados(int bytesTrama) {
        return 1 + 2 * (bytesTrama - 1);
    }

    /**
     * [START][LONGITUD][PAYLOAD...][CRC]  ->  [START] + 2 bytes por cada byte restante.
     * START queda sin codificar: es el delimitador de sincronismo.
     */
    public static byte[] codificarTrama(byte[] trama) {
        byte[] out = new byte[bytesCodificados(trama.length)];
        out[0] = trama[0];
        for (int i = 1; i < trama.length; i++) {
            int palabra = codificar(trama[i] & 0xFF);
            out[1 + 2 * (i - 1)] = (byte) ((palabra >> 8) & 0x0F);
            out[2 + 2 * (i - 1)] = (byte) (palabra & 0xFF);
        }
        return out;
    }

    /** Decodifica el cuerpo (todo lo que sigue a START) palabra por palabra. */
    public static List<Palabra> decodificarCuerpo(byte[] codificada) {
        List<Palabra> palabras = new ArrayList<>();
        for (int i = 1; i + 1 < codificada.length; i += 2) {
            int palabra = ((codificada[i] & 0x0F) << 8) | (codificada[i + 1] & 0xFF);
            palabras.add(decodificar(palabra));
        }
        return palabras;
    }
}
