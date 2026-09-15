package cl.curso.usuarios.crypto;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * "Disfraza" la peticion original para que el resto de la aplicacion no se
 * entere de que venia cifrada.
 *
 * Un HttpServletRequestWrapper es una peticion que envuelve a otra y puede
 * cambiar lo que responden sus metodos. Aqui cambiamos:
 *   - el cuerpo (getInputStream/getReader): devolvemos el JSON ya descifrado
 *   - el Content-Type: por el cable viaja application/octet-stream (bytes
 *     opacos), pero hacia adentro decimos application/json para que Jackson
 *     y @RequestBody funcionen exactamente igual que antes.
 *
 * Gracias a esto, UsuarioController no necesita ni una linea de cambio.
 */
public class PeticionDescifrada extends HttpServletRequestWrapper {

    private static final String TIPO_JSON = "application/json;charset=UTF-8";

    private final byte[] cuerpo;

    public PeticionDescifrada(HttpServletRequest original, byte[] cuerpoDescifrado) {
        super(original);
        this.cuerpo = cuerpoDescifrado;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream fuente = new ByteArrayInputStream(cuerpo);

        return new ServletInputStream() {
            @Override
            public int read() {
                return fuente.read();
            }

            @Override
            public boolean isFinished() {
                return fuente.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // No usamos lectura asincrona en este proyecto.
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public String getContentType() {
        return cuerpo.length == 0 ? null : TIPO_JSON;
    }

    @Override
    public String getCharacterEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public int getContentLength() {
        return cuerpo.length;
    }

    @Override
    public long getContentLengthLong() {
        return cuerpo.length;
    }

    @Override
    public String getHeader(String nombre) {
        if ("content-type".equalsIgnoreCase(nombre)) {
            return getContentType();
        }
        if ("content-length".equalsIgnoreCase(nombre)) {
            return String.valueOf(cuerpo.length);
        }
        return super.getHeader(nombre);
    }

    @Override
    public Enumeration<String> getHeaders(String nombre) {
        if ("content-type".equalsIgnoreCase(nombre) || "content-length".equalsIgnoreCase(nombre)) {
            String valor = getHeader(nombre);
            return valor == null
                    ? Collections.emptyEnumeration()
                    : Collections.enumeration(List.of(valor));
        }
        return super.getHeaders(nombre);
    }
}
