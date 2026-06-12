package com.ecommerce.nivora.common.utils;


import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class SlugUtils {

    public String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutDiacritics = pattern.matcher(normalized).replaceAll("");
        return withoutDiacritics
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    public String uniqueSlug(String base, java.util.function.Predicate<String> exists) {
        String slug = toSlug(base);
        if (!exists.test(slug)) return slug;
        int counter = 1;
        while (exists.test(slug + "-" + counter)) counter++;
        return slug + "-" + counter;
    }
}
