package com.upgrade.tools.executor;

import com.upgrade.tools.converter.SchemeConverter;
import com.upgrade.tools.initialize.SchemeConverterInitialize;
import com.upgrade.tools.util.Print;
import com.upgrade.tools.util.ResultsThreadLocal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * @author Albert Gomes Cabral
 */
public class SchemeConverterExecutor {

    public static void executor(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--help")) {
            Print.info(_helper());

            return;
        }

        Params params;

        if (_isInteractiveInvocation(args)) {
            params = _interactivePrompt();

            if (params == null) {
                Print.info("Cancelled.");

                return;
            }
        }
        else {
            params = _getParams(args);

            if (params == null) {
                throw new RuntimeException(
                    "Is mandatory to inform valid arguments to execute it." +
                        "\nType --help to see the usage");
            }
        }

        long start = System.nanoTime();

        SchemeConverter schemeConverter =
            SchemeConverterInitialize.getConverterType(
                    params.databaseType);

        schemeConverter.converter(
            params.path, params.sourceFileName, params.targetFileName,
            params.newFileName, params.indexesName);

        if (ResultsThreadLocal.getResultsThreadLocal()) {
            long elapsedSeconds =
                (System.nanoTime() - start) / 1_000_000_000L;

            Print.info(
                "Converted with success. Completed in %d seconds".formatted(
                    elapsedSeconds));
        }
        else {
            Print.error("Converter fail. Try again.");
        }
    }

    private static Params _getParams(String[] args) {
        if (args.length == 0) {
            return null;
        }

        Params params = new Params();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--database-type":
                case "-d":
                    params.databaseType = args[i + 1];
                    break;
                case "--path":
                case "-p":
                    params.path = args[i + 1];
                    break;
                case "--source-file":
                case "-sf":
                    params.sourceFileName = args[i + 1];
                    break;
                case "--target-file":
                case "-tf":
                    params.targetFileName = args[i + 1];
                    i++;
                    break;
                case "--new-file":
                case "-nf":
                    params.newFileName = args[i + 1];
                    break;
                case "--index-name":
                case "-in":
                    String indexes = args[i + 1];

                    String[] indexNameScratchSplit = indexes.split(",");

                    Collections.addAll(params.indexesName, indexNameScratchSplit);

                    break;
            }
        }

        return params;
    }

    private static String _helper() {
        return """
            See scheme converter usages:\s
             \
            
            --database-type or -d\s
            \t the database type target\s
            
            --path or -p\s
            \t the absolut path to database dump files\s
            
            -sf or --source-file\s
            \t the source file name that contains Liferay's scheme with target specifications\s
            
            --new-file or -nf\s
            \t the output file name that contains the converted dump\s
            
            --target-file or -tf\s
            \t the target file name that contains customer data\s

            --index-name or -in\s
            \t the unique index(es) to be skipped\s
            """;
    }

    private static Params _interactivePrompt() {
        Scanner scanner = new Scanner(System.in);

        Print.info(
            "=== Liferay Database Schema Converter — interactive mode ===");
        Print.info(
            "Press Ctrl+C at any time to abort.\n");

        Params params = new Params();

        params.databaseType = _promptDatabaseType(scanner);
        params.path = _promptDirectory(scanner);
        params.sourceFileName = _promptSqlFile(
            scanner, "Source file name (clean bundle reference)",
            params.path);
        params.targetFileName = _promptSqlFile(
            scanner, "Target file name (customer schema to fix)",
            params.path);
        params.newFileName = _promptOutputFileName(
            scanner, "fixed_schema.sql");
        params.indexesName = _promptIndexesToSkip(scanner);

        _printSummary(params);

        if (!_promptConfirm(
            scanner, "Run conversion?")) {

            return null;
        }

        return params;
    }

    private static boolean _isInteractiveInvocation(String[] args) {
        if (args.length == 0) {
            return true;
        }

        if (args.length == 1) {
            String arg = args[0];

            return arg.equals("-i") || arg.equals("--interactive");
        }

        return false;
    }

    private static void _printSummary(Params params) {
        System.out.println();
        System.out.println("--- Summary ---");
        System.out.println("Database type: " + params.databaseType);
        System.out.println("Path:          " + params.path);
        System.out.println("Source file:   " + params.sourceFileName);
        System.out.println("Target file:   " + params.targetFileName);
        System.out.println("Output file:   " + params.newFileName);
        System.out.println(
            "Skip indexes:  " +
                (params.indexesName.isEmpty() ?
                    "(none)" :
                    String.join(", ", params.indexesName)));
        System.out.println();
    }

    private static boolean _promptConfirm(
        Scanner scanner, String question) {

        System.out.print(
            question +
            " [Press Enter to confirm, anything else to cancel]");

        return scanner.nextLine().trim().isEmpty();
    }

    private static String _promptDatabaseType(Scanner scanner) {
        while (true) {
            System.out.print("Database type [mysql/postgresql]: ");

            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("mysql") || input.equals("postgresql")) {
                return input;
            }

            Print.error(
                "Invalid database type. Please choose 'mysql' or " +
                "'postgresql'.");
        }
    }

    private static String _promptDirectory(Scanner scanner) {
        while (true) {
            System.out.print("Path to dump files (absolute): ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                Print.error("Path cannot be empty.");

                continue;
            }

            File dir = new File(input);

            if (!dir.exists() || !dir.isDirectory()) {
                Print.error("Directory not found: " + input);

                continue;
            }

            if (!input.endsWith(File.separator) && !input.endsWith("/")) {
                input = input + File.separator;
            }

            return input;
        }
    }

    private static List<String> _promptIndexesToSkip(Scanner scanner) {
        System.out.print(
            "Indexes to skip, comma-separated (leave empty to skip): ");

        String input = scanner.nextLine().trim();

        List<String> indexes = new ArrayList<>();

        if (input.isEmpty()) {
            return indexes;
        }

        for (String name : input.split(",")) {
            String trimmed = name.trim();

            if (!trimmed.isEmpty()) {
                indexes.add(trimmed);
            }
        }

        return indexes;
    }

    private static String _promptOutputFileName(
        Scanner scanner, String defaultName) {

        System.out.print(
            "Output file name [" + defaultName + "]: ");

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return defaultName;
        }

        if (!input.endsWith(".sql")) {
            input = input + ".sql";
        }

        return input;
    }

    private static String _promptSqlFile(
        Scanner scanner, String label, String path) {

        while (true) {
            System.out.print(label + ": ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                Print.error("File name cannot be empty.");

                continue;
            }

            if (!input.endsWith(".sql")) {
                Print.error("File must have .sql extension.");

                continue;
            }

            File file = new File(path + input);

            if (!file.exists() || !file.isFile()) {
                Print.error("File not found: " + path + input);

                continue;
            }

            return input;
        }
    }

    private static class Params {
        public String databaseType;

        public String path;

        public String newFileName;

        public String sourceFileName;

        public String targetFileName;

        public List<String> indexesName = new ArrayList<>();
    }

}
