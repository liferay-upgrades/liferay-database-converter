package com.upgrade.tools.converter;

import com.upgrade.tools.constants.SchemeConverterSupportType;
import com.upgrade.tools.exception.ConverterException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class PostGreSQLSchemeConverter extends BaseSchemeConverter {

    /**
     * Marker that PostgreSQL's {@code pg_dump} writes at the very end of a
     * dump file. We splice ALTER TABLE / CREATE INDEX / CREATE RULE
     * statements from the source dump <strong>just before</strong> this
     * marker so the resulting file remains a valid pg_dump output.
     */
    static final String DUMP_COMPLETE_DELIMITER =
        "--\n-- PostgreSQL database dump complete";

    @Override
    protected Pattern getContextPattern() {
        return _CONTEXT_PATTERN;
    }

    @Override
    protected String getDatabaseType() {
        return SchemeConverterSupportType.POSTGRES;
    }

    @Override
    protected String postProcess(
            String targetResult, String sourceContent,
            List<String> indexesName)
        throws ConverterException {

        try {
            return _addIndexesRulesAndAlterTable(
                targetResult, sourceContent, indexesName);
        }
        catch (Exception exception) {
            throw new ConverterException(
                "PostgreSQL post-processing failed", exception);
        }
    }

    private String _addIndexesRulesAndAlterTable(
        String targetResult, String sourceStatement, List<String> indexesName) {

        targetResult = _spliceMatches(
            targetResult, _ALTER_TABLE_ONLY_PATTERN, sourceStatement, "\n");

        targetResult = _spliceMatches(
            targetResult, _INDEX_PATTERN, sourceStatement, "\n\n");

        Matcher uniqueIndexesMatcher = _UNIQUE_INDEX_PATTERN.matcher(
            sourceStatement);

        while (uniqueIndexesMatcher.find()) {
            if (!indexesName.isEmpty() &&
                indexesName.contains(uniqueIndexesMatcher.group(1))) {

                continue;
            }

            targetResult = targetResult.replace(
                DUMP_COMPLETE_DELIMITER,
                uniqueIndexesMatcher.group() + "\n\n" +
                    DUMP_COMPLETE_DELIMITER);
        }

        targetResult = _spliceMatches(
            targetResult, _CREATE_RULE_PATTERN, sourceStatement, "\n\n");

        return targetResult;
    }

    /**
     * Inserts every match of {@code pattern} found in {@code sourceStatement}
     * into {@code targetResult} immediately before the dump-complete marker,
     * separated by {@code separator}.
     */
    private String _spliceMatches(
        String targetResult, Pattern pattern, String sourceStatement,
        String separator) {

        Matcher matcher = pattern.matcher(sourceStatement);

        while (matcher.find()) {
            targetResult = targetResult.replace(
                DUMP_COMPLETE_DELIMITER,
                matcher.group() + separator + DUMP_COMPLETE_DELIMITER);
        }

        return targetResult;
    }

    private static final Pattern _ALTER_TABLE_ONLY_PATTERN = Pattern.compile(
        "ALTER TABLE ONLY\\s+public\\.(?!\\w+_x_\\d+)\\w+\\s+" +
            "ADD CONSTRAINT\\s+.*?PRIMARY KEY\\s*\\(.*?\\);\n");

    private static final Pattern _CONTEXT_PATTERN = Pattern.compile(
        "CREATE\\s+TABLE\\s+(?:public\\.)?([a-zA-Z_0-9]+)\\s*" +
            "\\(([^)]*?(\\([^)]*\\)[^)]*?)*)\\);",
        Pattern.DOTALL);

    private static final Pattern _CREATE_RULE_PATTERN = Pattern.compile(
        "CREATE\\s+RULE\\s+[\\w\\s]+ AS[\\s\\S]*?WHERE\\s*\\([^;]*\\);");

    private static final Pattern _INDEX_PATTERN = Pattern.compile(
        "CREATE\\s+INDEX\\s+(\\w+)\\s+ON\\s+public\\.(\\w+.*);");

    private static final Pattern _UNIQUE_INDEX_PATTERN = Pattern.compile(
        "CREATE\\s+UNIQUE\\s+INDEX\\s+(\\w+)\\s+ON\\s+public\\.(\\w+.*);");

}
