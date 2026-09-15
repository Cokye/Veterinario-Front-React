package cl.curso.usuarios.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Servicio central de cifrado de la comunicacion front <-> back.
 *
 * ESQUEMA "HIBRIDO" (es el mismo que usa TLS por dentro):
 *
 *   1. El servidor tiene un par de claves RSA (publica + privada).
 *      La PUBLICA se la entrega a cualquiera (endpoint /api/crypto/public-key).
 *      La PRIVADA nunca sale de aqui.
 *
 *   2. En CADA peticion el navegador inventa una clave AES-256 nueva y
 *      desechable ("clave de sesion"). Cifra el cuerpo con AES-GCM (rapido) y
 *      envia esa clave AES envuelta con la clave publica RSA (lenta pero no
 *      necesita secreto compartido previo).
 *
 *   3. El servidor abre el sobre con su clave privada, recupera la clave AES,
 *      descifra el cuerpo, procesa, y responde cifrando con LA MISMA clave AES.
 *      Como esa clave solo la conocen el navegador que la creo y el servidor,
 *      nadie mas puede leer la respuesta.
 *
 * Por que AES-GCM: ademas de cifrar, "autentica" (agrega un tag de 16 bytes).
 * Si alguien modifica un solo bit del mensaje, el descifrado falla en vez de
 * devolver basura. Eso nos da confidencialidad E integridad.
 *
 * IMPORTANTE (para la clase): esto NO reemplaza a HTTPS/TLS en produccion.
 * Es una capa ADICIONAL de cifrado de extremo a extremo sobre el payload.
 */
@Component
public class CifradoService {

    /** RSA-OAEP con SHA-256, que es lo que soporta el WebCrypto del navegador. */
    private static final String TRANSFORMACION_RSA = "RSA/ECB/OAEPPadding";

    /** AES en modo GCM, sin padding (GCM no lo necesita). */
    private static final String TRANSFORMACION_AES = "AES/GCM/NoPadding";

    /** Tamano del vector de inicializacion (IV) que recomienda GCM: 12 bytes. */
    public static final int LARGO_IV = 12;

    /** Largo del tag de autenticacion en bits (el valor por defecto de WebCrypto). */
    private static final int LARGO_TAG_BITS = 128;

    private final PublicKey clavePublica;
    private final PrivateKey clavePrivada;
    private final String clavePublicaBase64;
    private final String idClave;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Si en application.properties se definen las claves (base64), se usan esas.
     * Si no, se genera un par nuevo cada vez que arranca la app.
     *
     * Generarlas al arranque es comodo en desarrollo, pero significa que al
     * reiniciar el backend la clave publica cambia; por eso el frontend sabe
     * volver a pedirla cuando el servidor le dice "esa clave ya no es mia".
     */
    public CifradoService(
            @Value("${app.cifrado.clave-publica:}") String clavePublicaProp,
            @Value("${app.cifrado.clave-privada:}") String clavePrivadaProp) throws Exception {

        if (!clavePublicaProp.isBlank() && !clavePrivadaProp.isBlank()) {
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            this.clavePublica = fabrica.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(clavePublicaProp)));
            this.clavePrivada = fabrica.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(clavePrivadaProp)));
        } else {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();
            this.clavePublica = par.getPublic();
            this.clavePrivada = par.getPrivate();
        }

        // La clave publica viaja en formato SPKI (X.509) codificado en base64:
        // es exactamente lo que espera crypto.subtle.importKey('spki', ...).
        this.clavePublicaBase64 = Base64.getEncoder().encodeToString(clavePublica.getEncoded());

        // "Huella" de la clave: los primeros 8 bytes de su SHA-256. Sirve para
        // que el frontend sepa si la clave que tiene guardada sigue vigente.
        byte[] huella = MessageDigest.getInstance("SHA-256").digest(clavePublica.getEncoded());
        this.idClave = HexFormat.of().formatHex(huella, 0, 8);

        System.out.println("  Cifrado activo. Clave publica RSA id=" + idClave);
    }

    public String getClavePublicaBase64() {
        return clavePublicaBase64;
    }

    public String getIdClave() {
        return idClave;
    }

    /**
     * Abre el "sobre": descifra con la clave privada RSA los bytes de la clave
     * AES que mando el navegador, y la reconstruye como SecretKey.
     */
    public SecretKey abrirClaveAes(String claveEnvueltaBase64) throws Exception {
        byte[] envuelta = Base64.getDecoder().decode(claveEnvueltaBase64);

        Cipher rsa = Cipher.getInstance(TRANSFORMACION_RSA);
        // Hay que declarar SHA-256 tanto para el hash como para la mascara MGF1:
        // si solo se pone el nombre de la transformacion, Java usa SHA-1 en MGF1
        // y el descifrado NO coincide con el del navegador.
        rsa.init(Cipher.DECRYPT_MODE, clavePrivada, new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));

        byte[] bytesClave = rsa.doFinal(envuelta);
        return new SecretKeySpec(bytesClave, "AES");
    }

    /** Descifra un cuerpo AES-GCM (el tag de autenticacion viene al final). */
    public byte[] descifrar(SecretKey clave, byte[] iv, byte[] cifrado) throws Exception {
        Cipher aes = Cipher.getInstance(TRANSFORMACION_AES);
        aes.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG_BITS, iv));
        return aes.doFinal(cifrado);
    }

    /** Cifra un cuerpo con AES-GCM; devuelve ciphertext + tag concatenados. */
    public byte[] cifrar(SecretKey clave, byte[] iv, byte[] plano) throws Exception {
        Cipher aes = Cipher.getInstance(TRANSFORMACION_AES);
        aes.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG_BITS, iv));
        return aes.doFinal(plano);
    }

    /**
     * IV nuevo y aleatorio para cada respuesta.
     * REGLA DE ORO de GCM: jamas repetir el par (clave, IV).
     */
    public byte[] nuevoIv() {
        byte[] iv = new byte[LARGO_IV];
        aleatorio.nextBytes(iv);
        return iv;
    }
}
