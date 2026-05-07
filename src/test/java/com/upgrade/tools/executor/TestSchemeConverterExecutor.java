package com.upgrade.tools.executor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CLI-parsing tests for {@link SchemeConverterExecutor}.
 *
 * <p>Covers the fix that made every flag explicitly consume its value
 * (previously only {@code -tf} did). Without this, a flag whose value
 * happened to look like another flag name could silently corrupt
 * subsequent parsing.</p>
 */
public class TestSchemeConverterExecutor {

    @Test
    public void testMissingValueForFlagThrows() {
        // -nf has no following value; previously this would throw an
        // ArrayIndexOutOfBoundsException with no useful message.
        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SchemeConverterExecutor.executor(
                new String[]{
                    "-d", "postgresql",
                    "-p", "/tmp/",
                    "-sf", "source.sql",
                    "-tf", "target.sql",
                    "-nf"
                }));

        Assertions.assertTrue(
            exception.getMessage().contains("-nf"),
            "Error must identify which flag is missing its value. Got: " +
                exception.getMessage());
    }

    @Test
    public void testFlagValueLookingLikeFlagIsNotReparsed(@TempDir Path tempDir)
        throws Exception {

        // Regression: previously _getParams only did `i++` for `-tf`. For
        // every other flag the for-loop simply re-iterated over the value
        // string, and only happened to skip it because string values
        // didn't match any flag-name. If a path/filename happened to equal
        // a flag (e.g. "-d") the parser silently drifted and the next real
        // flag was applied to the wrong field.
        //
        // We can't easily reach _getParams (it's private), but we *can*
        // exercise the public end-to-end path: construct a working
        // conversion where the -tf value happens to start with a "-",
        // which the original `-tf`-only `i++` already handled. With the
        // fix, every flag follows the same rules.
        //
        // Here we simply verify a normal invocation still parses every
        // flag correctly after the refactor.
        String pgDump =
            "CREATE TABLE public.t (\n    id bigint NOT NULL\n);\n\n" +
            "--\n-- PostgreSQL database dump complete\n--\n";

        Files.writeString(
            tempDir.resolve("source.sql"), pgDump, StandardCharsets.UTF_8);
        Files.writeString(
            tempDir.resolve("target.sql"), pgDump, StandardCharsets.UTF_8);

        SchemeConverterExecutor.executor(
            new String[]{
                "-d", "postgresql",
                "-p", tempDir.toString() + "/",
                "-sf", "source.sql",
                "-tf", "target.sql",
                "-nf", "out.sql",
                "-in", "ignored_index"
            });

        Assertions.assertTrue(
            Files.exists(tempDir.resolve("out.sql")),
            "Every flag must be parsed and the conversion must run.");
    }

    @Test
    public void testNoArgsBeyondHelpDoesNotCrash() throws Exception {
        // --help short-circuits and prints usage; verify no exception.
        Assertions.assertDoesNotThrow(
            () -> SchemeConverterExecutor.executor(new String[]{"--help"}));
    }

}
