package Ejercicio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Clase HiloServidor Gestiona una petición HTTPS en un hilo independiente
 * Lee la petición del navegador, procesa la ruta y devuelve la respuesta HTML
 *
 * Rutas disponibles: GET / → Login POST /login → Procesa el inicio de sesión
 * POST /registro → Procesa el registro de usuario GET /inicio → Página
 * principal: panel de control y botones GET /reservar → Formulario de reserva
 * de cita POST /reservar → Procesa la reserva de cita GET /pasar → Formulario
 * para pasar la ITV POST /pasar → Ejecuta la inspección y muestra el resultado
 * Cualquier otra → Página de error 404
 *
 * @author Irene Condado Alcantarilla
 */
public class HiloServidor implements Runnable {

    // Socket del cliente (navegador)
    private final Socket s;

    // Recurso compartido (citas y líneas de inspección)
    private final Recurso rc;

    // Pruebas que se realizan en la ITV
    private static final String[] PRUEBAS = {"Luces", "Frenos", "Emisiones", "Dirección", "Suspensión"};

    // Frases normales que simula responder el vehículo
    private static final String[] FRASES_PERMITIDAS = {
        "vale", "recibido", "entendido", "procedo", "hecho", "si", "correcto", "ok", "de acuerdo"
    };

    // Frases de tipo "cuñao" que penalizan la probalidad de pasar cada prueba
    private static final String[] FRASES_CUNAO = {
        "ok jefe", "lo que tu digas", "a mandar", "como usted mande",
        "vamos al lio", "marchando", "manda usted", "perfecto maquina", "de lujo"
    };

    /**
     * Constructor HiloServidor
     *
     * @param s socket del cliente (navegador)
     * @param rc recurso compartido (citas y líneas de inspección)
     */
    public HiloServidor(Socket s, Recurso rc) {
        this.s = s;
        this.rc = rc;
    }

    /**
     * Método principal del hilo
     * Lee la petición HTTPS, determina la ruta y delega en el método adecuado
     */
    @Override
    public void run() {

        try (
                // Crear flujos de entrada y salida
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); PrintWriter pw = new PrintWriter(s.getOutputStream(), true)) {

            // Leer la primera línea de la petición HTTP (Ej: "GET /reservar HTTP/1.1")
            String primeraLinea = br.readLine();
            if (primeraLinea == null || primeraLinea.isEmpty()) {
                return;
            }

            String[] partes = primeraLinea.split(" ");
            if (partes.length < 2) {
                return;
            }

            String metodo = partes[0]; // GET o POST
            String rutaOriginal = partes[1];
            String ruta = rutaOriginal.split("\\?")[0]; // Ignorar parámetros 

            // Leer cabeceras para obtener Content-Length (peticiones POST)
            int contentLength = 0;
            String linea;
            while ((linea = br.readLine()) != null && !linea.isEmpty()) {
                if (linea.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(linea.split(":")[1].trim());
                }
            }

            // Leer el cuerpo en peticiones POST
            String cuerpo = "";
            if ("POST".equalsIgnoreCase(metodo) && contentLength > 0) {
                char[] buffer = new char[contentLength];
                br.read(buffer, 0, contentLength);
                cuerpo = new String(buffer);
            }

            // Enrutar la petición según método y ruta
            switch (metodo.toUpperCase() + " " + ruta) {
                case "GET /":
                    inicioSesion(pw);
                    break;
                case "POST /login":
                    // Extraemos los parámetros del cuerpo del POST
                    String emailL = extraerParametro(cuerpo, "email");
                    String passL = extraerParametro(cuerpo, "password");
                    if (rc.validarLogin(emailL, passL)) {
                        enviarRedireccion(pw, "/inicio");
                    } else {
                        enviarRespuesta(pw, "200 OK", PaginasHTML.login("<p style='color:red;'>Login incorrecto.</p>"));
                    }
                    break;

                case "POST /registro":
                    // Extraemos los parámetros del cuerpo del POST
                    String emailR = extraerParametro(cuerpo, "email");
                    String passR = extraerParametro(cuerpo, "password");
                    try {
                        String resultado = rc.registrarUsuario(emailR, passR);
                        enviarRespuesta(pw, "200 OK", PaginasHTML.login("<p style='color:green;'>" + resultado + "</p>"));
                    } catch (Exception e) {
                        enviarRespuesta(pw, "200 OK", PaginasHTML.login("<p style='color:red;'>" + e.getMessage() + "</p>"));
                    }
                    break;
                case "GET /inicio":
                    controlInicio(pw);
                    break;
                case "GET /reservar":
                    controlReservarGet(pw, "");
                    break;
                case "POST /reservar":
                    controlReservarPost(pw, cuerpo);
                    break;
                case "GET /pasar":
                    controlPasarGet(pw, "");
                    break;
                case "POST /pasar":
                    controlPasarPost(pw, cuerpo);
                    break;
                default:
                    controlError404(pw);
                    break;
            }

            // Cierre de recursos, lo hace automático pero aún así...
            br.close();
            pw.close();
            s.close();

        } catch (IOException e) {
            System.out.println("Error de comunicación: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Inspección interrumpida: " + e.getMessage());
        }
    }

    // CONTROLES DE RUTAS ------------------------------------------------------
    
    /**
     * Ruta: GET / 
     * Muestra la página de login y registro
     */
    private void inicioSesion(PrintWriter pw) {
        String html = PaginasHTML.login("");
        enviarRespuesta(pw, "200 OK", html);
    }

    /**
     * Ruta: GET /inicio 
     * Muestra la página principal con el panel de control y
     * los botones de acción
     */
    private void controlInicio(PrintWriter pw) {
        String panel = rc.generarPanel();
        String html = PaginasHTML.htmlIndex(panel);
        enviarRespuesta(pw, "200 OK", html);
    }

    /**
     * Ruta: GET /reserva 
     * Muestra el formulario de reserva de cita
     *
     * @param mensaje   mensaje opcional a mostrar (vacío si no hay ningúno)
     */
    private void controlReservarGet(PrintWriter pw, String mensaje) {
        enviarRespuesta(pw, "200 OK", PaginasHTML.htmlReservar(mensaje));
    }

    /**
     * Ruta: POST /reservar 
     * Procesa el formulario de reserva: extrae la
     * matrícula y registra la cita
     */
    private void controlReservarPost(PrintWriter pw, String cuerpo) {
        String matricula = extraerParametro(cuerpo, "matricula").toUpperCase().trim();

        if (matricula.isEmpty()) {
            controlReservarGet(pw, "&#10060; La matrícula no puede estar vacía.");
            return;
        }

        boolean registrada = rc.registrarCita(matricula);
        if (registrada) {
            System.out.println("Cita registrada para: " + matricula);
            // Redirige a la página de inicio para ver la cita en el panel
            enviarRedireccion(pw, "/inicio");
        } else {
            controlReservarGet(pw, "&#9888;&#65039; Ya existe una cita para la matrícula: " + matricula);
        }
    }

    /**
     * Ruta: GET /pasar 
     * Muestra el formulario para entrar a inspección
     *
     * @param mensaje   mensaje opcional a mostrar (vacío si no hay ningúno)
     */
    private void controlPasarGet(PrintWriter pw, String mensaje) {
        enviarRespuesta(pw, "200 OK", PaginasHTML.htmlPasar("", -1, mensaje));
    }

    /**
     * Ruta: POST /pasar 
     * Ejecuta la inspección del vehículo y muestra el resultado
     * El hilo espera sí todas las líneas de inspección están ocupadas
     */
    private void controlPasarPost(PrintWriter pw, String cuerpo) throws InterruptedException {
        String matricula = extraerParametro(cuerpo, "matricula").toUpperCase().trim();

        if (matricula.isEmpty()) {
            controlPasarGet(pw, "&#10060; La matrícula no puede estar vacía.");
            return;
        }

        // Verificar que tiene cita pendiente
        if (!rc.tieneCitaPendiente(matricula)) {
            controlPasarGet(pw, "&#10060; No existe cita pendiente para la matrícula: " + matricula
                    + "\nReserva una cita antes de pasar la ITV.");
            return;
        }

        // Ocupar una línea (espera si todas están ocupadas)
        int linea = rc.ocuparLinea(matricula);

        // Simulación de la inspección
        double probabilidad = 0.6;
        int pruebasSuperadas = 0;
        String[] respuestas = new String[PRUEBAS.length];
        boolean[] resultados = new boolean[PRUEBAS.length];
        double[] probabilidades = new double[PRUEBAS.length];

        // Realizar todas las pruebas
        for (int i = 0; i < PRUEBAS.length; i++) {
            // Guardar la probabilidad actual para esta prueba
            probabilidades[i] = probabilidad;

            // Tiempo de inspección entre 1 y 10 segundos
            int tiempo = (int) (Math.random() * 9000) + 1000;
            Thread.sleep(tiempo);

            // Simular respuesta del coche
            String respuesta = elegirFraseAleatoria();
            respuestas[i] = respuesta;

            // Comprobar si pasa la prueba
            double aleatorio = Math.random();
            if (aleatorio < probabilidad) {
                pruebasSuperadas++;
                resultados[i] = true;
            } else {
                resultados[i] = false;
            }

            // Si es frase cuñao, reducir probabilidad para las siguientes
            if (esFraseCunao(respuesta)) {
                probabilidad -= 0.1;
            }
        }

        // Resultado final
        boolean aprobado = (pruebasSuperadas == PRUEBAS.length);

        // Liberar la línea
        rc.liberarLinea(matricula);

        // Mostrar resultado en consola
        System.out.println("\n--- Resultado " + matricula + " (Línea " + linea + ") ---");
        System.out.println(matricula + (aprobado ? " ITV SUPERADA." : " ITV NO SUPERADA."));

        // Mostrar detalle de cada prueba con la probabilidad que tenia en ese momento
        for (int i = 0; i < PRUEBAS.length; i++) {
            String res = resultados[i] ? "✔ Sí" : "✘ No";
            int prob = (int) (probabilidades[i] * 100);  // Usa la probabilidad guardada
            System.out.println("  " + PRUEBAS[i] + ": " + res
                    + " (\"" + respuestas[i] + "\" - prob " + prob + "%)");
        }

        System.out.println("------------------------------------------\n");

        // Construir mensaje de resultado para el HTML
        StringBuilder resultado = new StringBuilder();
        resultado.append(aprobado
                ? "&#9989; ITV SUPERADA — Tome su pegatina\n\n"
                : "&#10060; ITV NO SUPERADA — Debe volver de nuevo\n\n");

        for (int i = 0; i < PRUEBAS.length; i++) {
            String icono = resultados[i] ? "✔" : "✘";
            int prob = (int) (probabilidades[i] * 100);
            resultado.append(icono).append(" ").append(PRUEBAS[i])
                    .append(": \"").append(respuestas[i]).append("\"")
                    .append(" (prob ").append(prob).append("%)\n");
        }

        enviarRespuesta(pw, "200 OK", PaginasHTML.htmlPasar(matricula, linea, resultado.toString()));
    }

    /**
     * Ruta desconocida -> Página de error 404
     */
    private void controlError404(PrintWriter pw) {
        String html = PaginasHTML.htmlError();
        enviarRespuesta(pw, "404 Not Found", html);
    }

    // LÓGICA DE INSPECCIÓN ----------------------------------------------------
    
    /**
     * Devuelve una frase aleatoria simulando la respuesta del vehículo: 
     * 70% frase normal 30% frase cuñao
     */
    private String elegirFraseAleatoria() {
        if (Math.random() < 0.7) {
            return FRASES_PERMITIDAS[(int) (Math.random() * FRASES_PERMITIDAS.length)];
        } else {
            return FRASES_CUNAO[(int) (Math.random() * FRASES_CUNAO.length)];
        }
    }

    /**
     * Comprueba si la respuesta dada es una frase cuñao
     *
     * @param respuesta     frase a comprobar
     * @return      true si es frase cuñao, false en caso comtrario
     */
    private boolean esFraseCunao(String respuesta) {
        if (respuesta == null) {
            return false;
        }
        String norm = respuesta.toLowerCase().trim();
        for (String frase : FRASES_CUNAO) {
            if (norm.equals(frase)) {
                return true;
            }
        }
        return false;
    }

    // UTILIDADES HTTP ---------------------------------------------------------
    
    /**
     * Envía una respuesta HTTP completa con cabeceras y cuerpo HTML
     *
     * @param pw        flujo de salida del cliente
     * @param status    código y texto de estado HTTP(ej: "200 OK")
     * @param html      contenido HTML de la respuesta
     */
    private void enviarRespuesta(PrintWriter pw, String status, String html) {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        pw.print("HTTP/1.1 " + status + "\r\n");
        pw.print("Content-Type: text/html; charset=UTF-8\r\n");
        pw.print("Content-Length: " + bytes.length + "\r\n");
        pw.print("Connection: close\r\n");
        pw.print("\r\n");
        pw.print(html);
        pw.flush();
    }

    /**
     * Envía una redirección HTTP 302 a la ruta indicada
     *
     * @param pw    flujo de salida del cliente
     * @param ruta  ruta de destino de la redirección
     */
    private void enviarRedireccion(PrintWriter pw, String ruta) {
        pw.print("HTTP/1.1 302 Found\r\n");
        pw.print("Location: " + ruta + "\r\n");
        pw.print("Connection: close\r\n");
        pw.print("\r\n");
        pw.flush();
    }

    /**
     * Extrae el valor de un parámetro del cuerpo de una petición POST
     * El cuerpo tiene formato: param1=valor1&param2=valor2
     *
     * @param cuerpo        cuerpo de la petición POST
     * @param parametro     nombre del parámetro a buscar
     * @return              valor decodificado del parámetro, o "" si no existe
     */
    private String extraerParametro(String cuerpo, String parametro) {
        if (cuerpo == null || cuerpo.isEmpty()) {
            return "";
        }
        String[] pares = cuerpo.split("&");
        for (String par : pares) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2 && kv[0].equals(parametro)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}
