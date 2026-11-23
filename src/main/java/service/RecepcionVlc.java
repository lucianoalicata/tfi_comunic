package service;

import core.ProtocoloVlc;
import core.TramaVlc;
import io.ComunicacionSerie;

import java.io.ByteArrayOutputStream;

/**
 * Acumula bytes hasta completar una trama y emite el resultado
 * al consumidor (la UI). Aquí vive la lógica de "cuándo validar".
 */
public class RecepcionVlc implements ComunicacionSerie.ListenerRecepcion {

    public record ResultadoValidacion(
            TramaVlc tramaRecibida,
            TramaVlc tramaEsperada,
            byte crcCalculado,
            boolean estructuraValida,
            boolean huboErrorFisico) { }

    public interface ListenerValidacion {
        void onTramaValidada(ResultadoValidacion resultado);
    }

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final ListenerValidacion listener;
    private volatile TramaVlc tramaEsperada;

    public RecepcionVlc(ListenerValidacion listener) {
        this.listener = listener;
    }

    public void registrarEnvio(TramaVlc trama) {
        this.tramaEsperada = trama;
        this.buffer.reset();
    }

    @Override
    public void onBytesRecibidos(byte[] bytes) {
        buffer.write(bytes, 0, bytes.length);
        byte[] acumulado = buffer.toByteArray();
        TramaVlc esperada = tramaEsperada;
        if (esperada == null) return;

        int tamEsperado = esperada.getLongitud() + 3;
        if (acumulado.length >= tamEsperado) {
            byte[] crudos = java.util.Arrays.copyOf(acumulado, tamEsperado);
            ResultadoValidacion r = validar(TramaVlc.desdeBytesCrudos(crudos), esperada);
            listener.onTramaValidada(r);

            buffer.reset();
            if (acumulado.length > tamEsperado) {
                buffer.write(acumulado, tamEsperado, acumulado.length - tamEsperado);
            }
        }
    }

    private ResultadoValidacion validar(TramaVlc recibida, TramaVlc esperada) {
        byte crcCalc = ProtocoloVlc.calcularCRC8(recibida.calcularCuerpoParaCrc());
        boolean estructuraOk = recibida.getByteStart() == TramaVlc.DELIMITADOR_START
                && recibida.getLongitud() == esperada.getLongitud();
        boolean errorFisico = !java.util.Arrays.equals(
                recibida.getPayload(), esperada.getPayload());
        return new ResultadoValidacion(
                recibida, esperada, crcCalc, estructuraOk, errorFisico);
    }
}