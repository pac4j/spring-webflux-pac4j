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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxSliceTest(controllers = CallbackController.class)
@Import(CallbackControllerTest.TestConfig.class)
public class CallbackControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Value("${pac4j.callback.path:/callback}")
    private String callbackPath;

    @MockitoBean
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

    @Test
    void oversizedCallbackIsRejectedBeforeAuthentication() {
        webClient.post().uri(callbackPath)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue("x".repeat(1024 * 1024))
            .exchange().expectStatus().isEqualTo(413);
        Mockito.verify(config, Mockito.never()).getCallbackLogic();
    }

    @SpringBootConfiguration
    public static class TestConfig {
        @Bean
        public CallbackController callbackController() {
            return new CallbackController();
        }
    }
}

