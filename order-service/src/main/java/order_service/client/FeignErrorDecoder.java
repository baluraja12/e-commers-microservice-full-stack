package order_service.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign call failed. Method: {}, Status: {}, Reason: {}", 
                methodKey, response.status(), response.reason());
        
        switch (response.status()) {
            case 404:
                return new RuntimeException("Resource not found. Method: " + methodKey);
            case 500:
                return new RuntimeException("Internal Server Error in downstream service. Method: " + methodKey);
            default:
                return new RuntimeException("Generic error. Status: " + response.status());
        }
    }
}
