package io;

import com.fazecast.jSerialComm.SerialPort;

/**
 * Capa de transporte serie. Solo abre/cierra/envi/e lee.
 * No conoce el protocolo ni la UI (SRP).
 */
public class ComunicacionSerie {

    public interface ListenerRecepcion {
        void onBytesRecibidos(byte[] bytes);
    }

    private static final int BAUDIOS = 9600;
    private static final int SLEEP_MS = 40;

    private SerialPort puerto;
    private final ListenerRecepcion listener;
    private volatile boolean escuchando;
    private Thread hiloEscucha;

    public ComunicacionSerie(ListenerRecepcion listener) {
        this.listener = listener;
    }

    public ResultadoConexion conectar() {
        SerialPort[] puertos = SerialPort.getCommPorts();
        if (puertos.length == 0) {
            return ResultadoConexion.fallo(
                "Error: No se encontró la interfaz de comunicación UART del microcontrolador.");
        }
        SerialPort candidato = puertos[0];
        candidato.setBaudRate(BAUDIOS);
        if (!candidato.openPort()) {
            return ResultadoConexion.fallo("Fallo de acceso exclusivo al puerto serie COM.");
        }
        this.puerto = candidato;
        return ResultadoConexion.exito("Canal serie inicializado en "
                + candidato.getSystemPortName() + " (9600 bps, 8N1).");
    }

    public boolean estaConectada() {
        return puerto != null && puerto.isOpen();
    }

    public String getNombrePuerto() {
        return puerto == null ? "Sin puerto" : puerto.getSystemPortName();
    }

    public boolean enviar(byte[] trama) {
        return estaConectada() && puerto.writeBytes(trama, trama.length) == trama.length;
    }

    public void iniciarEscucha() {
        if (!estaConectada() || escuchando) return;
        escuchando = true;
        hiloEscucha = new Thread(this::bucleEscucha, "vlc-escucha-serie");
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }

    private void bucleEscucha() {
        while (escuchando && estaConectada()) {
            int disponibles = puerto.bytesAvailable();
            if (disponibles > 0) {
                byte[] buffer = new byte[disponibles];
                int leidos = puerto.readBytes(buffer, buffer.length);
                if (leidos > 0) {
                    listener.onBytesRecibidos(java.util.Arrays.copyOf(buffer, leidos));
                }
            }
            dormir(SLEEP_MS);
        }
    }

    private void dormir(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            escuchando = false;
        }
    }

    public void cerrar() {
        escuchando = false;
        if (puerto != null && puerto.isOpen()) {
            puerto.closePort();
        }
    }

    /** Resultado de conexión tipado (evita usar String como estado). */
    public record ResultadoConexion(boolean exito, String mensaje) {
        public static ResultadoConexion exito(String m)   { return new ResultadoConexion(true, m); }
        public static ResultadoConexion fallo(String m)   { return new ResultadoConexion(false, m); }
    }
}