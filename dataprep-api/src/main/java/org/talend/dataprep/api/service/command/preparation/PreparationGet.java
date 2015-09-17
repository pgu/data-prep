package org.talend.dataprep.api.service.command.preparation;

import static org.talend.dataprep.api.service.command.common.GenericCommand.Defaults.emptyStream;
import static org.talend.dataprep.api.service.command.common.GenericCommand.Defaults.pipeStream;

import java.io.InputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.APIErrorCodes;
import org.talend.dataprep.api.service.APIService;
import org.talend.dataprep.api.service.command.common.GenericCommand;
import org.talend.dataprep.exception.TDPException;

@Component
@Scope("request")
public class PreparationGet extends GenericCommand<InputStream> {

    private PreparationGet(HttpClient client, String id) {
        super(APIService.PREPARATION_GROUP, client);
        execute(() -> new HttpGet(preparationServiceUrl + "/preparations/" + id));
        onError(e -> new TDPException(APIErrorCodes.UNABLE_TO_RETRIEVE_PREPARATION_LIST, e));
        on(HttpStatus.NO_CONTENT).then(emptyStream());
        on(HttpStatus.ACCEPTED).then(emptyStream());
        on(HttpStatus.OK).then(pipeStream());
    }

}
