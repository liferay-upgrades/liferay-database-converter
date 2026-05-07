package com.upgrade.tools.converter;

import com.upgrade.tools.constants.SchemeConverterSupportType;
import com.upgrade.tools.exception.ConverterException;
import com.upgrade.tools.util.TransformUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class MySQLSchemeConverter extends BaseSchemeConverter {

    @Override
    protected String beforeProcess(
            String targetResult, String sourceStatement,
            List<String> parameters)
        throws ConverterException {

        try {
            return _replacementConstraints(
                targetResult, sourceStatement, parameters);
        }
        catch (Exception exception) {
            throw new ConverterException(
                "MySQL constraint replacement failed", exception);
        }
    }

    @Override
    protected Pattern getContextPattern() {
        return _TABLE_NAME_PATTERN;
    }

    @Override
    protected String getDatabaseType() {
        return SchemeConverterSupportType.MYSQL;
    }

    @Override
    protected String postProcess(
            String targetResult, String sourceContent,
            List<String> indexesName)
        throws ConverterException {

        try {
            return _replaceStatements(targetResult, sourceContent);
        }
        catch (Exception exception) {
            throw new ConverterException(
                "MySQL post-processing failed", exception);
        }
    }

    private String _processReplacement(
        String statement, Pattern pattern, String sourceStatement) {

        Matcher matcher = pattern.matcher(statement);

        while (matcher.find()) {
            Matcher tableStatementMatcher = _TABLE_NAME_PATTERN.matcher(
                sourceStatement);

            while (tableStatementMatcher.find()) {
                String tableNameNormalized =
                    tableStatementMatcher.group(1).replace("`", "");

                if (tableNameNormalized.equalsIgnoreCase(matcher.group(1))) {
                    statement = statement.replace(
                        matcher.group(1), tableNameNormalized);
                }
            }
        }

        return statement;
    }

    private String _replaceStatements(String statement, String sourceStatement) {
        statement = _processReplacement(
            statement, _DROP_TABLE_PATTERN, sourceStatement);

        statement = _processReplacement(
            statement, _LOCK_TABLES_PATTERN, sourceStatement);

        return _processReplacement(
            statement, _ALTER_TABLE_PATTERN, sourceStatement);
    }

    private String _replacementConstraints(
        String columns, String constraints, List<String> keysName) {

        Matcher matcher = _PRIMARY_KEY_PATTERN.matcher(constraints);

        StringBuilder sb = new StringBuilder();

        if (matcher.find()) {
            String trimmedColumns = columns.replaceAll(",?\\s*\\)\\s*$", "");

            sb.append(trimmedColumns);
            sb.append(",\n  ");

            String keys = (keysName.isEmpty()) ?
                matcher.group() :
                _scapeKeys(matcher.group(), keysName);

            sb.append(keys);
        }

        return sb.toString();
    }

    private String _scapeKeys(String constraints, List<String> keys) {
        String[] constraintArray = constraints.split(",\\n");

        List<String> kept = new ArrayList<>(constraintArray.length);

        for (String raw : constraintArray) {
            String constraint = raw.trim();

            boolean skippedKey = TransformUtil.anyMatch(
                keys, constraint::contains);

            boolean isIndex =
                constraint.startsWith("KEY") ||
                    constraint.startsWith("UNIQUE KEY");

            if (isIndex && skippedKey) {
                continue;
            }

            kept.add(constraint);
        }

        return String.join(",\n  ", kept);
    }

    private static final Pattern _ALTER_TABLE_PATTERN = Pattern.compile(
        "ALTER\\s+TABLE\\s+`?([^`\\s]+)`?");

    private static final Pattern _DROP_TABLE_PATTERN = Pattern.compile(
        "DROP\\s+TABLE\\s+IF\\s+EXISTS\\s+`?([^`\\s]+)`?;");

    private static final Pattern _LOCK_TABLES_PATTERN = Pattern.compile(
        "LOCK\\s+TABLES\\s+`?([^`\\s]+)`?\\s+WRITE;");

    private static final Pattern _PRIMARY_KEY_PATTERN = Pattern.compile(
        "PRIMARY\\s+KEY\\s+(.+)(\\s*.*)+");

    private static final Pattern _TABLE_NAME_PATTERN = Pattern.compile(
        "CREATE\\s+TABLE\\s+(`[^`]+`)\\s*\\(([\\s\\S]*?\\)\\s*)(?=ENGINE|;)");

}
