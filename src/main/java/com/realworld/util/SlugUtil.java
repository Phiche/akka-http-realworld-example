package com.realworld.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");

    public static String toSlug(String input) {
        if (input == null) {
            return UUID.randomUUID().toString(); // Or throw an IllegalArgumentException
        }

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = EDGESDHASHES.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH);

        if (slug.isEmpty()) {
            // If the slug becomes empty after processing (e.g., input was all special characters)
            // generate a unique slug based on UUID to prevent issues.
            return UUID.randomUUID().toString();
        }

        // Append a short unique ID to reduce collision likelihood for similar titles
        String uniqueSuffix = "-" + UUID.randomUUID().toString().substring(0, 6);

        // Ensure slug isn't too long with suffix
        int maxBaseLength = 255 - uniqueSuffix.length();
        if (slug.length() > maxBaseLength) {
            slug = slug.substring(0, maxBaseLength);
        }

        return slug + uniqueSuffix;
    }

    public static String toSlugSimple(String input) {
        if (input == null) return ""; // Or throw exception

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slugPart = NONLATIN.matcher(normalized).replaceAll("");
        slugPart = EDGESDHASHES.matcher(slugPart).replaceAll("");
        slugPart = slugPart.toLowerCase(Locale.ENGLISH);

        if (slugPart.isEmpty()) {
            return "n-a"; // Or some other default for empty inputs
        }
        return slugPart;
    }
}
