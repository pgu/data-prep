package org.talend.dataprep.transformation.api.action.dynamic.cluster;

import static org.talend.dataprep.api.type.Type.BOOLEAN;
import static org.talend.dataprep.api.type.Type.STRING;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.i18n.MessagesBundle;
import org.talend.dataprep.transformation.api.action.dynamic.DynamicParameters;
import org.talend.dataprep.transformation.api.action.parameters.ClusterItem;
import org.talend.dataprep.transformation.api.action.parameters.Clusters;
import org.talend.dataprep.transformation.api.action.parameters.GenericParameter;
import org.talend.dataprep.transformation.api.action.parameters.Parameter;
import org.talend.datascience.common.inference.Analyzer;
import org.talend.datascience.common.recordlinkage.StringClusters;
import org.talend.datascience.common.recordlinkage.StringsClusterAnalyzer;

/**
 * Cluster action dynamic parameter generator It takes an InputStream as argument, containing the dataset
 */
@Component
public class ClusterParameters implements DynamicParameters {

    @Override
    public GenericParameter getParameters(final String columnId, final DataSet content) {
        // Analyze clusters service
        Analyzer<StringClusters> clusterAnalyzer = new StringsClusterAnalyzer();
        clusterAnalyzer.init();
        content.getRecords().forEach(row -> {
            String value = row.get(columnId);
            clusterAnalyzer.analyze(value);
        });
        clusterAnalyzer.end();
        // Build results
        final Clusters.Builder builder = Clusters
                .builder()
                .title(MessagesBundle.getString("parameter.textclustering.title.1"))
                .title(MessagesBundle.getString("parameter.textclustering.title.2"));
        final StringClusters result = clusterAnalyzer.getResult().get(0);
        for (StringClusters.StringCluster cluster : result) {
            // String clustering may cluster null / empty values, however not interesting for data prep.
            if (!StringUtils.isEmpty(cluster.survivedValue)) {
                final ClusterItem.Builder currentCluster = ClusterItem.builder();
                for (String value : cluster.originalValues) {
                    currentCluster.parameter(new Parameter(value, BOOLEAN.getName()));
                }
                currentCluster.replace(new Parameter("replaceValue", STRING.getName(), cluster.survivedValue));
                builder.cluster(currentCluster);
            }
        }
        return new GenericParameter("cluster", builder.build());
    }
}
