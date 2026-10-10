package com.mycompany.tfi_comunic;

import com.fazecast.jSerialComm.SerialPort;
import java.util.function.Consumer;

//Gestiona la conexión, el envío y la escucha del puerto serie
public class ComunicacionSerie {
    private SerialPort puerto;
    private final Consumer<byte[]> receptor;
    private volatile boolean escuchando;
    private Thread hiloEscucha;

    public ComunicacionSerie(Consumer<byte[]> receptor) {
        this.receptor = receptor;
    }

    //Intenta abrir el primer puerto serie disponible. Devuelve un mensaje de estado 
    public String conectar() {
        SerialPort[] puertos = SerialPort.getCommPorts();
        if (puertos.length == 0) {
            return "Error: No se encontró la interfaz de comunicación UART del microcontrolador.";
        }

        puerto = puertos[0];
        puerto.setBaudRate(9600);

        if (puerto.openPort()) {
            return "Canal serie inicializado en " + getNombrePuerto()
                    + " (9600 bps, 8N1). Esperando comandos...";
        }

        puerto = null;
        return "Fallo de acceso exclusivo al puerto serie COM.";
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

    //Inicia un hilo sencillo que entrega los bytes recibidos al consumidor
    public void iniciarEscucha() {
        if (!estaConectada() || escuchando) return;
        escuchando = true;
        hiloEscucha = new Thread(() -> {
            while (escuchando && estaConectada()) {
                int disponibles = puerto.bytesAvailable();
                if (disponibles > 0) {
                    byte[] buffer = new byte[disponibles];
                    int leidos = puerto.readBytes(buffer, buffer.length);
                    if (leidos > 0) {
                        byte[] recibidos = new byte[leidos];
                        System.arraycopy(buffer, 0, recibidos, 0, leidos);
                        receptor.accept(recibidos);
                    }
                }
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "vlc-escucha-serie");
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }

    public void cerrar() {
        escuchando = false;
        if (puerto != null && puerto.isOpen()) {
            puerto.closePort();
        }
    }
}