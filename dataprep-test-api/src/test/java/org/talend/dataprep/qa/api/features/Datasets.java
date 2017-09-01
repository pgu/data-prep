package org.talend.dataprep.qa.api.features;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Component
public class Datasets extends TalendFeature {

    public String uploadDataset(String filename, String name) throws IOException {
        System.out.println(this.getClass().getName() + ".uploadDataset(...)");
        String datasetName = name + System.currentTimeMillis();
        InputStream toto = Datasets.class.getResourceAsStream(filename);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(toto))) {
            System.out.println("Buffer = " + buffer);
            System.out.println(filename + " = ");
            System.out.println(buffer.lines().collect(Collectors.joining("\n")));
        }

//        Response response =
//                given().
//                        header(new Header("Content-Type", "text/plain")).
//                        body(Datasets.class.getResourceAsStream(filename)).
//                        when().
//                        post("http://10.42.10.99:9999/api/datasets?name=" + datasetName);
//        response.then().
//                statusCode(200);
//
//        String id = IOUtils.toString(response.getBody().asInputStream(), true);
//        return id;
        return null;
    }
}
