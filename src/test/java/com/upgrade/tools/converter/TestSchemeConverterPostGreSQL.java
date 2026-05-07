package com.upgrade.tools.converter;

import com.upgrade.tools.exception.ConverterException;
import com.upgrade.tools.executor.SchemeConverterExecutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Albert Gomes Cabral
 */
public class TestSchemeConverterPostGreSQL {

    @Test
    public void testSchemeConverterColumnDefinitions() throws Exception {
        String path = _basePath + "column-definitions/";

        SchemeConverterExecutor.executor(
            new String[]{
                "-d", "postgresql", "-p", path,
                "-sf", "source.sql", "-tf", "target.sql",
                "-nf", "new-create-table-statement.sql"
            });

        String newConvertedContent = _readContent(
            path, "new-create-table-statement.sql");

        String targetContent = _readContent(
            path, "excepted_create_table_statement.sql");

        Assertions.assertEquals(
            targetContent, newConvertedContent, "failed");
    }

    @Test
    public void testSchemeConverterParameters() throws Exception {
        String path = _basePath + "parameters/";

        // valid files extensions

        Assertions.assertThrows(
            ConverterException.class,
            () -> SchemeConverterExecutor.executor(
                    new String[]{
                        "-d", "postgresql", "-p", path,
                        "-sf", "source.bak", "-tf", "target.bak",
                        "-nf", "new-create-table-statement.sql"
                    })
        );

        // supported database parameter

        Assertions.assertThrows(
            ConverterException.class,
            () -> SchemeConverterExecutor.executor(
                    new String[]{
                        "-d", "mariadb", "-p", path,
                        "-sf", "source.sql", "-tf", "target.sql",
                        "-nf", "new-create-table-statement.sql"
                    })
        );

        // invalid path directory

        Assertions.assertThrows(
            ConverterException.class,
            () -> SchemeConverterExecutor.executor(
                    new String[]{
                        "-d", "mariadb", "-p", "/path",
                        "-sf", "source.sql", "-tf", "target.sql",
                        "-nf", "new-create-table-statement.sql"
                    })
        );
    }

    @Test
    public void testDumpCompleteDelimiterConstant() {
        // Pin the magic-string delimiter so accidental edits are caught by
        // the test suite rather than producing silently-wrong output.
        Assertions.assertEquals(
            "--\n-- PostgreSQL database dump complete",
            PostGreSQLSchemeConverter.DUMP_COMPLETE_DELIMITER);
    }

    @Test
    public void testPostProcessInsertsAlterIndexAndRule() throws Exception {
        // The fixtures already contain ALTER TABLE / INDEX / RULE statements
        // in source.sql but not in target.sql. Run the converter and verify
        // that all of them appear in the output before the dump-complete
        // delimiter (i.e., the delimiter constant survived the refactor and
        // still drives splice positioning).
        String path = _basePath + "column-definitions/";

        SchemeConverterExecutor.executor(
            new String[]{
                "-d", "postgresql", "-p", path,
                "-sf", "source.sql", "-tf", "target.sql",
                "-nf", "post-process-check.sql"
            });

        try {
            String output = _readContent(path, "post-process-check.sql");

            int delimiterIndex = output.indexOf(
                PostGreSQLSchemeConverter.DUMP_COMPLETE_DELIMITER);

            Assertions.assertTrue(
                delimiterIndex > 0,
                "Output must contain the dump-complete delimiter");

            String beforeDelimiter = output.substring(0, delimiterIndex);

            Assertions.assertTrue(
                beforeDelimiter.contains(
                    "ALTER TABLE ONLY public.batchengineimporttask"),
                "ALTER TABLE PRIMARY KEY must be spliced before delimiter");
            Assertions.assertTrue(
                beforeDelimiter.contains(
                    "CREATE INDEX ix_ffc978a3 ON public.testaccountentry"),
                "CREATE INDEX must be spliced before delimiter");
            Assertions.assertTrue(
                beforeDelimiter.contains("CREATE RULE update_dlcontent_data_"),
                "CREATE RULE must be spliced before delimiter");
        }
        finally {
            new File(path + "post-process-check.sql").delete();
        }
    }

    @Test
    public void testIndexNameFlagSkipsUniqueIndex(@TempDir Path tempDir)
        throws Exception {

        // Build a minimal pg_dump fragment with one unique index and verify
        // that --index-name with that name suppresses it from the output.
        String source =
            "CREATE TABLE public.t (\n" +
            "    id bigint NOT NULL\n" +
            ");\n\n" +
            "CREATE UNIQUE INDEX ix_to_keep ON public.t USING btree (id);\n" +
            "CREATE UNIQUE INDEX ix_to_skip ON public.t USING btree (id);\n" +
            "--\n-- PostgreSQL database dump complete\n--\n";

        String target =
            "CREATE TABLE public.t (\n" +
            "    id bigint\n" +
            ");\n\n" +
            "--\n-- PostgreSQL database dump complete\n--\n";

        Path sourceFile = tempDir.resolve("source.sql");
        Path targetFile = tempDir.resolve("target.sql");

        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        Files.writeString(targetFile, target, StandardCharsets.UTF_8);

        SchemeConverterExecutor.executor(
            new String[]{
                "-d", "postgresql",
                "-p", tempDir.toString() + "/",
                "-sf", "source.sql",
                "-tf", "target.sql",
                "-nf", "out.sql",
                "-in", "ix_to_skip"
            });

        String output = Files.readString(
            tempDir.resolve("out.sql"), StandardCharsets.UTF_8);

        Assertions.assertTrue(
            output.contains("ix_to_keep"),
            "Unique index NOT in -in list must be present");
        Assertions.assertFalse(
            output.contains("ix_to_skip"),
            "Unique index in -in list must be skipped");
    }

    @Test
    public void testIndexNameFlagWithoutSkipKeepsAllUniqueIndexes(
            @TempDir Path tempDir)
        throws Exception {

        String source =
            "CREATE TABLE public.t (\n    id bigint NOT NULL\n);\n\n" +
            "CREATE UNIQUE INDEX ix_a ON public.t USING btree (id);\n" +
            "CREATE UNIQUE INDEX ix_b ON public.t USING btree (id);\n" +
            "--\n-- PostgreSQL database dump complete\n--\n";

        String target =
            "CREATE TABLE public.t (\n    id bigint\n);\n\n" +
            "--\n-- PostgreSQL database dump complete\n--\n";

        Files.writeString(
            tempDir.resolve("source.sql"), source, StandardCharsets.UTF_8);
        Files.writeString(
            tempDir.resolve("target.sql"), target, StandardCharsets.UTF_8);

        new PostGreSQLSchemeConverter().converter(
            tempDir.toString() + "/", "source.sql", "target.sql",
            "out.sql", List.of());

        String output = Files.readString(
            tempDir.resolve("out.sql"), StandardCharsets.UTF_8);

        Assertions.assertTrue(output.contains("ix_a"));
        Assertions.assertTrue(output.contains("ix_b"));
    }

    @AfterAll
    public static void cleanUp() {
        String path = _basePath + "column-definitions/";

        File file = new File(path + "new-create-table-statement.sql");

        System.out.printf("Clean up %s%n", file.delete());
    }

    private String _readContent(String path, String fileName) throws Exception {
        try (InputStream inputStream = new FileInputStream(path + fileName)) {
            return new String(
                Objects.requireNonNull(inputStream).readAllBytes());
        }
        catch (Exception exception) {
            throw new Exception(exception);
        }
    }

    private static final String _basePath =
        System.getProperty("user.dir") + "/src/test/resources/";

}
