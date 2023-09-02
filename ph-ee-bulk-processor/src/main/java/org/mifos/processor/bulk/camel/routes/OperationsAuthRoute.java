package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OPS_APP_ACCESS_TOKEN;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class OperationsAuthRoute extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {

        from("rest:get:test/auth").to("direct:get-access-token");

        /**
         * Error handling route
         */
        from("direct:access-token-error").id("access-token-error").process(exchange -> {
            logger.error("Error while fetching Access Token from server: " + exchange.getIn().getBody());
        });

        /**
         * Save Access Token to AccessTokenStore
         */
        from("direct:access-token-save").id("access-token-save").unmarshal().json(JsonLibrary.Jackson, HashMap.class).process(exchange -> {
            // TODO: Figure out access token storage if required
            Map<String, Object> jsonObject = exchange.getIn().getBody(HashMap.class);
            exchange.setProperty(OPS_APP_ACCESS_TOKEN, jsonObject.get("access_token"));
            logger.debug("Saved Access Token: " + exchange.getProperty(OPS_APP_ACCESS_TOKEN, String.class));
            exchange.getIn().setBody(jsonObject.toString());
        });

        /**
         * Fetch Access Token from SLCB
         */
        getBaseExternalApiRequestRouteDefinition("access-token-fetch", HttpRequestMethod.POST)
                .setHeader(Exchange.REST_HTTP_QUERY,
                        simpleF("username=%s&password=%s&grant_type=%s", operationsAppConfig.username, operationsAppConfig.password,
                                "password"))
                .setHeader("Authorization", constant("Basic Y2xpZW50Og=="))
                .setHeader(HEADER_PLATFORM_TENANT_ID, simple("${exchangeProperty." + TENANT_ID + "}"))
                .toD(operationsAppConfig.authUrl + "?bridgeEndpoint=true").log(LoggingLevel.INFO, "Auth response: \n\n ${body}");

        /**
         * Access Token check validity and return value
         */
        from("direct:get-access-token").id("get-access-token").to("direct:access-token-fetch").choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200")).log("Access Token Fetch Successful").to("direct:access-token-save")
                .otherwise().log("Access Token Fetch Unsuccessful").to("direct:access-token-error");
    }
}
