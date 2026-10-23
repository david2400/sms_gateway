package com.essenza.sms_gateway.modules.GestionPlatillas.domain.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.SmsEncoding;

/**
 * Calcula codificación, longitud y número de partes de un SMS (GSM 03.38).
 */
public final class SmsSegmentCalculator {

    /** Alfabeto básico GSM-7. */
    private static final String GSM_BASIC =
            "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
                    + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";
    /** Extensión GSM-7: cada uno ocupa 2 posiciones. */
    private static final String GSM_EXTENDED = "^{}\\[~]|€\f";

    private SmsSegmentCalculator() {
    }

    public static SmsEncoding encodingOf(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (GSM_BASIC.indexOf(c) < 0 && GSM_EXTENDED.indexOf(c) < 0) {
                return SmsEncoding.UCS2;
            }
        }
        return SmsEncoding.GSM7;
    }

    public static int lengthOf(String text, SmsEncoding encoding) {
        if (encoding == SmsEncoding.UCS2) {
            return text.length();
        }
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            length += GSM_EXTENDED.indexOf(text.charAt(i)) >= 0 ? 2 : 1;
        }
        return length;
    }

    public static int partsOf(int length, SmsEncoding encoding) {
        if (length == 0) {
            return 0;
        }
        return length <= encoding.singleLimit() ? 1 : (int) Math.ceil(length / (double) encoding.multipartLimit());
    }
}
