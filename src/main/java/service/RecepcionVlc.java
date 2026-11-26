package service;

import core.HammingCodec;
import core.ModoEnlace;
import core.ProtocoloVlc;
import core.TramaVlc;
import io.ComunicacionSerie;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Acumula bytes hasta completar una trama y emite el resultado
 * al consumidor (la UI). Aquí vive la lógica de "cuándo validar".
 *
 * Con ModoEnlace.CRC_HAMMING el receptor primero corrige con Hamming
 * (usando solo los bits recibidos) y recién después verifica el CRC-8
 * sobre la trama corregida. La trama esperada solo se usa para comparar
 * contra lo que realmente se transmitió (matriz XOR / verificación de laboratorio).
 */
public class RecepcionVlc implements ComunicacionSerie.ListenerRecepcion {

    public record ResultadoValidacion(
            TramaVlc tramaRecibida,   // datos tal como salieron del canal (sin corregir)
            TramaVlc tramaCorregida,  // tras Hamming (igual a tramaRecibida si el modo es SOLO_CRC)
            TramaVlc tramaEsperada,   // lo que se envió (solo para el laboratorio)
            byte crcCalculado,        // CRC-8 recalculado sobre tramaCorregida
            boolean estructuraValida, // START correcto y LONGITUD coherente
            boolean huboErrorFisico,  // lo recibido (sin corregir) difiere de lo enviado
            ModoEnlace modo,
            List<HammingCodec.Palabra> palabras) { } // vacío si el modo es SOLO_CRC

    public interface ListenerValidacion {
        void onTramaValidada(ResultadoValidacion resultado);
    }

    /** Envío en curso: se guarda junto para que trama y modo nunca queden desfasados. */
    private record EnvioPendiente(TramaVlc trama, ModoEnlace modo) { }

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final ListenerValidacion listener;
    private volatile EnvioPendiente pendiente;

    public RecepcionVlc(ListenerValidacion listener) {
        this.listener = listener;
    }

    public void registrarEnvio(TramaVlc trama, ModoEnlace modo) {
        this.pendiente = new EnvioPendiente(trama, modo);
        this.buffer.reset();
    }

    @Override
    public void onBytesRecibidos(byte[] bytes) {
        buffer.write(bytes, 0, bytes.length);
        byte[] acumulado = buffer.toByteArray();
        EnvioPendiente envio = pendiente;
        if (envio == null) return;

        int bytesTrama = envio.trama().getLongitud() + 3;
        int tamEsperado = (envio.modo() == ModoEnlace.CRC_HAMMING)
                ? HammingCodec.bytesCodificados(bytesTrama)
                : bytesTrama;

        if (acumulado.length >= tamEsperado) {
            byte[] crudos = Arrays.copyOf(acumulado, tamEsperado);
            listener.onTramaValidada(validar(crudos, envio));

            buffer.reset();
            if (acumulado.length > tamEsperado) {
                buffer.write(acumulado, tamEsperado, acumulado.length - tamEsperado);
            }
        }
    }

    private ResultadoValidacion validar(byte[] crudos, EnvioPendiente envio) {
        TramaVlc esperada = envio.trama();
        boolean startOk = crudos[0] == TramaVlc.DELIMITADOR_START;

        TramaVlc recibida;
        TramaVlc corregida;
        List<HammingCodec.Palabra> palabras;

        if (envio.modo() == ModoEnlace.CRC_HAMMING) {
            palabras = HammingCodec.decodificarCuerpo(crudos);
            byte[] sinCorregir = new byte[palabras.size() + 1];
            byte[] corregidos = new byte[palabras.size() + 1];
            sinCorregir[0] = crudos[0];
            corregidos[0] = crudos[0];
            for (int i = 0; i < palabras.size(); i++) {
                sinCorregir[i + 1] = (byte) palabras.get(i).datoRecibido();
                corregidos[i + 1] = (byte) palabras.get(i).datoCorregido();
            }
            recibida = TramaVlc.desdeBytesCrudos(sinCorregir);
            corregida = TramaVlc.desdeBytesCrudos(corregidos);
        } else {
            palabras = List.of();
            recibida = TramaVlc.desdeBytesCrudos(crudos);
            corregida = recibida;
        }

        byte crcCalc = ProtocoloVlc.calcularCRC8(corregida.calcularCuerpoParaCrc());
        boolean estructuraOk = startOk && corregida.getLongitud() == esperada.getLongitud();
        boolean errorFisico = !Arrays.equals(recibida.getPayload(), esperada.getPayload())
                || huboAlteracionEnParidad(palabras, esperada);
        return new ResultadoValidacion(recibida, corregida, esperada, crcCalc,
                estructuraOk, errorFisico, envio.modo(), palabras);
    }

    /** En modo Hamming, el canal también puede alterar bits de paridad sin tocar los datos. */
    private boolean huboAlteracionEnParidad(List<HammingCodec.Palabra> palabras, TramaVlc esperada) {
        byte[] tx = esperada.aBytesTransmision();
        for (int i = 0; i < palabras.size(); i++) {
            if (palabras.get(i).palabraRx() != HammingCodec.codificar(tx[i + 1] & 0xFF)) {
                return true;
            }
        }
        return false;
    }
}
