package com.upgrade.tools.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Albert Gomes Cabral
 */
public class SchemeConverterUtil {

    public static String readContent(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            return stringBuilder.toString();
        }
    }

    public static List<String> readChunks(
        InputStream inputStream, int chunkSize) throws IOException {

        List<String> chunks = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int count = 0;

            StringBuilder chunkBuilder = new StringBuilder();
            chunkBuilder.append("-");

            while ((line = bufferedReader.readLine()) != null) {
                count++;

                if (count == chunkSize) {
                    chunks.add(chunkBuilder.toString());
                    chunkBuilder.setLength(0);
                    count = 0;
                }
                else if (chunkBuilder.length() >= chunkSize) {
                    chunks.add(chunkBuilder.toString());
                    chunkBuilder.setLength(0);
                    chunkBuilder.append(line).append("\n");
                }
                else {
                    chunkBuilder.append(line).append("\n");
                }
            }

            if (chunks.isEmpty()) {
                chunks.add(chunkBuilder.toString());
            }
        }

        return chunks;
    }

    public static List<String> readChunksSafe(InputStream inputStream, int bufferSize)
        throws IOException {

        List<String> chunks = new ArrayList<>();

        byte[] buffer = new byte[bufferSize];
        int bytesRead;

        try (BufferedInputStream bufferedInputStream =
                 new BufferedInputStream(inputStream)) {

            chunks.add("-");

            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                chunks.add(new String(
                    buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
        }

        return chunks;
    }

}
