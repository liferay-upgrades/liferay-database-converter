package com.upgrade.tools.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link SchemeConverterUtil}. Covers the fix that removed the
 * silent {@code sb.append("-")} prefix hack in {@code readContent}, which
 * was previously needed to compensate for {@code _readFiles} consuming the
 * first byte of the input stream.
 */
public class TestSchemeConverterUtil {

    @Test
    public void testReadContentReturnsFileVerbatim() throws Exception {
        String input = "--\n-- PostgreSQL database dump\nCREATE TABLE x (id bigint);\n";

        try (InputStream stream = new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8))) {

            String result = SchemeConverterUtil.readContent(stream);

            Assertions.assertEquals(
                input, result,
                "readContent must return the input verbatim — no '-' prefix");
        }
    }

    @Test
    public void testReadContentNoLeadingHyphenInjected() throws Exception {
        // A previous implementation prepended "-" to the buffer. Verify the
        // method does NOT add any character that isn't in the source.
        String input = "CREATE TABLE x (id bigint);\n";

        try (InputStream stream = new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8))) {

            String result = SchemeConverterUtil.readContent(stream);

            Assertions.assertFalse(
                result.startsWith("-"),
                "readContent must not prepend a '-' to non-comment input");
            Assertions.assertEquals(input, result);
        }
    }

    @Test
    public void testReadContentEmptyStreamReturnsEmptyString() throws Exception {
        try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
            String result = SchemeConverterUtil.readContent(stream);

            Assertions.assertEquals("", result);
        }
    }

    @Test
    public void testReadContentPreservesUtf8(@TempDir Path tempDir)
        throws IOException {

        String input =
            "-- naïve column çase 文字\n" +
            "CREATE TABLE \"über\" (id bigint);\n";

        Path file = tempDir.resolve("utf8.sql");

        Files.writeString(file, input, StandardCharsets.UTF_8);

        try (InputStream stream = Files.newInputStream(file)) {
            Assertions.assertEquals(
                input, SchemeConverterUtil.readContent(stream),
                "UTF-8 multi-byte characters must round-trip");
        }
    }

    @Test
    public void testReadContentClosesStream() throws Exception {
        ClosedTrackingStream stream = new ClosedTrackingStream(
            "data\n".getBytes(StandardCharsets.UTF_8));

        SchemeConverterUtil.readContent(stream);

        Assertions.assertTrue(
            stream.closed,
            "readContent must close the input stream when done");
    }

    private static final class ClosedTrackingStream extends ByteArrayInputStream {
        boolean closed;

        ClosedTrackingStream(byte[] buffer) {
            super(buffer);
        }

        @Override
        public void close() throws IOException {
            super.close();

            closed = true;
        }
    }

}
