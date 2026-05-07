package com.upgrade.tools.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author Albert Gomes Cabral
 */
public class SchemeConverterUtil {

    /**
     * Reads the entire contents of {@code inputStream} as UTF-8 text and
     * returns it line-by-line, terminating each line with {@code "\n"}
     * (including the trailing line). The input stream is closed by this
     * method.
     */
    public static String readContent(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        }
    }

    private SchemeConverterUtil() {
    }

}
