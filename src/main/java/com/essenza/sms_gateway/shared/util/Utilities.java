package com.essenza.sms_gateway.shared.util;

import static org.apache.commons.lang3.StringUtils.trim;

public class Utilities {
    public static int toNumber(String numero) {
        try {
            return Integer.parseInt(trim(numero));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String getString(Object object){
        return object==null ?"":String.valueOf(object).trim().replaceAll("[\\r\\n]", "");
    }

    public static String separarPalabra(String cadena, int numero) {
        cadena = getString(cadena);

        if (cadena == null || cadena.isEmpty() || numero < 1 || numero > 2) {
            return "";
        }

        String[] palabras = cadena.split(" ");
        if (palabras.length == 1) {
            return (numero == 1) ? palabras[0] : "";
        }

        if (numero == 1) {
            return palabras[0];
        } else {
            StringBuilder result = new StringBuilder();
            for (int i = 1; i < palabras.length; i++) {
                result.append(palabras[i]);
                if (i < palabras.length - 1) {
                    result.append(" ");
                }
            }
            return result.toString();
        }
    }
}
