package de.rwth.idsg.steve.handler.ocpp12;

import de.rwth.idsg.steve.handler.AbstractOcppResponseHandler;
import de.rwth.idsg.steve.web.dto.task.RequestTask;
import ocpp.cp._2010._08.ClearCacheRequest;
import ocpp.cp._2010._08.ClearCacheResponse;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 30.12.2014
 */
public class ClearCacheResponseHandler extends AbstractOcppResponseHandler<ClearCacheRequest, ClearCacheResponse> {

    public ClearCacheResponseHandler(RequestTask<ClearCacheRequest> task, String chargeBoxId) {
        super(task, chargeBoxId);
    }

    @Override
    public void handleResult(ClearCacheResponse response) {
        requestTask.addNewResponse(chargeBoxId, response.getStatus().value());
    }
}
