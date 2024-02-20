package org.pac4j.springframework.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.pac4j.core.config.Config;
import org.pac4j.springframework.context.SpringWebfluxWebContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = CallbackController.class)
@Import(CallbackControllerTest.TestConfig.class)
public class CallbackControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Value("${pac4j.callback.path:/callback}")
    private String callbackPath;

    @Autowired
    public Config config;

    @Test
    void testRequestContentIsPassedToWebContext() {
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("param1", "XXXX");
        formData.add("param2", "YYYY");

        Mockito.when(config.getCallbackLogic()).thenReturn((config, s, aBoolean, s1, frameworkParameters) -> {
            final String requestContext = SpringWebfluxWebContextFactory.INSTANCE.newContext(frameworkParameters).getRequestContent();
            Assertions.assertEquals(requestContext, "param1=XXXX&param2=YYYY");
            return Mono.empty();
        });

        webClient.post()
                .uri(callbackPath)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus()
                .isOk();
    }

    @SpringBootConfiguration
    public static class TestConfig {
        @MockBean
        public Config config;
        @Bean
        public CallbackController callbackController() {
            return new CallbackController();
        }
    }
}

