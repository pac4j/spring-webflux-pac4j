package org.pac4j.springframework.context;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class WebContextTest {

    @Test
    void relativeCallbackUsesPublicUriAfterForwardedHeaderTransformation() {
        final var request = MockServerHttpRequest.get("http://internal.example:8080/protected")
            .localAddress(InetSocketAddress.createUnresolved("internal.example", 8080))
            .header("X-Forwarded-Proto", "https")
            .header("X-Forwarded-Host", "public.example")
            .header("X-Forwarded-Port", "443").build();
        final var transformed = new ForwardedHeaderTransformer().apply(request);
        final var exchange = MockServerWebExchange.from(request).mutate().request(transformed).build();
        final var context = new SpringWebfluxWebContext(exchange);

        assertEquals("https", context.getScheme());
        assertTrue(context.isSecure());
        assertEquals("public.example", context.getServerName());
        assertEquals(443, context.getServerPort());
        assertEquals("https://public.example/callback", new DefaultUrlResolver(true).compute("/callback", context));
    }

    @Test
    void uriDeterminesDefaultAndExplicitPortsWithoutLocalAddress() {
        for (final var url : new String[] {"http://public.example", "https://public.example", "https://public.example:8443"}) {
            final var context = new SpringWebfluxWebContext(MockServerWebExchange.from(MockServerHttpRequest.get(url)));
            assertEquals(url + "/callback", new DefaultUrlResolver(true).compute("/callback", context));
        }
    }

    @Test
    void forwardedHeadersAreIgnoredUntilSpringTransformsTheRequest() {
        final var request = MockServerHttpRequest.get("http://public.example/protected")
            .header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "untrusted.example").build();
        final var context = new SpringWebfluxWebContext(MockServerWebExchange.from(request));
        assertFalse(context.isSecure());
        assertEquals("http://public.example/callback", new DefaultUrlResolver(true).compute("/callback", context));
    }

    @Test
    void securityFollowsPublicSchemeRatherThanInternalTlsConnection() {
        final var mockRequest = MockServerHttpRequest.get("https://internal.example:8443/protected")
            .header("X-Forwarded-Proto", "http")
            .header("X-Forwarded-Host", "public.example")
            .header("X-Forwarded-Port", "80").build();
        final var request = mockRequest.mutate().sslInfo(Mockito.mock(SslInfo.class)).build();
        final var transformed = new ForwardedHeaderTransformer().apply(request);
        final var exchange = MockServerWebExchange.from(mockRequest).mutate().request(transformed).build();
        final var context = new SpringWebfluxWebContext(exchange);

        assertNotNull(exchange.getRequest().getSslInfo());
        assertEquals("http", context.getScheme());
        assertFalse(context.isSecure());
        assertEquals("http://public.example/callback", new DefaultUrlResolver(true).compute("/callback", context));
    }
}
