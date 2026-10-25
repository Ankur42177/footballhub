package com.footballhub.common.util;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** URL slug helpers. Slugs are ASCII, lowercase and hyphen-separated. */
public final class Slugs {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_BASE_LENGTH = 80;

    private Slugs() {
    }

    public static String slugify(String input) {
        String text = input == null ? "" : input.toLowerCase(Locale.ROOT);
        // Letters that Unicode decomposition does not reduce to ASCII.
        text = text.replace("ø", "o").replace("æ", "ae").replace("œ", "oe").replace("ß", "ss")
                .replace("đ", "d").replace("ł", "l").replace("þ", "th")
                .replace("'", "").replace("’", "");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String ascii = DIACRITICS.matcher(normalized).replaceAll("").replace("&", " and ");
        String slug = trimHyphens(NON_ALPHANUMERIC.matcher(ascii).replaceAll("-"));
        if (slug.length() > MAX_BASE_LENGTH) {
            slug = trimHyphens(slug.substring(0, MAX_BASE_LENGTH));
        }
        return slug.isEmpty() ? "post" : slug;
    }

    public static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static String trimHyphens(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }
}
