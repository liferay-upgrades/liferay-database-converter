package com.upgrade.tools.converter;

import com.upgrade.tools.executor.SchemeConverterExecutor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for the MySQL converter.
 *
 * <p>Pre-existing test coverage was PostgreSQL-only. After hoisting the MySQL
 * regex Patterns to {@code static final} fields we wanted at least one
 * end-to-end test exercising both the column-definition fixup and the
 * post-process table-name normalization.</p>
 *
 * <p>The converter <strong>replaces target column definitions with the
 * source's definition where the column name matches</strong>. It does not
 * add source-only columns to the target. The fixtures in
 * {@code src/test/resources/mysql-fixtures/} therefore share the same
 * column names but have different types in target.</p>
 */
public class TestSchemeConverterMySQL {

    @AfterEach
    public void cleanUp() {
        File output = new File(_path + "out.sql");

        if (output.exists()) {
            output.delete();
        }
    }

    @Test
    public void testMySQLColumnDefinitionsAreFixedFromSource() throws Exception {
        SchemeConverterExecutor.executor(
            new String[]{
                "-d", "mysql", "-p", _path,
                "-sf", "source.sql", "-tf", "target.sql",
                "-nf", "out.sql"
            });

        String output = Files.readString(
            Path.of(_path, "out.sql"), StandardCharsets.UTF_8);

        // accountid was `bigint` in target, `bigint NOT NULL` in source.
        Assertions.assertTrue(
            output.contains("`accountid` bigint NOT NULL"),
            "Target's looser `accountid` definition must be replaced with " +
                "source's stricter one. Got:\n" + output);

        // description was `blob` in target, `text` in source.
        Assertions.assertTrue(
            output.contains("`description` text"),
            "Target's `description blob` must be replaced with source's " +
                "`description text`. Got:\n" + output);
        Assertions.assertFalse(
            output.contains("`description` blob"),
            "Old target type for `description` must not survive. Got:\n" +
                output);
    }

    @Test
    public void testMySQLPostProcessNormalizesDropTableCasing()
        throws Exception {

        SchemeConverterExecutor.executor(
            new String[]{
                "-d", "mysql", "-p", _path,
                "-sf", "source.sql", "-tf", "target.sql",
                "-nf", "out.sql"
            });

        String output = Files.readString(
            Path.of(_path, "out.sql"), StandardCharsets.UTF_8);

        Assertions.assertFalse(
            output.contains("DROP TABLE IF EXISTS `MYACCOUNT`"),
            "Uppercase target name must NOT survive in DROP TABLE. " +
                "Got:\n" + output);
        Assertions.assertTrue(
            output.contains("DROP TABLE IF EXISTS `myaccount`;"),
            "DROP TABLE must use source's lowercase name. Got:\n" + output);
    }

    @Test
    public void testMySQLPostProcessNormalizesLockTablesCasing()
        throws Exception {

        SchemeConverterExecutor.executor(
            new String[]{
                "-d", "mysql", "-p", _path,
                "-sf", "source.sql", "-tf", "target.sql",
                "-nf", "out.sql"
            });

        String output = Files.readString(
            Path.of(_path, "out.sql"), StandardCharsets.UTF_8);

        Assertions.assertTrue(
            output.contains("LOCK TABLES `myaccount` WRITE;"),
            "LOCK TABLES casing must be normalized. Got:\n" + output);
        Assertions.assertFalse(
            output.contains("LOCK TABLES `MYACCOUNT`"),
            "Uppercase LOCK TABLES must not survive. Got:\n" + output);
    }

    private static final String _path =
        System.getProperty("user.dir") + "/src/test/resources/mysql-fixtures/";

}
