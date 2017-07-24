//  ============================================================================
//
//  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.format;

import static org.talend.dataprep.transformation.format.JsonFormat.JSON;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.transformation.pipeline.node.TransformerWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

@Scope("prototype")
@Component("writer#" + JSON)
public class JsonWriter implements TransformerWriter {

    /** The data-prep ready jackson module. */
    @Autowired
    private transient ObjectMapper mapper;

    /** Jackson generator. */
    private JsonGenerator generator;

    public JsonWriter() {
    }

    /**
     * <b>Needed</b> private constructor for the WriterRegistrationService.
     *
     * @param params ignored parameters.
     */
    public JsonWriter(final Map<String, String> params) {
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void setOutput(OutputStream output) {
        try {
            this.generator = mapper.getFactory().createGenerator(output);
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    @Override
    public void write(final RowMetadata rowMetadata) throws IOException {
        startArray();
        rowMetadata.getColumns().forEach(col -> {
            try {
                generator.writeObject(col);
            } catch (IOException e) {
                throw new TDPException(CommonErrorCodes.UNABLE_TO_WRITE_JSON, e);
            }
        });
        endArray();
    }

    @Override
    public void write(final DataSetRow row) throws IOException {
        generator.writeObject(row.valuesWithId());
    }

    @Override
    public void startArray() throws IOException {
        generator.writeStartArray();
    }

    @Override
    public void endArray() throws IOException {
        generator.writeEndArray();
    }

    @Override
    public void startObject() throws IOException {
        generator.writeStartObject();
    }

    @Override
    public void endObject() throws IOException {
        generator.writeEndObject();
    }

    @Override
    public void fieldName(String name) throws IOException {
        generator.writeFieldName(name);
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }

    @Override
    public String toString() {
        return "JsonWriter";
    }
}
