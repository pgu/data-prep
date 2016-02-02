package org.talend.dataprep.schema.csv;

import static org.junit.Assert.assertEquals;
import static org.talend.dataprep.schema.csv.CSVFormatGuess.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.schema.AbstractSchemaTestUtils;

/**
 * Unit test for CSVFormatUtils.
 * 
 * @see CSVFormatUtils
 */

public class CSVFormatUtilsTest extends AbstractSchemaTestUtils {

    /** The component to test. */
    @Autowired
    private CSVFormatUtils csvFormatUtils;

    @Test
    public void shouldUseNewSeparator() {

        final DataSetMetadata updated = metadataBuilder.metadata() //
                .id("updated") //
                .parameter(HEADER_COLUMNS_PARAMETER,
                        "[\"nickname|secret\",\"firstname|secret\",\"lastname|date\",\"of\",\"birth|city\"]")
                .parameter(SEPARATOR_PARAMETER, "|") // new separator
                .parameter(HEADER_NB_LINES_PARAMETER, "12").build();

        // when
        csvFormatUtils.useNewSeparator(updated);

        // then
        final Map<String, String> parameters = updated.getContent().getParameters();
        assertEquals("[]", parameters.get(HEADER_COLUMNS_PARAMETER));
        assertEquals("|", parameters.get(SEPARATOR_PARAMETER));
        assertEquals("12", parameters.get(HEADER_NB_LINES_PARAMETER));
    }
}