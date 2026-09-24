package org.pac4j.springframework.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.pac4j.core.config.Config;
import org.pac4j.springframework.context.SpringWebfluxSessionStore;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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
        final var events = new java.util.concurrent.CopyOnWriteArrayList<String>();
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
}
