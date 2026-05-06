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

    @Override
    protected Pattern getContextPattern() {
        return Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:public\\.)?([a-zA-Z_0-9]+)\\s*" +
                    "\\(([^)]*?(\\([^)]*\\)[^)]*?)*)\\);",
            Pattern.DOTALL);
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
            return _postProcess(
                targetResult, sourceContent, indexesName);
        }
        catch (Exception exception) {
            throw new ConverterException(exception);
        }
    }

    private String _postProcess(
        String targetStatement, String sourceStatement,
        List<String> indexesName) {

         return _addIndexesRulesAndAlterTable(
            targetStatement, sourceStatement, indexesName);
    }

    private String _addIndexesRulesAndAlterTable(
        String targetResult, String sourceStatement, List<String> indexesName) {

        Pattern alterTableOnly = Pattern.compile(
            "ALTER TABLE ONLY\\s+public\\.(?!\\w+_x_\\d+)\\w+\\s+" +
                "ADD CONSTRAINT\\s+.*?PRIMARY KEY\\s*\\(.*?\\);\n");

        Matcher alterTableOnlyMatcher = alterTableOnly.matcher(sourceStatement);

        String delimiter = "--\n" + "-- PostgreSQL database dump complete";

        while (alterTableOnlyMatcher.find()) {
            targetResult = targetResult.replace(
                delimiter, alterTableOnlyMatcher.group() + "\n" + delimiter
            );
        }

        Pattern indexesPattern = Pattern.compile(
            "CREATE\\s+INDEX\\s+(\\w+)\\s+ON\\s+public\\.(\\w+.*);");

        Matcher indexesMatcher = indexesPattern.matcher(sourceStatement);

        while (indexesMatcher.find()) {
            targetResult = targetResult.replace(
                delimiter, indexesMatcher.group() + "\n\n" + delimiter
            );
        }

        Pattern uniqueIndexesPattern = Pattern.compile(
        "CREATE\\s+UNIQUE\\s+INDEX\\s+(\\w+)\\s+ON" +
                "\\s+public\\.(\\w+.*);");

        Matcher uniqueIndexesMatcher =
            uniqueIndexesPattern.matcher(sourceStatement);

        while (uniqueIndexesMatcher.find()) {

            if (!indexesName.isEmpty()){
                String sourceUniqueIndexMatcher = uniqueIndexesMatcher.group(1);

                if (indexesName.contains(sourceUniqueIndexMatcher)) {
                    continue;
                }
            }

            targetResult = targetResult.replace(
                delimiter, uniqueIndexesMatcher.group() + "\n\n" + delimiter
            );
        }

        Pattern createRulesPattern = Pattern.compile(
            "CREATE\\s+RULE\\s+[\\w\\s]+ AS[\\s\\S]*?WHERE\\s*\\([^;]*\\);");

        Matcher createRulesMatcher = createRulesPattern.matcher(sourceStatement);

        while (createRulesMatcher.find()) {
            targetResult = targetResult.replace(
                delimiter, createRulesMatcher.group() + "\n\n" + delimiter
            );
        }

        return targetResult;
    }

}
