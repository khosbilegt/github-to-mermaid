package mn.khosbilegt.endpoint;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Provider
public class ExceptionMappers {
    private final Logger LOG = Logger.getLogger("PortfolioManager");

    @ServerExceptionMapper
    public RestResponse<JsonObject> mapException(Throwable throwable) {
        RestResponse.Status statusCode = RestResponse.Status.INTERNAL_SERVER_ERROR;
        if (throwable instanceof NotFoundException) {
            statusCode = RestResponse.Status.NOT_FOUND;
        }
        String message = throwable.getMessage();
        LOG.debugv(throwable, "Exception caught by Exception Mapper: {0}", throwable.getMessage());
        return RestResponse.status(statusCode, new JsonObject()
                .put("status", "FAILED")
                .put("exception", throwable.getClass().getName())
                .put("message", message)
        );
    }
}