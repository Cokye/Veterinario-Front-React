package cl.curso.usuarios.crypto;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Retiene la respuesta en memoria en vez de mandarla al navegador.
 *
 * El controlador (o Spring Security, o el manejo de errores) escribe aqui
 * creyendo que escribe en la red. Cuando la cadena de filtros termina,
 * CifradoFiltro toma estos bytes, los cifra, y recien ahi los envia.
 *
 * Tambien interceptamos:
 *   - setContentType / setContentLength: el tipo real (application/json,
 *     text/plain...) se guarda aqui y viaja como cabecera X-Enc-Content-Type,
 *     porque hacia afuera todo sale como application/octet-stream.
 *   - sendError: si dejaramos que el contenedor genere su pagina de error, esa
 *     pagina saldria SIN cifrar despues de nuestro filtro. En su lugar
 *     escribimos nosotros un JSON de error dentro del buffer.
 *   - flushBuffer: lo anulamos para que nada se envie antes de tiempo.
 */
public class RespuestaEnBuffer extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private ServletOutputStream flujo;
    private PrintWriter escritor;
    private String tipoContenido;
    private boolean errorEnviado;

    public RespuestaEnBuffer(HttpServletResponse original) {
        super(original);
    }

    /** Los bytes que la aplicacion quiso enviar, todavia en claro. */
    public byte[] getCuerpo() {
        if (escritor != null) {
            escritor.flush();
        }
        return buffer.toByteArray();
    }

    /** El Content-Type original (el que hay que restaurar tras descifrar). */
    public String getTipoContenidoOriginal() {
        return tipoContenido;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (flujo == null) {
            flujo = new ServletOutputStream() {
                @Override
                public void write(int b) {
                    buffer.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    // No usamos escritura asincrona en este proyecto.
                }
            };
        }
        return flujo;
    }

    @Override
    public PrintWriter getWriter() {
        if (escritor == null) {
            escritor = new PrintWriter(
                    new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8), true);
        }
        return escritor;
    }

    @Override
    public void setContentType(String tipo) {
        this.tipoContenido = tipo;
        // A proposito NO se lo pasamos a la respuesta real: el tipo de salida
        // lo decide el filtro (application/octet-stream).
    }

    @Override
    public String getContentType() {
        return tipoContenido;
    }

    // El largo real cambia al cifrar, asi que ignoramos el que fije la app.
    @Override
    public void setContentLength(int largo) {
        // intencionalmente vacio
    }

    @Override
    public void setContentLengthLong(long largo) {
        // intencionalmente vacio
    }

    @Override
    public void setHeader(String nombre, String valor) {
        if (esCabeceraDeCuerpo(nombre)) {
            if ("content-type".equalsIgnoreCase(nombre)) {
                this.tipoContenido = valor;
            }
            return;
        }
        super.setHeader(nombre, valor);
    }

    @Override
    public void addHeader(String nombre, String valor) {
        if (esCabeceraDeCuerpo(nombre)) {
            if ("content-type".equalsIgnoreCase(nombre)) {
                this.tipoContenido = valor;
            }
            return;
        }
        super.addHeader(nombre, valor);
    }

    private boolean esCabeceraDeCuerpo(String nombre) {
        return "content-length".equalsIgnoreCase(nombre) || "content-type".equalsIgnoreCase(nombre);
    }

    @Override
    public void sendError(int codigo) {
        sendError(codigo, null);
    }

    @Override
    public void sendError(int codigo, String mensaje) {
        if (errorEnviado) {
            return;
        }
        errorEnviado = true;

        setStatus(codigo);
        this.tipoContenido = "application/json;charset=UTF-8";

        String texto = mensaje == null ? "" : mensaje.replace('"', '\'');
        String json = "{\"status\":" + codigo + ",\"error\":\"" + texto + "\"}";
        buffer.reset();
        buffer.writeBytes(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Nada puede salir a la red antes de pasar por el cifrado. */
    @Override
    public void flushBuffer() {
        // intencionalmente vacio
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void resetBuffer() {
        buffer.reset();
    }
}
