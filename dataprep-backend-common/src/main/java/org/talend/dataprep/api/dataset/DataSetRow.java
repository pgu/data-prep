package org.talend.dataprep.api.dataset;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.talend.dataprep.exception.CommonMessages;
import org.talend.dataprep.exception.Exceptions;

public class DataSetRow {

    private boolean deleted = false;

    private final Map<String, String> values = new HashMap<>();

    public DataSetRow() {
    }

    public DataSetRow(Map<String, String> values) {
        this.values.putAll(values);
    }

    public DataSetRow set(String name, String value) {
        values.put(name, value);
        return this;
    }

    public String get(String name) {
        return values.get(name);
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void writeTo(OutputStream stream) {
        if (isDeleted()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = values.entrySet().iterator();
        builder.append('{');
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            builder.append('\"').append(entry.getKey()).append('\"').append(':').append('\"').append(entry.getValue())
                    .append('\"');
            if (iterator.hasNext()) {
                builder.append(',');
            }
        }
        builder.append('}');
        try {
            stream.write(builder.toString().getBytes());
        } catch (IOException e) {
            throw Exceptions.User(CommonMessages.UNABLE_TO_SERIALIZE_TO_JSON, e);
        }
    }

    /**
     * Clear all values in this row and reset state as it was when created (e.g. {@link #isDeleted()} returns
     * <code>false</code>).
     */
    public void clear() {
        deleted = false;
        values.clear();
    }
}
