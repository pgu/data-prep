package org.talend.dataprep.transformation.pipeline.runtime;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.AvroUtils;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.actions.DataSetRowAction;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.actions.context.ActionContext;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.ActionNode;

public class IntergrationTest {

    @Test
    public void testIntegration() {
        final Node node = NodeBuilder
                .source() //
                .to(new ActionNode(new RunnableAction(new MyCustomAction()))) //
                .build();

        final PipelineOptions options = PipelineOptionsFactory.create();
        final Pipeline pipeline = Pipeline.create(options);
        final Schema schema = Schema.createRecord( //
                "MyIntegrationTest", //
                "a dataprep preparation", //
                "org.talend.dataprep", //
                false //
        );
        schema.setFields(Collections.singletonList(new Schema.Field("col0",
                SchemaBuilder.builder().unionOf().nullBuilder().endNull().and().stringType().endUnion(),
                StringUtils.EMPTY, null)));

        final IndexedRecord record = new GenericData.Record(schema);
        final Create.Values<IndexedRecord> root = Create.of(record).withCoder(AvroCoder.of(IndexedRecord.class, schema));
        final PCollection<IndexedRecord> main = pipeline.apply(root);

        final BeamRuntime beamRuntime = new BeamRuntime(main);
        node.accept(beamRuntime);

        final PCollection<KV<IndexedRecord, AvroUtils.Metadata>> collection2 = beamRuntime.getResult().apply(ParDo.of(new Printer()));

        pipeline.run().waitUntilFinish();

    }

    private static class MyCustomAction implements DataSetRowAction, Serializable {

        @Override
        public Collection<DataSetRow> apply(DataSetRow dataSetRow, ActionContext actionContext) {
            return Collections.singletonList(dataSetRow.set("col0", "Test"));
        }
    }

    private static class Printer extends DoFn<KV<IndexedRecord, AvroUtils.Metadata>, KV<IndexedRecord, AvroUtils.Metadata>> implements Serializable {

        @ProcessElement
        public void process(ProcessContext c) {
            System.out.println(c.element().getKey());
        }
    }
}
