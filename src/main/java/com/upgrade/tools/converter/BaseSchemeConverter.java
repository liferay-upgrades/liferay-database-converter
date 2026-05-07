package com.upgrade.tools.converter;

import com.upgrade.tools.exception.ConverterException;
import com.upgrade.tools.util.Print;
import com.upgrade.tools.util.ResultsThreadLocal;
import com.upgrade.tools.util.SchemeConverterUtil;
import com.upgrade.tools.util.TransformUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public abstract class BaseSchemeConverter
    implements SchemeConverter {

    protected abstract Pattern getContextPattern();

    protected abstract String getDatabaseType();

    protected String beforeProcess(
            String targetResult, String sourceStatement,
            List<String> indexesName)
        throws ConverterException {

        Objects.requireNonNull(targetResult);

        return targetResult;
    }

    protected String postProcess(
            String targetResult, String sourceContent,
            List<String> indexesName)
        throws ConverterException {

        Objects.requireNonNull(targetResult);
        Objects.requireNonNull(sourceContent);

        return targetResult;
    }

    @Override
    public void converter(
            String path, String sourceName, String targetName, String newName,
            List<String> indexNames)
        throws ConverterException {

        Print.info(
            "Starting scheme converter to %s database".formatted(
                getDatabaseType()));

        Map<String, String> contentsMap = _readFiles(
            path, sourceName, targetName);

        String sourceContent = contentsMap.get("source.content");
        String targetContent = contentsMap.get("target.content");

        String converted = _converterContextPattern(
            sourceContent, targetContent, getContextPattern(), indexNames);

        String processed = postProcess(converted, sourceContent, indexNames);

        try {
            _writerResult(processed, path, newName);
        }
        catch (IOException ioException) {
            throw new ConverterException(
                "Unable to write SQL output file at " + path + newName,
                ioException);
        }
    }

    private String _concat(String value, int index, int size) {
        if (index == size) {
            return value;
        }
        else {
            return value + ",";
        }
    }

    private String _converterContextPattern(
            String sourceContent, String targetContent, Pattern pattern,
            List<String> keys)
        throws ConverterException {

        Matcher matcherTarget = pattern.matcher(targetContent);
        StringBuilder sb = new StringBuilder();

        while (matcherTarget.find()) {
            Matcher matcherSource = pattern.matcher(sourceContent);

            while (matcherSource.find()) {
                String tableNameSource = matcherSource.group(1);
                String tableNameTarget = matcherTarget.group(1);

                if (tableNameSource.equalsIgnoreCase(tableNameTarget)) {
                    Print.info(String.format(
                        "Converting %s table", tableNameSource));

                    String columnsSource = matcherSource.group(2);
                    String columnsTarget = matcherTarget.group(2);

                    String convertedColumns = _getConvertedColumns(
                        columnsSource, columnsTarget);

                    convertedColumns = beforeProcess(
                        convertedColumns, columnsSource, keys);

                    matcherTarget.appendReplacement(
                        sb,
                        Matcher.quoteReplacement(
                            matcherTarget.group(0)
                                .replace(columnsTarget, convertedColumns)
                        ));

                    Print.replacement(
                        columnsTarget, convertedColumns, pattern);

                    break;
                }
            }
        }

        matcherTarget.appendTail(sb);

        return sb.toString();
    }

    private String _extractNormalizedColumnName(String column) {
        Matcher matcher = _COLUMN_NAME_PATTERN.matcher(column);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1)
            .replace("\"", "")
            .replace("`", "")
            .toLowerCase();
    }

    private String _formatColumns(Set<String> newColumns, String columnsTarget) {
        Pattern pattern = Pattern.compile(
            "(`?\"?[a-zA-Z0-9_.-]+_?\"?`?)\\s(\\w+\\(?.+),?");

        Matcher matcherTarget = pattern.matcher(columnsTarget);

        int index = 0;

        StringBuilder sb = new StringBuilder();

        while (matcherTarget.find()) {
            for (String column : newColumns) {
                Matcher matcherColumn = _COLUMN_NAME_PATTERN.matcher(
                    column);

                while (matcherColumn.find()) {
                    String normalizeTargetColumn =
                        matcherTarget.group(1).replaceAll(
                            "\"", "");
                    String normalizeNewColumn =
                        matcherColumn.group(1).replaceAll(
                            "\"", "");

                    if (normalizeTargetColumn.equalsIgnoreCase(
                        normalizeNewColumn)) {

                        index++;

                        matcherTarget.appendReplacement(
                            sb, Matcher.quoteReplacement(
                                _concat(
                                    column, index, newColumns.size())));
                    }
                }
            }
        }

        matcherTarget.appendTail(sb);

        return sb.toString();
    }

    private String _getConvertedColumns(
        String sourceColumns, String targetColumns) {

        return _formatColumns(
            _newColumnsResults(sourceColumns, targetColumns),
            targetColumns);
    }

    private Set<String> _getColumnsSet(String columnContent) {
        Set<String> columns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (String column : columnContent.split(",\\n")) {
            String upperColumn = column.trim().toUpperCase();

            if (upperColumn.matches(
                "^(PRIMARY|UNIQUE|FOREIGN|KEY|CONSTRAINT)\\b.*")) {
                continue;
            }

            Matcher matcher = _COLUMN_NAME_PATTERN.matcher(column);

            if (matcher.find()) {
                columns.add(column.trim());
            }
        }

        return columns;
    }

    private Set<String> _newColumnsResults(
        String sourceColumns, String targetColumns) {

        Set<String> sourceColumnsSet = _getColumnsSet(sourceColumns);
        Set<String> targetColumnsSet = _getColumnsSet(targetColumns);

        Set<String> sourceColumnNames =
            TransformUtil.transformToSet(
                sourceColumnsSet,
                this::_extractNormalizedColumnName);

        Set<String> newColumns = new HashSet<>(sourceColumnsSet);

        newColumns.addAll(
            TransformUtil.transformToSet(
                targetColumnsSet,
                (column) -> {
                    String name = _extractNormalizedColumnName(
                        column);

                    if (name == null) {
                        return null;
                    }

                    if (sourceColumnNames.contains(name)) {
                        return null;
                    }

                    return column;
                }));

        return newColumns;
    }

    private Map<String, String> _readFiles(
            String path, String sourceName, String targetName)
        throws ConverterException {

        if (!sourceName.endsWith(_VALID_EXTENSION) ||
            !targetName.endsWith(_VALID_EXTENSION)) {

            throw new ConverterException(
                "File extension must end with " + _VALID_EXTENSION +
                    " (got source=" + sourceName +
                    ", target=" + targetName + ")");
        }

        Map<String, String> contentMap = new HashMap<>();

        contentMap.put(
            "source.content", _readSqlFile(path + sourceName, "source"));
        contentMap.put(
            "target.content", _readSqlFile(path + targetName, "target"));

        return contentMap;
    }

    private String _readSqlFile(String fullPath, String role)
        throws ConverterException {

        File file = new File(fullPath);

        if (!file.exists() || !file.isFile()) {
            throw new ConverterException(
                "The " + role + " file does not exist: " + fullPath);
        }

        if (file.length() == 0) {
            throw new ConverterException(
                "The " + role + " file is empty: " + fullPath);
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            return SchemeConverterUtil.readContent(inputStream);
        }
        catch (IOException ioException) {
            throw new ConverterException(
                "Unable to read " + role + " file: " + fullPath,
                ioException);
        }
    }

    private void _writerResult(
            String content, String path, String newName)
        throws IOException {

        File file = new File(path + newName);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, StandardCharsets.UTF_8))) {

            writer.write(content);

            ResultsThreadLocal.setResultsThreadLocal(true);
        }
    }

    private static final Pattern _COLUMN_NAME_PATTERN = Pattern.compile(
        "(`?\"?[a-zA-Z0-9_.-]+_?\"?`?)\\s+[^,]+(?:,|$)");

    private static final String _VALID_EXTENSION = ".sql";

}
