package org.talend.dataprep.helper.api;

/**
 * {@link Enum} representation of {@link Action} parameters types.
 */
public enum ActionParamEnum {
    FROM_PATTERN_MODE,
    NEW_PATTERN,
    SCOPE,
    COLUMN_NAME,
    COLUMN_ID,
    ROW_ID,
    LIMIT,
    SEPARATOR,
    MANUAL_SEPARATOR_STRING,
    MANUAL_SEPARATOR_REGEX,
    FILTER,
    REGION_CODE,
    MODE,
    FORMAT_TYPE,
    OPERATOR,
    OPERAND,
    CREATE_NEW_COLUMN;

    public String getName() {
        return this.name().toLowerCase();
    }
}
