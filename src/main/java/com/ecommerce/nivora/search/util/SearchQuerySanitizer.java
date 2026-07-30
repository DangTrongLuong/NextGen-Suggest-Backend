package com.ecommerce.nivora.search.util;

import java.util.regex.Pattern;

/**
 * Utility to sanitize and validate search queries before passing to AI service or logging to DB.
 */
public class SearchQuerySanitizer {

    // Regex for common SQL Injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(" +
                    "\\b(OR|AND)\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+" + // OR 1=1, AND 1=1
                    "|\\b(OR|AND)\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+" + // OR 'a'='a'
                    "|\\bUNION\\s+(ALL\\s+)?SELECT\\b" +                   // UNION SELECT
                    "|\\b(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|EXEC|SHOW|CREATE)\\b\\s+.*\\b(FROM|INTO|TABLE|DATABASE|SCHEMA)\\b" +
                    "|\\bINFORMATION_SCHEMA\\b" +                          // information_schema
                    "|\\bBENCHMARK\\s*\\(" +                               // BENCHMARK()
                    "|\\bSLEEP\\s*\\(" +                                   // SLEEP()
                    "|--\\s*$" +                                           // Trailing -- comment
                    "|--\\s*-" +                                           // -- - comment
                    "|/\\*.*?\\*/" +                                       // Block comment /* */
                    "|'\\s*(OR|AND)\\s+'" +                                // ' OR '
                    "|\"\\s*(OR|AND)\\s+\"" +                              // " OR "
                    ")"
    );

    // Regex for XSS & Script Injection patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(" +
                    "<\\s*script[^>]*>" +                                  // <script>
                    "|<\\s*/\\s*script\\s*>" +                             // </script>
                    "|javascript\\s*:" +                                   // javascript:
                    "|vbscript\\s*:" +                                     // vbscript:
                    "|onload\\s*=" +                                       // onload=
                    "|onerror\\s*=" +                                      // onerror=
                    "|onclick\\s*=" +                                      // onclick=
                    "|onmouseover\\s*=" +                                  // onmouseover=
                    "|<\\s*iframe[^>]*>" +                                 // <iframe>
                    "|<\\s*object[^>]*>" +                                 // <object>
                    "|<\\s*embed[^>]*>" +                                  // <embed>
                    "|eval\\s*\\(" +                                       // eval()
                    "|document\\.cookie" +                                 // document.cookie
                    ")"
    );

    // Regex for detecting queries with NO alphanumeric characters at all (pure attack punctuation or garbage)
    private static final Pattern ONLY_PUNCTUATION_PATTERN = Pattern.compile("^[^\\p{L}\\p{N}]+$");

    /**
     * Checks whether the search query is malicious or invalid.
     * @param query Raw user search input
     * @return true if query contains SQL injection, XSS, or non-searchable malicious content
     */
    public static boolean isMaliciousOrInvalid(String query) {
        if (query == null) {
            return true;
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return true;
        }

        // Length limits (excessively long queries or 1-char symbols)
        if (trimmed.length() > 300) {
            return true;
        }

        // Check if query is only special punctuation (e.g. "' -- -", "/* */", ";")
        if (ONLY_PUNCTUATION_PATTERN.matcher(trimmed).matches()) {
            return true;
        }

        // Check for SQL Injection patterns
        if (SQL_INJECTION_PATTERN.matcher(trimmed).find()) {
            return true;
        }

        // Check for XSS / Script Injection patterns
        if (XSS_PATTERN.matcher(trimmed).find()) {
            return true;
        }

        return false;
    }

    /**
     * Clean leading and trailing quotes or extra control chars if safe.
     */
    public static String sanitize(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().replaceAll("[\\r\\n\\t]+", " ");
    }
}
