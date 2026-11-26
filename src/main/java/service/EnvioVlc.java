package service;

import core.HammingCodec;
import core.ModoEnlace;
import core.TramaVlc;
import io.ComunicacionSerie;
import java.nio.charset.StandardCharsets;

/** Construye la trama desde el texto y la envía por el puerto serie. */
public class EnvioVlc {

    private final ComunicacionSerie comunicacion;
    private final RecepcionVlc recepcion;

    public EnvioVlc(ComunicacionSerie comunicacion, RecepcionVlc recepcion) {
        this.comunicacion = comunicacion;
        this.recepcion = recepcion;
    }

    /** @return la trama enviada, o null si el texto es inválido o no hay conexión. */
    public TramaVlc enviarTexto(String texto, ModoEnlace modo) {
        if (!comunicacion.estaConectada() || texto == null || texto.isBlank()) {
            return null;
        }
        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        int longitud = Math.min(datos.length, TramaVlc.MAX_PAYLOAD);

        byte[] payload = new byte[longitud];
        System.arraycopy(datos, 0, payload, 0, longitud);

        TramaVlc trama = TramaVlc.desdePayload(payload);
        byte[] aTransmitir = (modo == ModoEnlace.CRC_HAMMING)
                ? HammingCodec.codificarTrama(trama.aBytesTransmision())
                : trama.aBytesTransmision();

        recepcion.registrarEnvio(trama, modo);
        comunicacion.enviar(aTransmitir);
        return trama;
    }
}
