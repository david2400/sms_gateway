package com.essenza.sms_gateway.modules.SmsCore.shared.crypto;

import java.io.Console;

/**
 * Utilidad de línea de comandos (sin Spring) para preparar claves cifradas.
 * <pre>
 *   llave              genera una llave nueva para SMS_CRYPTO_KEY
 *   cifrar [texto]     cifra con la llave de SMS_CRYPTO_KEY; sin texto lo pide por consola (no queda en el historial)
 * </pre>
 * En IntelliJ: Run → Edit Configurations → Application, main class esta clase, "Program arguments" y
 * "Environment variables" con SMS_CRYPTO_KEY.
 */
public final class ClaveCifradaCli {

    private ClaveCifradaCli() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            uso();
            return;
        }
        switch (args[0]) {
            case "llave" -> System.out.println(AesGcmCipher.generateKey());
            case "cifrar" -> {
                String key = System.getenv("SMS_CRYPTO_KEY");
                if (key == null || key.isBlank()) {
                    System.err.println("Defina la variable de entorno SMS_CRYPTO_KEY");
                    System.exit(1);
                }
                String plain = args.length > 1 ? args[1] : leerOculto();
                System.out.println(new AesGcmCipher(key).encrypt(plain));
            }
            default -> uso();
        }
    }

    private static String leerOculto() {
        Console console = System.console();
        if (console == null) {
            System.err.println("Sin consola interactiva: pase el texto como segundo argumento");
            System.exit(1);
        }
        return new String(console.readPassword("Clave a cifrar: "));
    }

    private static void uso() {
        System.out.println("Uso: ClaveCifradaCli llave | ClaveCifradaCli cifrar [texto]");
    }
}
