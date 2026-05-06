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
        String content, String sourceStatement, List<String> parameters) {

        return _replacementConstraints(content, sourceStatement, parameters);
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
    protected List<String> postProcess(
            List<String> targetResults, String sourceContent,
            List<String> indexesName)
        throws ConverterException {

        try {
            List<String> result = new ArrayList<>(targetResults.size());

            for (String targetResult : targetResults) {
                result.add(
                    _replaceStatements(
                        targetResult, sourceContent));
            }

            return result;
        }
        catch (Exception exception) {
            throw new ConverterException(exception);
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
                    tableStatementMatcher.group(1).replace(
                        "`", "");

                if (tableNameNormalized.equalsIgnoreCase(
                        matcher.group(1))) {

                    statement = statement.replace(
                        matcher.group(1), tableNameNormalized);
                }
            }
        }

        return statement;
    }

    private String _replaceStatements(String statement, String sourceStatement) {
        statement = _processReplacement(
            statement,
            Pattern.compile("DROP\\s+TABLE\\s+IF\\s+EXISTS\\s+`?([^`\\s]+)`?;"),
            sourceStatement);

        statement = _processReplacement(
            statement,
            Pattern.compile("LOCK\\s+TABLES\\s+`?([^`\\s]+)`?\\s+WRITE;"),
            sourceStatement);

        return _processReplacement(
             statement,
             Pattern.compile("ALTER\\s+TABLE\\s+`?([^`\\s]+)`?"),
             sourceStatement);
    }

    private String _replacementConstraints(
        String columns, String constraints, List<String> keysName) {

        Pattern pattern = Pattern.compile("PRIMARY\\s+KEY\\s+(.+)(\\s*.*)+");

        Matcher matcher = pattern.matcher(constraints);

        StringBuilder sb = new StringBuilder();

        if (matcher.find()) {
            String trimmedColumns = columns.replaceAll(
                ",?\\s*\\)\\s*$", "");

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

    private final Pattern _TABLE_NAME_PATTERN = Pattern.compile(
        "CREATE\\s+TABLE\\s+(`[^`]+`)\\s*\\(([\\s\\S]*?\\)\\s*)(?=ENGINE|;)");

}
