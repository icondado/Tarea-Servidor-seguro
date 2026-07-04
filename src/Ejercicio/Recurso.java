package Ejercicio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Clase Recurso
 * Gestiona el acceso concurrente a las citas y a las líneas de inspección
 *
 * El ConcurrentHashMap "citas" almacena: 
 *      clave -> matrícula del vehículo 
 *      valor -> 0 (cita pendiente - esperando línea libre) | 1-4 (línea asignada)
 *
 * @author Irene Condado Alcantarilla
 */
public class Recurso {

    // Número máximo de líneas de inspección disponibles
    private static final int MAX_LINEAS = 4;

    // Mapa concurrente: matrícula → estado (0=pendiente, 1-4=línea)
    private static final ConcurrentHashMap<String, Integer> citas = new ConcurrentHashMap<>();

    // Número de líneas actualmente ocupadas
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final String FILE_USUARIOS = "usuarios.txt";
    private final String FILE_LOG = "log.txt";
    private final String CLAVE_AES = "1234567890123456"; // 16 bytes exactos para AES

    // GESTIÓN DE CITAS --------------------------------------------------------
    
    /**
     * Registra una nueva cita para la matrícula indicada 
     * Si ya existe una cita para esa matrícula, no se duplica
     *
     * @param matricula     matrícula del vehículo
     * @return      true si se ha registrado, false si ya existía
     */
    public synchronized boolean registrarCita(String matricula) {
        
        if (citas.containsKey(matricula)) {
            return false;
        }
        citas.put(matricula, 0); // estado: pendiente
        return true;
        
    }

    /**
     * Comprueba si existe una cita pendiente para la matrícula indicada
     *
     * @param matricula     matrícula del vehículo
     * @return      true si tiene cita pendiente (valor 0)
     */
    public boolean tieneCitaPendiente(String matricula) { 
        
        return citas.containsKey(matricula) && citas.get(matricula) == 0;
        
    }

    /**
     * Elimina la cita de una matrícula al terminar la inspección
     *
     * @param matricula matrícula del vehículo
     */
    public void eliminarCita(String matricula) { 
        
        citas.remove(matricula);
        
    }

    // GESTIÓN DE LÍNEAS -------------------------------------------------------
    
    /**
     * Ocupa una línea de inspección para la matrícula indicada 
     * Si todas las líneas están ocupadas, el hilo espera hasta que alguna quede libre
     *
     * @param matricula     matrícula del vehículo
     * @return      número de línea asignada (1-4)
     * @throws      InterruptedException si el hilo es interrumpido mientras espera
     */
    public synchronized int ocuparLinea(String matricula) throws InterruptedException {
        
        long currentOccupiedLines = citas.values().stream().filter(line -> line > 0).count();

        // Esperar mientras no haya líneas libres
        while (currentOccupiedLines >= MAX_LINEAS) {
            wait();            
            currentOccupiedLines = citas.values().stream().filter(line -> line > 0).count();
        }

        // Buscar la primera línea libre (1 a MAX_LINEAS)
        int lineaAsignada = -1;
        for (int i = 1; i <= MAX_LINEAS; i++) {
            if (!citas.containsValue(i)) {
                lineaAsignada = i;
                break;
            }
        }

        if (lineaAsignada == -1) {
            throw new IllegalStateException("No free line found despite condition. This indicates a logic error.");
        }

        // Asignar la línea a la matrícula
        citas.put(matricula, lineaAsignada);
        System.out.println(matricula + " entra en línea " + lineaAsignada + ".");
        return lineaAsignada;
    }

    /**
     * Libera la línea de inspección de la matrícula indicada 
     * y notifica a los hilos que estén esperando
     *
     * @param matricula matrícula del vehículo
     */
    public synchronized void liberarLinea(String matricula) {
        
        eliminarCita(matricula);
        notifyAll();
        System.out.println(matricula + " ha salido de la ITV.");
        
    }

    // GENERACIÓN DE HTML (panel de control e índice)---------------------------
    
    /**
     * Genera el HTML del panel de control 
     * Muestra las citas pendientes y el estado de cada línea de inspección
     *
     * @return  fragmento HTML con el panel
     */
    public synchronized String generarPanel() { 
        
        StringBuilder sb = new StringBuilder();

        // --- Citas (texto simple arriba del panel)
        sb.append("<div style='color:white; font-size:18px;'>")
                .append("<strong>CITAS</strong><br>")
                .append("-----------------------------<br>");

        List<String> pendingCitas = citas.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();

        if (pendingCitas.isEmpty()) {
            sb.append("<em>Sin citas pendientes</em><br>");
        } else {
            pendingCitas.forEach(matricula -> sb.append(matricula).append("<br>"));
        }

        sb.append("<br><strong>LÍNEAS DE INSPECCIÓN</strong><br>")
                .append("-----------------------------<br><br>")
                .append("</div>");

        Map<Integer, String> occupiedLines = new ConcurrentHashMap<>();
        citas.forEach((matricula, line) -> {
            if (line > 0) {
                occupiedLines.put(line, matricula);

            }
        });

        // --- Panel principal tipo LED
        for (int i = 1; i <= MAX_LINEAS; i++) {
            String matricula = occupiedLines.getOrDefault(i, "LIBRE");
            boolean libre = matricula.equals("LIBRE");
            String color = libre ? "verde" : "rojo";

            sb.append("""
                <div class="linea">
                    <div class="%s">%s</div>
                    <div class="%s">%d</div>
                </div>
            """.formatted(color, matricula, "linea-num", i));
        }

        return sb.toString();
    }

    // GESTIÓN SEGURA DE USUARIOS ----------------------------------------------
    /**
     * Registra un nuevo usuario con email y contraseña
     * Valida el formato del email y los requisitos de la contraseña,
     * aplica hash BCrypt y cifra el fichero de usuarios con AES
     * 
     * @param email     correo electrónico del usuario
     * @param pass      contraseña en texto plano
     * @return          mensaje de resultado del registro
     * @throws Exception    Excepción si la contraseña no cumple los requisitos
     */
    public String registrarUsuario(String email, String pass) throws Exception {
        
        // Validar Regex
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            return "Email inválido.";
        }
        if (!pass.matches("^(?=.*[a-zA-Z])(?=.*\\d).{6,}$")) {
            escribirLog("Contraseña no cumple requisitos: " + pass);
            throw new Exception("Contraseña no válida: debe ser alfanumérica y de al menos 6 caracteres.");
        }

        lock.writeLock().lock();
        try {
            List<String> usuarios = leerYDescifrarUsuarios();
            for (String u : usuarios) {
                if (u.startsWith(email + ":")) {
                    return "El usuario ya existe.";
                }
            }

            // Hash con BCrypt
            String hash = BCrypt.hashpw(pass, BCrypt.gensalt());
            usuarios.add(email + ":" + hash);

            // Cifrar y Guardar
            guardarYCifrarUsuarios(usuarios);
            return "Registro correcto.";
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Validar las credenciales de un usuario
     * 
     * @param email correo electrónico del usuario
     * @param pass  contraseña en texto plano
     * @return      true si las credenciales son correctas, false en caso contrario
     */
    public boolean validarLogin(String email, String pass) {
        
        lock.readLock().lock();
        try {
            List<String> usuarios = leerYDescifrarUsuarios();
            for (String linea : usuarios) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] partes = linea.split(":", 2);
                if (partes[0].equals(email)) {
                    if (BCrypt.checkpw(pass, partes[1])) {
                        return true;
                    } else {
                        escribirLog("Contraseña no cumple requisitos: " + pass); // contraseña incorrecta
                        return false;
                    }
                }
            }
            escribirLog("Login incorrecto: " + email); // email no existe
            return false;
        } catch (Exception e) {
            escribirLog("Login incorrecto: " + email); // error inesperado
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    // CIFRADO AES -------------------------------------------------------------
    
    /**
     * Lee el fichero de usuariosm lo descifra con AES y devuelve las líneas
     * 
     * @return      lista de líneas con formato "email:hashBCrypt"
     * @throws Exception    si ocurre un error de cifrado o lectura
     */
    private List<String> leerYDescifrarUsuarios() throws Exception {
        
        File file = new File(FILE_USUARIOS);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        byte[] encoded = java.nio.file.Files.readAllBytes(file.toPath()); 
        if (encoded.length == 0) {
            return new ArrayList<>();
        }

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(CLAVE_AES.getBytes(StandardCharsets.UTF_8), "AES")); 
        String descifrado = new String(cipher.doFinal(encoded), StandardCharsets.UTF_8); 

        return Arrays.stream(descifrado.split("\\r?\\n"))
                .filter(l -> !l.isBlank())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /**
     * Cifra la lista de usuarios con AES y la guarda en el fichero
     * 
     * @param usuarios      lista de líneas con formato "email:hashCrypt"
     * @throws Exception    si ocurre un error de cifrado o escritura
     */
    private void guardarYCifrarUsuarios(List<String> usuarios) throws Exception {
        
        String contenido = String.join("\n", usuarios);
        System.out.println("Guardando usuarios: " + contenido);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(CLAVE_AES.getBytes(StandardCharsets.UTF_8), "AES"));
        byte[] cifrado = cipher.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        java.nio.file.Files.write(new File(FILE_USUARIOS).toPath(), cifrado);
        System.out.println("usuarios.txt escrito, bytes: " + cifrado.length);
    }

    // LOG ---------------------------------------------------------------------
    
    /**
     * Escribe una entrada en el fichero de log con marca de tiempo
     * 
     * @param mensaje  texto a registrar
     */
    public synchronized void escribirLog(String mensaje) {
        
        // Usar 'true' en FileWriter para añadir líneas sin borrar las anteriores
        try (FileWriter fw = new FileWriter(FILE_LOG, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {

            out.println(LocalDateTime.now() + " - " + mensaje);
            System.out.println("Log escrito: " + mensaje);
        } catch (IOException e) {
            System.err.println("Error al escribir en log.txt: " + e.getMessage());
        }
    }    
}
