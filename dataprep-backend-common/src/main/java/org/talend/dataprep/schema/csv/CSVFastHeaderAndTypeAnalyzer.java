package org.talend.dataprep.schema.csv;

import java.util.*;

import org.talend.dataprep.api.type.Type;

/**
 * This class performs header and type analysis on a sample of records (lines).
 * 
 * It detects whether the sample contains a header or not. If a header could not be detected one is generated. It also
 * performs baseline (very simple) type detection based upon the sample of records.
 */
public class CSVFastHeaderAndTypeAnalyzer {

    /**
     * Used to mark a field as absent in a record (line).
     */
    private static final int ABSENT = -4;

    /**
     * used to mark a field as a decimal in a record (line).
     */
    private static final int DECIMAL = -3;

    /**
     * Used to mark a field as an integer in a record (line).
     */
    private static final int INTEGER = -2;

    /**
     * Used to mark a field as a boolean in a record (line).
     */
    private static final int BOOLEAN = -1;

    /**
     * Used to mark a field as being empty in a record (line).
     */
    private static final int EMPTY = 0;

    /**
     * The sample of csv lines used to perform the analysis.
     */
    private final List<String> sampleLines;

    /**
     * The types of all fields of all records found in this sample. An enum could be used for type checking but we will
     * not be able to detect stable pattern.
     */
    private final List<Integer>[] sampleTypes;

    /**
     * The maximum number of fields of records in this sample. It is used as the default number of fields. Records which
     * do not reach <tt>maxFields</tt> are lengthen to reach it and absent fields are marked as so.
     */
    private int maxFields = 0;

    /**
     * The separator used for the current analysis.
     */
    private final Separator separator;

    /**
     * a flag which tells whether the analysis has been performed or not.
     */
    private boolean analysisPerformed = false;

    /**
     * A boolean specifying whether or not, according to the separator, the first line is a header or not.
     */
    private boolean firstLineAHeader = false;

    private boolean consistent = true;

    private Map<String, Type> headers = null;

    private final String DEFAULT_HEADER_PREFIX = "COL";

    /**
     * Constructor
     * 
     * @param sampleLines the lines used to perform the analysis
     * @param separator the specified character or string used as a separator
     */
    public CSVFastHeaderAndTypeAnalyzer(List<String> sampleLines, Separator separator) {
        this.sampleLines = sampleLines;
        this.separator = separator;
        sampleTypes = setFieldTypes();
    }

    /**
     * Performs type analysis for fields of each record.
     * 
     * @return an array of list of types (a list of types for each record)
     */
    private List<Integer>[] setFieldTypes() {
        List<Integer>[] result = new ArrayList[sampleLines.size()];
        for (int i = 0; i < sampleLines.size(); i++) {
            result[i] = setFieldType(i);
            if (result[i].size() > maxFields) {
                maxFields = result[i].size();
            }
        }
        // Make maxFields the number of fields (t for all lines of the sample
        for (List<Integer> list : result) {
            while (list.size() < maxFields) {
                list.add(ABSENT);
            }
        }
        return result;
    }

    /**
     * Performs type analysis for fields of the record at the specified index.
     * 
     * @param i the index of the record to be analyzed.
     * @return the list of types of the record
     */
    private List<Integer> setFieldType(int i) {
        ArrayList<Integer> result = new ArrayList<>();
        String s = (i < sampleLines.size()) ? sampleLines.get(i) : null;
        if (s == null) {
            return null;
        }
        Scanner scanner = new Scanner(s);
        scanner.useDelimiter(separator.getSeparator() + "");
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) {
                result.add(INTEGER);
                scanner.next();
            } else if (scanner.hasNextDouble()) {
                result.add(DECIMAL);
                scanner.next();
            } else if (scanner.hasNextBoolean()) {
                result.add(BOOLEAN);
                scanner.next();
            } else {
                String field = scanner.next();
                switch (field) {
                case "":
                    result.add(EMPTY);
                    break;
                default: // used to detect a stable length of a field (may be it is a date or a pattern)
                    result.add(field.length());
                }
            }
        }
        return result;
    }

    /**
     * Performs the column typing, i.e, look for the type of each column. A column has String type by default. If it has
     * more than half of considered number of records (<tt>end</tt> - <tt>start</tt> + 1) of the same type being numeric
     * or boolean then its type is set to numeric or boolean respectively. It starts at the specified index
     * <tt>start</tt> inclusive and stops at the specified index <tt>end</tt> also inclusive.
     * 
     * @param start the specified inclusive start index
     * @param end the specified exclusive end index
     * @return the list of types (type for each column)
     */
    private List<Type> columnTyping(int start, int end) {

        if (start > end) {
            throw new IllegalArgumentException("The end column " + end + " should be greater than the start column " + start);
        }

        if (sampleTypes.length <= start || sampleTypes.length <= end || start < 0 || end < 0) {
            throw new IllegalArgumentException("start or end are out of the columns' bound");
        }

        List<Type> result = new ArrayList<>();
        int size = (end - start) / 2;

        for (int i = 0; i < maxFields; i++) {
            final HashMap<Integer, Integer> typeCount = new HashMap<>();
            for (int j = start; j <= end; j++) {
                int currentType = sampleTypes[j].get(i);
                Integer oldValue = typeCount.get(currentType);
                typeCount.put(currentType, oldValue != null ? oldValue + 1 : 1);
            }
            Optional<Map.Entry<Integer, Integer>> optional = typeCount.entrySet().stream().filter(e -> e.getValue() > size)
                    .findFirst();

            try {
                Map.Entry<Integer, Integer> mostFrequentEntry = optional.get();
                switch (mostFrequentEntry.getKey()) {
                case BOOLEAN:
                    result.add(Type.BOOLEAN);
                    break;
                case INTEGER:
                    result.add(Type.INTEGER);
                    break;
                case DECIMAL:
                    result.add(Type.DOUBLE);
                    break;
                default:
                    result.add(Type.STRING);
                }
            } catch (NoSuchElementException exc) {
                result.add(Type.STRING);
            }
        }
        return result;
    }

    /**
     * Performs typing for the first record.
     * 
     * @return the list of types based on a first record analysis
     */
    private List<Type> firstRecordTyping() {
        return columnTyping(0, 0);
    }

    /**
     * Performs Column typing based on all records but the first one.
     * 
     * @return the list of types based on all records but the first one
     */
    private List<Type> columnTypingWithoutFirstRecord() {
        return columnTyping(1, sampleTypes.length - 1);
    }

    /**
     * Performs Column typing based on all records.
     * 
     * @return the list of types based on all records
     */
    private List<Type> allRecordsColumnTyping() {
        return columnTyping(0, sampleTypes.length - 1);
    }

    /**
     * Returns true if the list of specified types are all String (text) and false otherwise.
     * 
     * @param types the list of specified types
     * @return true if the list of specified types are all String (text) and false otherwise
     */
    private boolean allStringTypes(List<Type> types) {
        for (Type type : types) {
            // if not string
            if (!Type.STRING.equals(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs header and typing analysis.
     */
    public void analyze() {
        if (!analysisPerformed) {
            // Perform Header analysis if the sample has at least two lines
            if (sampleLines.size() > 1) {
                // check consistency: the first line must contains the separator
                if (!separator.countPerLine.containsKey(1)) {
                    consistent = false;
                } else {
                    final List<Type> firstRecordTypes = firstRecordTyping();
                    final List<Type> columnTypingWithoutFirstRecord = columnTypingWithoutFirstRecord();
                    // if the first line is all text and all fields are present and following lines have some columns
                    // which are at least 50% not text
                    // mark the separator as having a header
                    if (allStringTypes(firstRecordTypes) && !sampleTypes[0].contains(ABSENT)
                            && !Arrays.equals(firstRecordTypes.toArray(), columnTypingWithoutFirstRecord.toArray())) {
                        firstLineAHeader = true;
                    }
                }
            }
            // type analysis: if there is a header the first line is excluded from type analysis, otherwise it is
            // included
            headers = new LinkedHashMap<>();
            if (firstLineAHeader) {
                List<Type> columnTypes = columnTypingWithoutFirstRecord();
                Scanner scanner = new Scanner(sampleLines.get(0));
                scanner.useDelimiter(separator.getSeparator() + "");
                int i = 0;
                while (scanner.hasNext()) {
                    String col = scanner.next();
                    headers.put(col, columnTypes.get(i++));
                }
            } else {
                List<Type> columnTypes = allRecordsColumnTyping();
                int i = 1;
                for (Type type : columnTypes) {
                    headers.put(DEFAULT_HEADER_PREFIX + (i++), type);
                }
            }

        }
        analysisPerformed = true;
    }

    /**
     * Returns <tt>true</tt> if the first line is a header according to the used separator and false otherwise.
     *
     * @return <tt>true</tt> if the first line is a header according to the used separator and false otherwise
     */
    public boolean isFirstLineAHeader() {
        if (!analysisPerformed) {
            analyze();
        }
        return firstLineAHeader;
    }

    /**
     * Returns a map associating to each column of the header its type.
     *
     * @return a map associating to each column of the header its type
     */
    public Map<String, Type> getHeaders() {
        if (!analysisPerformed) {
            analyze();
        }
        return headers;
    }

    /**
     * Returns <tt>true</tt> if the first record contains the separator and <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if the first record contains the separator and <tt>false</tt> otherwise
     */
    public boolean isConsistent() {
        if (!analysisPerformed) {
            analyze();
        }
        return consistent;
    }
}