package Ejercicio;

/**
 * Clase PaginasHTML
 * Contiene los métodos estáticos que generan el HTML de cada página del servidor
 * 
 * @author
 */
public class PaginasHTML {

    /**
     * Genera la página principal (panel de control)
     * Se refresca automáticamente cada 2 segundos para reflejar el estado en tiempo real
     *
     * @param panelHTML fragmento HTML generado por Recurso.generarPanel()
     * @return HTML completo de la página de inicio
     */
    public static String htmlIndex(String panelHTML) { 
        
        return "<html><head>"
                + "<title>ITV del Infierno</title>"
                + "<meta charset='UTF-8'>"
                + "<meta http-equiv='refresh' content='2'>" 
                + "<style>"
                + "body {"
                + "  font-family: Arial, sans-serif;"
                + "  background: linear-gradient(135deg, #000000, #440000);"
                + "  color: white;"
                + "  text-align: center;"
                + "  padding-bottom: 40px;"
                + "}"
                + ".boton {"
                + "  background: #ff3333;"
                + "  color: white;"
                + "  padding: 20px 40px;"
                + "  font-size: 22px;"
                + "  margin: 20px;"
                + "  border: none;"
                + "  border-radius: 10px;"
                + "  cursor: pointer;"
                + "  transition: 0.3s;"
                + "}"
                + ".boton:hover { background:#cc0000; transform: scale(1.05); }"
                + ".panel {"
                + "  background: black;"
                + "  width: 60%;"
                + "  margin: auto;"
                + "  padding: 25px;"
                + "  border: 8px solid #333;"
                + "  color: #00ff00;"
                + "  font-family: 'Courier New', monospace;"
                + "  box-shadow: 0 0 15px #ff000055;"
                + "}"
                + ".panel-titulo {"
                + "  text-align: center;"
                + "  font-size: 30px;"
                + "  font-weight: bold;"
                + "  color: #ff3333;"
                + "  border-bottom: 4px solid #ff3333;"
                + "  padding-bottom: 12px;"
                + "  margin-bottom: 20px;"
                + "  letter-spacing: 3px;"
                + "}"
                + ".encabezado, .linea {"
                + "  display: grid;"
                + "  grid-template-columns: 70% 30%;"
                + "  font-size: 22px;"
                + "  letter-spacing: 2px;"
                + "  padding: 6px 0;"
                + "}"
                + ".linea { font-size: 24px; }" 
                + ".verde { color: #00ff00; }" 
                + ".rojo { color: #ff3333; }" 
                + "</style>"
                + "</head><body>"
                + "<h1 style='color:#ff5555;'>ITV del Infierno</h1>"
                + "<form action='/reservar' method='GET'>"
                + "<button class='boton'>Reservar Cita</button>"
                + "</form>"
                + "<form action='/pasar' method='GET'>"
                + "<button class='boton'>Pasar ITV</button>"
                + "</form>"
                + "<h2 style='color:#ff8888;'>Panel de Llamadas</h2>"
                + "<div class='panel'>"
                + "<div class='panel-titulo'>LÍNEAS DE INSPECCIÓN</div>"
                + panelHTML
                + "</div></body></html>";
    }

    /**
     * Genera la página del formulario de reserva de cita
     *
     * @param mensaje mensaje informativo a mostrar (vacío si no hay ninguno)
     * @return HTML completo de la página de reserva 
     */
    public static String htmlReservar(String mensaje) {
        
        String bloqueMsg = (mensaje != null && !mensaje.isEmpty())
                ? "<p style='color:#ffcc00; font-size:18px;'>" + mensaje + "</p>" : "";
        return "<html><head><meta charset='UTF-8'><title>Reservar Cita</title>"
                + "<style>"
                + "body { background: linear-gradient(135deg, #550000, #220000); "
                + "       color:white; font-family:Arial; text-align:center; padding-top:40px; }"
                + "input { padding:12px; font-size:18px; border-radius:8px; border:none; width:250px; }"
                + "button { background: #ff4444; color:white; padding:12px 25px; "
                + "        border:none; border-radius:8px; font-size:18px; cursor:pointer; margin-left:10px; }"
                + "button:hover { background: #cc0000; }"
                + "a { color:#ffaaaa; font-size:20px; text-decoration:none; display:block; margin-top:20px; }"
                + "</style></head>"
                + "<body>"
                + "<h1>Reservar Cita ITV</h1>"
                + bloqueMsg
                + "<form action='/reservar' method='POST'>"
                + "  <input type='text' name='matricula' placeholder='Matrícula (ej: 1234ABC)' required>"
                + "  <button type='submit'>Confirmar</button>"
                + "</form>"
                + "<a href='/inicio'>← Volver al inicio</a>"
                + "</body></html>";
    }

    /**
     * Genera la página del formulario para pasar la ITV,
     * incluyendo el resultado de la inspección si ya se ha realizado
     *
     * @param matricula matrícula del vehículo (vacío si aún no se ha enviado)
     * @param linea     número de línea asignada (-1 si no aplica)
     * @param mensaje   resultado de la inspección en HTML (vacío si no hay ninguno)
     * @return HTML completo de la página de inspección
     */
    public static String htmlPasar(String matricula, int linea, String mensaje) {
        
        String valor = (matricula != null) ? matricula : "";
        String bloqueMsg = (mensaje != null && !mensaje.isEmpty())
                ? String.format("<div style='background:#330000; padding:20px; border-radius:10px; margin-top:20px; white-space:pre-line; border:1px solid #ff3333;'>"
                        + "<h2 style='color:#ff3333;'>Resultado para %s (Línea %d)</h2>%s</div>", valor, linea, mensaje)
                : "";
        return "<html><head><meta charset='UTF-8'><title>Pasar ITV</title>"
                + "<style>"
                + "body { background: linear-gradient(135deg, #000000, #440000); color:white; font-family:Arial; text-align:center; padding:40px; }"
                + "h1 { color:#ff3333; }"
                + "input { padding:15px; font-size:20px; border-radius:10px; border:2px solid #ff3333; background:#1a1a1a; color:white; width:280px; text-align:center; }"
                + "button { background:#ff3333; color:white; padding:15px 35px; border:none; border-radius:10px; font-size:20px; cursor:pointer; margin-top:10px; }"
                + "button:hover { background:#cc0000; transform:scale(1.05); }"
                + "a { color:#ffaaaa; text-decoration:none; display:block; margin-top:30px; font-size:18px; }"
                + "</style></head><body>"
                + "<h1>Entrada a Inspección</h1>"
                + "<form action='/pasar' method='POST'>"
                + "  <input type='text' name='matricula' placeholder='Matrícula' required value='" + valor + "'><br>"
                + "  <button type='submit'>Entrar a línea</button>"
                + "</form>"
                + bloqueMsg
                + "<a href='/inicio'>← Volver al inicio</a>"
                + "</body></html>";
    }

    /**
     * Genera la página de error 404
     *
     * @return HTML completo de la página de error
     */
    public static String htmlError() {
        return "<html><head><meta charset='UTF-8'><title>404 Not Found</title>"
                + "<style>"
                + "body { background:#1a1a1a; color:white; font-family:Arial; text-align:center; padding-top:100px; }"
                + "h1 { color:#ff3333; font-size:60px; }"
                + "a { color:#ff3333; font-size:20px; }"
                + "</style></head><body>"
                + "<h1>404</h1><p>Página no encontrada.</p>"
                + "<a href='/inicio'>Volver al inicio</a>"
                + "</body></html>";
    }

    /**
     * Genera la página de login y registro de usuarios
     *
     * @param msg mensaje de error o confirmación a mostrar (vacío si no hay ninguno)
     * @return HTML completo de la página de acceso
     */
    public static String login(String msg) {
        return "<!DOCTYPE html>"
                + "<html lang='es'>"
                + "<head>"
                + "<link rel=icon href=data:,/>"
                + "<meta charset='UTF-8'>"
                + "<title>Login</title>"
                + "<style>"
                + "body {"
                + "  font-family: Arial, sans-serif;"
                + "  background: linear-gradient(135deg,#550000,#220000); "
                + "  display: flex;"
                + "  justify-content: center;"
                + "  align-items: center;"
                + "  height: 100vh;"
                + "  margin: 0;"
                + "}"
                + ".container {"
                + "  background: white;"
                + "  padding: 40px;"
                + "  border-radius: 15px;"
                + "  box-shadow: 0 8px 16px rgba(0,0,0,0.2);"
                + "  width: 350px;"
                + "}"
                + "h2 {"
                + "  text-align: center;"
                + "}"
                + "input {"
                + "  width: 100%;"
                + "  padding: 10px;"
                + "  margin: 10px 0;"
                + "  border-radius: 8px;"
                + "  border: 1px solid #ccc;"
                + "}"
                + "button {"
                + "  width: 100%;"
                + "  padding: 12px;"
                + "  background-color: #ff3333;"
                + "  color: white;"
                + "  border: none;"
                + "  border-radius: 8px;"
                + "  cursor: pointer;"
                + "  font-size: 16px;"
                + "}"
                + "button:hover {"
                + "  background-color: #cc0000;"
                + "}"
                + ".msg {"
                + "  color: red;"
                + "  text-align: center;"
                + "}"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + (msg != null && !msg.isEmpty() ? "<div class='msg'>" + msg + "</div>" : "")
                + "<h2>Iniciar sesión</h2>"
                + "<form action='/login' method='post'>"
                + "<input name='email' placeholder='Correo electrónico' required>"
                + "<input name='password' type='password' placeholder='Contraseña' required>"
                + "<button>Entrar</button>"
                + "</form>"
                + "<h2>Registro</h2>"
                + "<form action='/registro' method='post'>"
                + "<input name='email' placeholder='Correo electrónico' required>"
                + "<input name='password' type='password' placeholder='Contraseña' pattern='(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}'"
                + " title='Mí­nimo 6 caracteres, letras y números' minlength='6' required>"
                + "<button>Registrarse</button>"
                + "</form>"
                + "</div></body></html>";
    }
}
