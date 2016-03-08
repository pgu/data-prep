// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.schema.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.schema.Serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

@Service("serializer#html")
public class HtmlSerializer implements Serializer {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlSerializer.class);

    @Resource(name = "serializer#html#executor")
    TaskExecutor executor;

    @Override
    public InputStream serialize(InputStream rawContent, DataSetMetadata metadata) {
        try {
            PipedInputStream pipe = new PipedInputStream();
            PipedOutputStream jsonOutput = new PipedOutputStream(pipe);

            Runnable r = doSerialize(rawContent, metadata, jsonOutput);
            executor.execute(r);
            return pipe;
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNABLE_TO_SERIALIZE_TO_JSON, e);
        }
    }

    public Runnable doSerialize(InputStream rawContent, DataSetMetadata dataSetMetadata, OutputStream jsonOutput) {

        return () -> {
            try {

                Map<String, String> parameters = dataSetMetadata.getContent().getParameters();
                String valuesSelector = parameters.get(HtmlFormatGuesser.VALUES_SELECTOR_KEY);
                ValuesContentHandler valuesContentHandler = new ValuesContentHandler(valuesSelector);

                HtmlParser htmlParser = new HtmlParser();
                Metadata metadata = new Metadata();

                htmlParser.parse( rawContent, valuesContentHandler, metadata, new ParseContext() );

                JsonGenerator generator = new JsonFactory().createGenerator(jsonOutput);
                generator.writeStartArray(); // start the record
                List<ColumnMetadata> columns = dataSetMetadata.getRowMetadata().getColumns();

                for (List<String> values : valuesContentHandler.getValues()) {

                    if (values.isEmpty()) {
                        // avoid empty record which can fail analysis
                        continue;
                    }

                    generator.writeStartObject();

                    int idx = 0;

                    for (String value : values) {
                        ColumnMetadata columnMetadata = columns.get(idx);
                        generator.writeFieldName(columnMetadata.getId());
                        String cellValue = value;
                        if (cellValue != null) {
                            generator.writeString(cellValue);
                        } else {
                            generator.writeNull();
                        }
                        idx++;
                    }
                    generator.writeEndObject();
                }

                generator.writeEndArray(); // end the record
                generator.flush();
            } catch (Exception e) {
                // Consumer may very well interrupt consumption of stream (in case of limit(n) use for sampling).
                // This is not an issue as consumer is allowed to partially consumes results, it's up to the
                // consumer to ensure data it consumed is consistent.
                LOGGER.debug("Unable to continue serialization for {}. Skipping remaining content.", dataSetMetadata.getId(), e);
            } finally {
                try {
                    jsonOutput.close();
                } catch (IOException e) {
                    LOGGER.error("Unable to close output", e);
                }
            }
        };

    }
}
