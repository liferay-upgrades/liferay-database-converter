package com.upgrade.tools.executor;

import com.upgrade.tools.converter.SchemeConverter;
import com.upgrade.tools.initialize.SchemeConverterInitialize;
import com.upgrade.tools.util.Print;
import com.upgrade.tools.util.ResultsThreadLocal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Albert Gomes Cabral
 */
public class SchemeConverterExecutor {

    public static void executor(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--help")) {
            Print.info(_helper());

            return;
        }

        Params params = _getParams(args);

        if (params == null) {
            throw new RuntimeException(
                "Is mandatory to inform valid arguments to execute it." +
                    "\nType --help to see the usage");
        }

        long start = System.nanoTime();

        SchemeConverter schemeConverter =
            SchemeConverterInitialize.getConverterType(
                params.databaseType);

        schemeConverter.converter(
            params.path, params.sourceFileName, params.targetFileName,
            params.newFileName, params.indexesName);

        if (ResultsThreadLocal.getResultsThreadLocal()) {
            Print.info(
                ("Converted with success. " +
                    "Completed in %d seconds").formatted(
                        System.nanoTime() - start / 1_000_000_000));
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

    private static class Params {
        public String databaseType;

        public String path;

        public String newFileName;

        public String sourceFileName;

        public String targetFileName;

        public List<String> indexesName = new ArrayList<>();
    }

}
