package com.essenza.sms_gateway.modules.SmsCore.shared.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesGcmCipherTest {

    private final AesGcmCipher cipher = new AesGcmCipher(AesGcmCipher.generateKey());

    @Test
    void cifraYDescifraConCaracteresEspeciales() {
        for (String plain : new String[]{"M451v0,123", "Mv.SEdD5", "ñ áé €", ""}) {
            String enc = cipher.encrypt(plain);
            assertTrue(enc.startsWith(AesGcmCipher.PREFIX));
            assertEquals(plain, cipher.decrypt(enc));
        }
    }

    @Test
    void mismaClaveDaTextosDistintos() {
        assertNotEquals(cipher.encrypt("abc"), cipher.encrypt("abc"));
    }

    @Test
    void otraLlaveNoDescifra() {
        String enc = cipher.encrypt("abc");
        AesGcmCipher otra = new AesGcmCipher(AesGcmCipher.generateKey());
        assertThrows(IllegalArgumentException.class, () -> otra.decrypt(enc));
    }

    @Test
    void textoAlteradoNoDescifra() {
        String enc = cipher.encrypt("abcdef");
        char[] c = enc.toCharArray();
        int i = AesGcmCipher.PREFIX.length() + 20; // dentro del texto cifrado, lejos del relleno Base64
        c[i] = c[i] == 'A' ? 'B' : 'A';
        assertThrows(IllegalArgumentException.class, () -> cipher.decrypt(new String(c)));
    }

    @Test
    void llaveInvalidaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> new AesGcmCipher("corta"));
        assertThrows(IllegalArgumentException.class, () -> new AesGcmCipher("AAAA"));
        assertThrows(IllegalArgumentException.class, () -> new AesGcmCipher(" "));
    }

    @Test
    void formatoDesconocidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> cipher.decrypt("textoplano"));
    }
}
