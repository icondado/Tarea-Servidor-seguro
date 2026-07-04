package Ejercicio;

import java.io.IOException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * Clase Servidor
 * Representa la estación de ITV como servidor HTTPS seguro
 * Acepta conexiones HTTPS desde navegadores y crea un hilo
 * independiente para gestionar cada petición
 *
 * @author Irene Condado Alcantarilla
 */
public class Servidor {

    // Puerto en el que escucha el servidor HTTPS
    private static final int PUERTO = 8443;

    public static void main(String[] args) throws IOException {

        try {

            // Recurso compartido que gestiona citas y líneas de inspección
            Recurso rc = new Recurso();

            // Configuración del almacén de claves SSL
            System.setProperty("javax.net.ssl.keyStore", "AlmacenSSL");
            System.setProperty("javax.net.ssl.keyStorePassword", "123456");

            // Crear el servidor HTTPS seguro en el puerto indicado
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(PUERTO);
            System.out.println("Servidor HTTPS (SEGURO) ITV del Infierno arrancando en https://localhost:" + PUERTO + "...");

            // El servidor permanece activo esperando conexiones
            while (true) {
                // Espera la conexión de un cliente HTTPS
                SSLSocket s = (SSLSocket) ss.accept();

                // Se crea un hilo independiente para atender cada cliente
                new Thread(new HiloServidor(s, rc)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
