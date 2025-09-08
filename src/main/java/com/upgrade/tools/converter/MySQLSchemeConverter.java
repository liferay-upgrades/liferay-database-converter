package com.upgrade.tools.converter;

import com.upgrade.tools.constants.SchemeConverterSupportType;

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
        List<String> contents, String sourceContent, List<String> indexesName) {

        List<String> result = new ArrayList<>(contents.size());

        for (String content : contents) {
            String statement = _replaceStatements(content, sourceContent);

            result.add(statement);
        }

        return result;
    }

    private String _processReplacement(String statement, Pattern pattern, String sourceStatement) {
        Matcher matcher = pattern.matcher(statement);

        while (matcher.find()) {
            Matcher tableStatementMatcher = _TABLE_NAME_PATTERN.matcher(
                sourceStatement);

            while (tableStatementMatcher.find()) {
                String tableName = tableStatementMatcher.group(1).replace(
                        "`", "");

                if (tableName.equalsIgnoreCase(matcher.group(1))) {
                    statement = statement.replace(matcher.group(1), tableName);
                }
            }
        }

        return statement;
    }

    private String _replaceStatements(String statement, String sourceStatement) {
        statement = _processReplacement(
            statement,
            Pattern.compile("INSERT\\s+INTO\\s+`?([^`\\s]+)`?\\s+VALUES"),
            sourceStatement);

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
            sb.append(columns.replace("\n)", ","));
            sb.append("\n");
            sb.append("  ");

            String keys = keysName.isEmpty() ? matcher.group() : _scapeKeys(
                matcher.group(), keysName);

            sb.append(keys);
        }

        return sb.toString();
    }

    private String _scapeKeys(String constraints, List<String> keys) {
        StringBuilder sb = new StringBuilder();

        String[] constraintArray = constraints.split(",\\n");

        for (int i = 0; i < constraintArray.length; i++) {
            String constraint = constraintArray[i].trim();

            boolean skippedKey = keys.stream().anyMatch(constraint::contains);

            if ((constraint.startsWith("KEY") || constraint.startsWith("UNIQUE KEY"))) {
                if (skippedKey) {
                    continue;
                }
            }

            if (constraint.startsWith("PRIMARY KEY") || !skippedKey) {
                sb.append(constraint);
            }

            if (i < constraintArray.length - 1) {
                sb.append(",\n  ");
            }
        }

        return sb.toString();
    }

    private final Pattern _TABLE_NAME_PATTERN = Pattern.compile(
        "CREATE\\s+TABLE\\s+(`[^`]+`)\\s*\\(([\\s\\S]*?\\)\\s*)(?=ENGINE|;)");


}
