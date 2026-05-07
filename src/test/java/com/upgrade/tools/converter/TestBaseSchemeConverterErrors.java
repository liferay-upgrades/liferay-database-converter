package com.upgrade.tools.converter;

import com.upgrade.tools.exception.ConverterException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the file-validation contract added to
 * {@link BaseSchemeConverter#converter}. We use the PostgreSQL converter as
 * a concrete subclass since {@link BaseSchemeConverter} is abstract and the
 * file-validation logic is database-agnostic.
 */
public class TestBaseSchemeConverterErrors {

    @Test
    public void testInvalidExtensionThrowsConverterException(@TempDir Path tempDir)
        throws Exception {

        Path source = tempDir.resolve("source.bak");
        Path target = tempDir.resolve("target.bak");

        Files.writeString(source, "data");
        Files.writeString(target, "data");

        SchemeConverter converter = new PostGreSQLSchemeConverter();

        ConverterException exception = Assertions.assertThrows(
            ConverterException.class,
            () -> converter.converter(
                tempDir.toString() + "/", "source.bak", "target.bak",
                "out.sql", List.of()));

        Assertions.assertTrue(
            exception.getMessage().contains(".sql"),
            "Error message must mention required .sql extension. Got: " +
                exception.getMessage());
    }

    @Test
    public void testEmptySourceThrowsConverterException(@TempDir Path tempDir)
        throws Exception {

        Path source = tempDir.resolve("source.sql");
        Path target = tempDir.resolve("target.sql");

        Files.createFile(source);
        Files.writeString(target, "CREATE TABLE x (id bigint);\n");

        SchemeConverter converter = new PostGreSQLSchemeConverter();

        ConverterException exception = Assertions.assertThrows(
            ConverterException.class,
            () -> converter.converter(
                tempDir.toString() + "/", "source.sql", "target.sql",
                "out.sql", List.of()));

        Assertions.assertTrue(
            exception.getMessage().toLowerCase().contains("empty"),
            "Error message must indicate emptiness. Got: " +
                exception.getMessage());
        Assertions.assertTrue(
            exception.getMessage().contains("source"),
            "Error message must identify which file (source). Got: " +
                exception.getMessage());
    }

    @Test
    public void testEmptyTargetThrowsConverterException(@TempDir Path tempDir)
        throws Exception {

        Path source = tempDir.resolve("source.sql");
        Path target = tempDir.resolve("target.sql");

        Files.writeString(source, "CREATE TABLE x (id bigint);\n");
        Files.createFile(target);

        SchemeConverter converter = new PostGreSQLSchemeConverter();

        ConverterException exception = Assertions.assertThrows(
            ConverterException.class,
            () -> converter.converter(
                tempDir.toString() + "/", "source.sql", "target.sql",
                "out.sql", List.of()));

        Assertions.assertTrue(
            exception.getMessage().contains("target"),
            "Error message must identify which file (target). Got: " +
                exception.getMessage());
    }

    @Test
    public void testMissingSourceThrowsConverterException(@TempDir Path tempDir)
        throws Exception {

        Path target = tempDir.resolve("target.sql");

        Files.writeString(target, "CREATE TABLE x (id bigint);\n");

        SchemeConverter converter = new PostGreSQLSchemeConverter();

        ConverterException exception = Assertions.assertThrows(
            ConverterException.class,
            () -> converter.converter(
                tempDir.toString() + "/", "missing.sql", "target.sql",
                "out.sql", List.of()));

        Assertions.assertTrue(
            exception.getMessage().contains("does not exist"),
            "Error message must say file is missing. Got: " +
                exception.getMessage());
        Assertions.assertTrue(
            exception.getMessage().contains("missing.sql"),
            "Error message must include the path. Got: " +
                exception.getMessage());
    }

    @Test
    public void testStreamNotPreAdvancedAffectsContent(@TempDir Path tempDir)
        throws Exception {

        // Regression: previously _readFiles called inputStream.read() to test
        // for emptiness, advancing the stream by 1 byte and relying on
        // readContent's "-" prefix to compensate. Verify both source and
        // target's first byte are now preserved by checking that the output
        // file starts with what the source file starts with.
        String source =
            "CREATE TABLE x (\n" +
            "    id bigint NOT NULL,\n" +
            "    val bigint\n" +
            ");\n";

        String target =
            "CREATE TABLE x (\n" +
            "    id bigint,\n" +
            "    val bigint\n" +
            ");\n";

        Path sourceFile = tempDir.resolve("source.sql");
        Path targetFile = tempDir.resolve("target.sql");

        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        Files.writeString(targetFile, target, StandardCharsets.UTF_8);

        SchemeConverter converter = new PostGreSQLSchemeConverter();

        converter.converter(
            tempDir.toString() + "/", "source.sql", "target.sql",
            "out.sql", List.of());

        String output = Files.readString(
            tempDir.resolve("out.sql"), StandardCharsets.UTF_8);

        Assertions.assertTrue(
            output.startsWith("CREATE"),
            "Output must start with 'CREATE' — first byte must NOT be " +
                "lost or substituted with '-'. Got: " +
                output.substring(0, Math.min(20, output.length())));
    }

}
