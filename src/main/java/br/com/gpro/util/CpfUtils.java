package br.com.gpro.util;

public final class CpfUtils {
    private CpfUtils() {}

    /** Remove tudo que não é dígito. */
    public static String onlyDigits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Validação completa do CPF (11 dígitos + DVs). */
    public static boolean isValid(String cpf) {
        String d = onlyDigits(cpf);
        if (d.length() != 11) return false;

        // rejeita sequências (000... 111... etc.)
        if (d.chars().distinct().count() == 1) return false;

        int dv1 = calcDV(d, 9, 10); // usa pesos 10..2 sobre os 9 primeiros
        int dv2 = calcDV(d,10, 11); // usa pesos 11..2 sobre os 10 primeiros

        return dv1 == (d.charAt(9) - '0') && dv2 == (d.charAt(10) - '0');
    }

    private static int calcDV(String d, int len, int startWeight) {
        int soma = 0, peso = startWeight;
        for (int i = 0; i < len; i++) {
            soma += (d.charAt(i) - '0') * (peso--);
        }
        int resto = soma % 11;
        return (resto < 2) ? 0 : (11 - resto);
    }

    /** Formata para ###.###.###-## (se tiver 11 dígitos), senão retorna original. */
    public static String format(String cpf) {
        String d = onlyDigits(cpf);
        if (d.length() != 11) return cpf;
        return d.substring(0,3)+"."+d.substring(3,6)+"."+d.substring(6,9)+"-"+d.substring(9);
    }

    /** Retorna os 11 dígitos, se válido; caso contrário lança IllegalArgumentException. */
    public static String normalizeOrThrow(String cpf) {
        String d = onlyDigits(cpf);
        if (!isValid(d)) throw new IllegalArgumentException("CPF inválido");
        return d;
    }
}
