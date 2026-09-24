package org.pac4j.springframework.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(10)
class SessionLifecycleTest {

    @Test
    void renewalCompletesBeforeStoreReportsSuccess() {
        final var exchange = spy(MockServerWebExchange.from(MockServerHttpRequest.get("/callback")));
        final var session = mock(WebSession.class);
        final var renewed = new AtomicBoolean();
        when(exchange.getSession()).thenReturn(Mono.just(session));
        when(session.changeSessionId()).thenReturn(Mono.delay(Duration.ofMillis(50))
            .doOnNext(i -> renewed.set(true)).then());
        StepVerifier.create(Mono.fromCallable(() -> {
            assertTrue(new SpringWebfluxSessionStore(exchange).renewSession(null));
            assertTrue(renewed.get());
            return true;
        }).subscribeOn(Schedulers.boundedElastic())).expectNext(true).verifyComplete();
    }

    @Test
    void invalidationFailureIsPropagated() {
        final var exchange = spy(MockServerWebExchange.from(MockServerHttpRequest.get("/logout")));
        final var session = mock(WebSession.class);
        when(exchange.getSession()).thenReturn(Mono.just(session));
        final var failure = new IllegalStateException("session backend unavailable");
        when(session.invalidate()).thenReturn(Mono.error(failure));
        StepVerifier.create(Mono.fromCallable(() -> new SpringWebfluxSessionStore(exchange).destroySession(null))
            .subscribeOn(Schedulers.boundedElastic())).expectErrorMatches(e -> e == failure).verify();
    }
}
