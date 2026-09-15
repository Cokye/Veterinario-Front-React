package cl.curso.usuarios.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registra el filtro de cifrado en la cadena de filtros del servidor.
 *
 * El ORDEN es lo importante de esta clase:
 *
 *   -200  CifradoFiltro          <- nosotros (descifra la entrada)
 *   -100  springSecurityFilterChain (JWT, CORS, autorizacion)
 *    ...  Spring MVC -> UsuarioController
 *
 * Al ser el mas externo, el cifrado es lo PRIMERO que se deshace al entrar y
 * lo ULTIMO que se aplica al salir; todo lo que hay en medio (incluidas las
 * respuestas 401 de Spring Security) queda cubierto.
 */
@Configuration
public class CifradoConfig {

    /** Orden de la cadena de Spring Security (SecurityProperties.DEFAULT_FILTER_ORDER). */
    private static final int ORDEN_SPRING_SECURITY = -100;

    @Bean
    public FilterRegistrationBean<CifradoFiltro> registroCifradoFiltro(
            CifradoService cifradoService,
            @Value("${app.cifrado.obligatorio:true}") boolean obligatorio,
            @Value("${app.cors.origenes-permitidos}") List<String> origenesPermitidos) {

        FilterRegistrationBean<CifradoFiltro> registro = new FilterRegistrationBean<>();
        registro.setFilter(new CifradoFiltro(cifradoService, obligatorio, origenesPermitidos));
        registro.addUrlPatterns("/*");
        registro.setOrder(ORDEN_SPRING_SECURITY - 100);
        registro.setName("cifradoFiltro");
        return registro;
    }
}
