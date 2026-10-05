package org.pac4j.springframework.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.pac4j.core.authorization.authorizer.CsrfAuthorizer;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.springframework.context.SpringWebfluxSessionStore;
import org.pac4j.springframework.context.SpringWebfluxWebContextFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.pac4j.springframework.context.SpringWebfluxWebContext.REQUEST_BODY_ATTRIBUTE;

@Timeout(10)
class ReactiveEndpointsTest {

    @Test
    void emptyGetCallbackRunsOnceOnWorkerWithSessionAndQueryParameters() {
        final var request = MockServerHttpRequest.get("/callback?code=abc&state=xyz").build();
        final var original = MockServerWebExchange.from(request);
        final var exchange = spy(original);
        final var session = new MockWebSession();
        session.getAttributes().put("state", "xyz");
        when(exchange.getSession()).thenReturn(Mono.delay(Duration.ofMillis(50)).map(i -> (WebSession) session).cache());
        final var calls = new AtomicInteger();
        final var config = new Config();
        config.setCallbackLogic((c, url, renew, client, parameters) -> {
            assertFalse(Schedulers.isInNonBlockingThread());
            assertEquals("abc", exchange.getRequest().getQueryParams().getFirst("code"));
            assertEquals("xyz", new SpringWebfluxSessionStore(exchange).get(null, "state").orElseThrow());
            calls.incrementAndGet();
            return Mono.empty();
        });
        final var controller = new CallbackController();
        controller.setConfig(config);
        final var result = controller.callback(exchange);
        assertEquals(0, calls.get());
        StepVerifier.create(result.subscribeOn(Schedulers.parallel())).verifyComplete();
        assertEquals(1, calls.get());
    }

    @Test
    void postCallbackReadsOnlyReadableBytesFromDirectBuffer() {
        final var text = "SAMLResponse=test&name=Jérôme";
        final var bytes = text.getBytes(StandardCharsets.UTF_8);
        final var buffer = ByteBuffer.allocateDirect(bytes.length + 4);
        buffer.putInt(123).put(bytes).flip();
        buffer.position(4);
        final var data = DefaultDataBufferFactory.sharedInstance.wrap(buffer);
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/callback")
            .body(Mono.just(data)));
        final var calls = new AtomicInteger();
        final var config = new Config();
        config.setCallbackLogic((c, url, renew, client, parameters) -> {
            assertEquals(text, exchange.getAttribute(REQUEST_BODY_ATTRIBUTE));
            assertFalse(Schedulers.isInNonBlockingThread());
            calls.incrementAndGet();
            return Mono.empty();
        });
        final var controller = new CallbackController();
        controller.setConfig(config);
        StepVerifier.create(controller.callback(exchange).subscribeOn(Schedulers.parallel())).verifyComplete();
        assertEquals(1, calls.get());
    }

    @Test
    void logoutWaitsForSessionInvalidationBeforeResponse() {
        final var exchange = spy(MockServerWebExchange.from(MockServerHttpRequest.get("/logout")));
        final var session = mock(WebSession.class);
        final var events = new CopyOnWriteArrayList<String>();
        when(session.invalidate()).thenReturn(Mono.delay(Duration.ofMillis(50))
            .doOnNext(i -> events.add("invalidated")).then());
        when(exchange.getSession()).thenReturn(Mono.delay(Duration.ofMillis(50)).map(i -> (WebSession) session).cache());
        final var config = new Config();
        config.setLogoutLogic((c, url, pattern, local, destroy, central, parameters) -> {
            assertFalse(Schedulers.isInNonBlockingThread());
            assertTrue(new SpringWebfluxSessionStore(exchange).destroySession(null));
            return Mono.fromRunnable(() -> events.add("response"));
        });
        final var controller = new LogoutController();
        controller.setConfig(config);
        final var result = controller.logout(exchange);
        assertTrue(events.isEmpty());
        StepVerifier.create(result.subscribeOn(Schedulers.parallel())).verifyComplete();
        assertEquals(List.of("invalidated", "response"), events);
    }

    @Test
    void securityFilterLoadsSessionAndRunsLogicOnWorkerBeforeChain() {
        final var exchange = spy(MockServerWebExchange.from(MockServerHttpRequest.get("/protected")));
        final var session = new MockWebSession();
        session.getAttributes().put("profile", "alice");
        when(exchange.getSession()).thenReturn(Mono.delay(Duration.ofMillis(50)).map(i -> (WebSession) session).cache());
        final var calls = new AtomicInteger();
        final var config = new Config();
        config.setSecurityLogic((c, access, clients, authorizers, matchers, parameters) -> {
            assertFalse(Schedulers.isInNonBlockingThread());
            assertEquals("alice", new SpringWebfluxSessionStore(exchange).get(null, "profile").orElseThrow());
            calls.incrementAndGet();
            return assertDoesNotThrow(() -> access.adapt(null, null, List.of()));
        });
        final var chainCalls = new AtomicInteger();
        final var result = new SecurityFilter(config).filter(exchange,
            e -> Mono.fromRunnable(chainCalls::incrementAndGet));
        assertEquals(0, calls.get());
        StepVerifier.create(result.subscribeOn(Schedulers.parallel())).verifyComplete();
        assertEquals(1, calls.get());
        assertEquals(1, chainCalls.get());
    }

    @Test
    void callbackExposesDecodedFormParametersAlongsideQueryAndRawBody() {
        final var body = "username=J%C3%A9r%C3%B4me&password=a%2Bb+test&code=abc&state=xyz&tag=one&tag=two";
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/callback?tag=query&client_name=oidc")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(body));
        final var config = new Config();
        config.setCallbackLogic((c, url, renew, client, parameters) -> {
            final var context = SpringWebfluxWebContextFactory.INSTANCE.newContext(parameters);
            assertEquals(body, context.getRequestContent());
            assertEquals("abc", context.getRequestParameter("code").orElseThrow());
            assertEquals("xyz", context.getRequestParameter("state").orElseThrow());
            assertEquals("oidc", context.getRequestParameter("client_name").orElseThrow());
            assertEquals("query", context.getRequestParameter("tag").orElseThrow());
            assertArrayEquals(new String[] {"query", "one", "two"}, context.getRequestParameters().get("tag"));
            final var credentials = new FormExtractor("username", "password")
                .extract(new CallContext(context, new SpringWebfluxSessionStore(exchange), c.getProfileManagerFactory()))
                .orElseThrow();
            final var form = (UsernamePasswordCredentials) credentials;
            assertEquals("Jérôme", form.getUsername());
            assertEquals("a+b test", form.getPassword());
            return Mono.empty();
        });
        final var controller = new CallbackController();
        controller.setConfig(config);
        StepVerifier.create(controller.callback(exchange)).verifyComplete();
    }

    @Test
    void securityFilterAcceptsFormCsrfTokenAndPreservesBodyForDownstream() {
        final var body = "pac4jCsrfToken=valid-token&value=hello";
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/protected")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(body));
        final var config = new Config();
        config.setSecurityLogic((c, access, clients, authorizers, matchers, parameters) -> {
            final var context = SpringWebfluxWebContextFactory.INSTANCE.newContext(parameters);
            final var sessionStore = c.getSessionStoreFactory().newSessionStore(parameters);
            sessionStore.set(context, Pac4jConstants.CSRF_TOKEN, "valid-token");
            sessionStore.set(context, Pac4jConstants.CSRF_TOKEN_EXPIRATION_DATE, System.currentTimeMillis() + 60_000);
            assertTrue(new CsrfAuthorizer().isAuthorized(context, sessionStore, List.of()));
            return assertDoesNotThrow(() -> access.adapt(context, sessionStore, List.of()));
        });
        final var calls = new AtomicInteger();
        StepVerifier.create(new SecurityFilter(config).filter(exchange, downstream ->
            downstream.getFormData().doOnNext(form -> {
                assertEquals("hello", form.getFirst("value"));
                calls.incrementAndGet();
            }).then(DataBufferUtils.join(downstream.getRequest().getBody())
                .doOnNext(buffer -> {
                    try {
                        assertEquals(body, buffer.toString(StandardCharsets.UTF_8));
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                }).then()))).verifyComplete();
        assertEquals(1, calls.get());
    }

    @Test
    void controllerLogicOverridesConfigAndRunsOnWorker() {
        final var config = new Config();
        config.setCallbackLogic((c, url, renew, client, parameters) -> {
            fail("Config callback must not replace the controller override");
            return Mono.empty();
        });
        config.setLogoutLogic((c, url, pattern, local, destroy, central, parameters) -> {
            fail("Config logout must not replace the controller override");
            return Mono.empty();
        });
        final var callbacks = new AtomicInteger();
        final var callback = new CallbackController();
        callback.setConfig(config);
        callback.setCallbackLogic((c, url, renew, client, parameters) -> {
            assertFalse(Schedulers.isInNonBlockingThread());
            callbacks.incrementAndGet();
            return Mono.empty();
        });
        final var logouts = new AtomicInteger();
        final var logout = new LogoutController();
        logout.setConfig(config);
        logout.setLogoutLogic((c, url, pattern, local, destroy, central, parameters) -> {
            assertFalse(Schedulers.isInNonBlockingThread());
            logouts.incrementAndGet();
            return Mono.empty();
        });
        StepVerifier.create(callback.callback(MockServerWebExchange.from(MockServerHttpRequest.get("/callback")))
            .subscribeOn(Schedulers.parallel())).verifyComplete();
        StepVerifier.create(logout.logout(MockServerWebExchange.from(MockServerHttpRequest.get("/logout")))
            .subscribeOn(Schedulers.parallel())).verifyComplete();
        assertEquals(1, callbacks.get());
        assertEquals(1, logouts.get());
    }

    @Test
    void callbackEnforcesCustomLimitForSingleAndChunkedBodiesBeforeLoadingSession() {
        final var config = new Config();
        final var controller = new CallbackController();
        controller.setConfig(config);
        controller.setMaxBodySize(8);
        for (final var chunks : List.of(List.of("123456789"), List.of("1234", "56789"))) {
            final var body = Flux.fromIterable(chunks)
                .map(chunk -> DefaultDataBufferFactory.sharedInstance.wrap(chunk.getBytes(StandardCharsets.UTF_8)));
            final var exchange = spy(MockServerWebExchange.from(MockServerHttpRequest.post("/callback").body(body)));
            StepVerifier.create(controller.callback(exchange))
                .expectErrorMatches(error -> error instanceof ResponseStatusException status
                    && status.getStatusCode().value() == 413).verify();
            verify(exchange, never()).getSession();
        }
    }

    @Test
    void callbackAcceptsBodyAtLimitAndAllowsIncreasingDefaultLimitForForms() {
        final var controller = new CallbackController();
        final var config = new Config();
        final var calls = new AtomicInteger();
        config.setCallbackLogic((c, url, renew, client, parameters) -> {
            final var context = SpringWebfluxWebContextFactory.INSTANCE.newContext(parameters);
            assertEquals(controller.getMaxBodySize(), context.getRequestContent().getBytes(StandardCharsets.UTF_8).length);
            if (context.getRequestHeader("Content-Type").orElse("").startsWith("application/x-www-form-urlencoded")) {
                assertEquals(controller.getMaxBodySize() - 5, context.getRequestParameter("code").orElseThrow().length());
            }
            calls.incrementAndGet();
            return Mono.empty();
        });
        controller.setConfig(config);
        controller.setMaxBodySize(8);
        StepVerifier.create(controller.callback(MockServerWebExchange.from(MockServerHttpRequest.post("/callback")
            .body("12345678")))).verifyComplete();
        controller.setMaxBodySize(512 * 1024);
        StepVerifier.create(controller.callback(MockServerWebExchange.from(MockServerHttpRequest.post("/callback")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("code=" + "x".repeat(controller.getMaxBodySize() - 5))))).verifyComplete();
        assertEquals(2, calls.get());
        assertThrows(IllegalArgumentException.class, () -> controller.setMaxBodySize(0));
    }

    @Test
    void callbackAfterSecurityFilterRetainsBodyAndAppliesItsOwnLimit() {
        final var config = new Config();
        config.setSecurityLogic((c, access, clients, authorizers, matchers, parameters) ->
            assertDoesNotThrow(() -> access.adapt(null, null, List.of())));
        final var calls = new AtomicInteger();
        config.setCallbackLogic((c, url, renew, client, parameters) -> {
            final var context = SpringWebfluxWebContextFactory.INSTANCE.newContext(parameters);
            assertEquals("code=abc", context.getRequestContent());
            assertEquals("abc", context.getRequestParameter("code").orElseThrow());
            calls.incrementAndGet();
            return Mono.empty();
        });
        final var controller = new CallbackController();
        controller.setConfig(config);
        final var filter = new SecurityFilter(config);
        final var request = MockServerHttpRequest.post("/callback")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body("code=abc");
        StepVerifier.create(filter.filter(MockServerWebExchange.from(request), controller::callback)).verifyComplete();
        assertEquals(1, calls.get());
        controller.setMaxBodySize(4);
        StepVerifier.create(filter.filter(MockServerWebExchange.from(request), controller::callback))
            .expectErrorMatches(error -> error instanceof ResponseStatusException status
                && status.getStatusCode().value() == 413).verify();
        assertEquals(1, calls.get());
    }

    @Test
    void logoutExposesFormParametersUsingDeclaredCharset() {
        final var config = new Config();
        config.setLogoutLogic((c, url, pattern, local, destroy, central, parameters) -> {
            final var context = SpringWebfluxWebContextFactory.INSTANCE.newContext(parameters);
            assertEquals("Jérôme", context.getRequestParameter("name").orElseThrow());
            assertEquals("/bye", context.getRequestParameter("url").orElseThrow());
            return Mono.empty();
        });
        final var controller = new LogoutController();
        controller.setConfig(config);
        final var request = MockServerHttpRequest.post("/logout")
            .contentType(MediaType.parseMediaType("application/x-www-form-urlencoded;charset=ISO-8859-1"))
            .body("name=J%E9r%F4me&url=%2Fbye");
        StepVerifier.create(controller.logout(MockServerWebExchange.from(request))).verifyComplete();
    }

    @Test
    void securityFilterLeavesNonFormBodiesUnreadUntilDownstreamSubscribes() {
        final var subscriptions = new AtomicInteger();
        final var body = Flux.defer(() -> {
            subscriptions.incrementAndGet();
            return Flux.just(DefaultDataBufferFactory.sharedInstance
                .wrap("{\"value\":1}".getBytes(StandardCharsets.UTF_8)));
        });
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/protected")
            .contentType(MediaType.APPLICATION_JSON).body(body));
        final var config = new Config();
        config.setSecurityLogic((c, access, clients, authorizers, matchers, parameters) -> {
            assertEquals(0, subscriptions.get());
            return assertDoesNotThrow(() -> access.adapt(null, null, List.of()));
        });
        final var filter = new SecurityFilter(config);
        filter.setMaxBodySize(1);
        StepVerifier.create(filter.filter(exchange, downstream ->
            DataBufferUtils.join(downstream.getRequest().getBody())
                .doOnNext(buffer -> {
                    try {
                        assertEquals("{\"value\":1}", buffer.toString(StandardCharsets.UTF_8));
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                }).then())).verifyComplete();
        assertEquals(1, subscriptions.get());
    }
}
