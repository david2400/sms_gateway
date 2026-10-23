package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tipo de dato de una variable de plantilla: valida el valor recibido y lo formatea para el SMS.
 * <ul>
 *   <li>{@code TEXTO}: cualquier texto (se recorta).</li>
 *   <li>{@code ENTERO}: dígitos con signo opcional.</li>
 *   <li>{@code DECIMAL}: número con punto o coma decimal.</li>
 *   <li>{@code MONEDA}: número; se muestra en pesos colombianos ({@code $1.250.000} / {@code $1.250,50}).</li>
 *   <li>{@code FECHA}: {@code yyyy-MM-dd} o {@code dd/MM/yyyy}; se muestra {@code dd/MM/yyyy}.</li>
 *   <li>{@code HORA}: {@code HH:mm} (24 h); se muestra {@code h:mm AM/PM} (ej. {@code 14:30} → {@code 2:30 PM}).</li>
 *   <li>{@code URL}: debe iniciar por {@code http://} o {@code https://}.</li>
 * </ul>
 */
public enum VariableDataType {

    TEXTO {
        @Override
        public String format(String value) {
            return value.trim();
        }
    },
    ENTERO {
        @Override
        public String format(String value) {
            String v = value.trim();
            if (!INTEGER.matcher(v).matches()) {
                throw new IllegalArgumentException("debe ser un número entero");
            }
            return v;
        }
    },
    DECIMAL {
        @Override
        public String format(String value) {
            String v = value.trim();
            if (!DECIMAL_NUMBER.matcher(v).matches()) {
                throw new IllegalArgumentException("debe ser un número (ej. 12.5)");
            }
            return v.replace(',', '.');
        }
    },
    MONEDA {
        @Override
        public String format(String value) {
            String v = value.trim().replace("$", "").replace(" ", "");
            if (!DECIMAL_NUMBER.matcher(v).matches()) {
                throw new IllegalArgumentException("debe ser un valor numérico (ej. 125000 o 125000.50)");
            }
            BigDecimal amount = new BigDecimal(v.replace(',', '.'));
            boolean hasCents = amount.stripTrailingZeros().scale() > 0;
            DecimalFormat format = new DecimalFormat(hasCents ? "#,##0.00" : "#,##0", COP);
            return "$" + format.format(amount);
        }
    },
    FECHA {
        @Override
        public String format(String value) {
            String v = value.trim();
            for (DateTimeFormatter f : new DateTimeFormatter[]{DateTimeFormatter.ISO_LOCAL_DATE, DATE_OUT}) {
                try {
                    return LocalDate.parse(v, f).format(DATE_OUT);
                } catch (DateTimeParseException ignored) {
                    // se intenta el siguiente formato
                }
            }
            throw new IllegalArgumentException("debe ser una fecha yyyy-MM-dd o dd/MM/yyyy");
        }
    },
    HORA {
        @Override
        public String format(String value) {
            try {
                return LocalTime.parse(value.trim(), TIME_IN).format(TIME_OUT);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("debe ser una hora HH:mm (24 h)");
            }
        }
    },
    URL {
        @Override
        public String format(String value) {
            String v = value.trim();
            if (!URL_PATTERN.matcher(v).matches()) {
                throw new IllegalArgumentException("debe ser una URL http:// o https://");
            }
            return v;
        }
    };

    private static final Pattern INTEGER = Pattern.compile("-?\\d{1,18}");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("-?\\d{1,15}([.,]\\d{1,4})?");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S{3,}");
    private static final DecimalFormatSymbols COP = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DATE_OUT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter TIME_IN = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter TIME_OUT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    static {
        COP.setGroupingSeparator('.');
        COP.setDecimalSeparator(',');
    }

    /**
     * Valida y formatea el valor.
     *
     * @throws IllegalArgumentException con el motivo (en español) si el valor no es válido para el tipo
     */
    public abstract String format(String value);
}
