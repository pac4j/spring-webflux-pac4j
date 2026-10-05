package org.pac4j.springframework.context;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Loads bounded request content for synchronous pac4j clients while preserving
 * a replayable body and parsed form data for downstream WebFlux handlers.
 *
 * @author Jerome Leleu
 * @since 3.0.2
 */
public final class SpringWebfluxRequestBody {

    /** Default maximum body size in bytes (256 KiB). */
    public static final int DEFAULT_MAX_BODY_SIZE = 256 * 1024;

    /** Exchange attribute containing decoded form parameters. */
    public static final String FORM_PARAMETERS_ATTRIBUTE = "PAC4J_FORM_PARAMETERS";

    private static final String CACHED_BODY_ATTRIBUTE = "PAC4J_CACHED_REQUEST_BODY";

    private SpringWebfluxRequestBody() {}

    /**
     * Prepares form requests, leaving other request bodies unread.
     * @param exchange the request and response
     * @param maxBodySize the maximum body size in bytes
     * @return the exchange with replayable form content
     */
    public static Mono<ServerWebExchange> prepareFormData(final ServerWebExchange exchange, final int maxBodySize) {
        return isForm(exchange) ? prepare(exchange, maxBodySize) : Mono.just(exchange);
    }

    /**
     * Loads the raw body and, for URL-encoded forms, decoded parameters.
     * @param exchange the request and response
     * @param maxBodySize the maximum body size in bytes
     * @return the exchange with replayable content
     */
    public static Mono<ServerWebExchange> prepare(final ServerWebExchange exchange, final int maxBodySize) {
        return Mono.defer(() -> {
            if (maxBodySize <= 0) {
                throw new IllegalArgumentException("maxBodySize must be positive");
            }
            final byte[] cached = exchange.getAttribute(CACHED_BODY_ATTRIBUTE);
            final Mono<byte[]> body = cached == null
                ? DataBufferUtils.join(exchange.getRequest().getBody(), maxBodySize).map(buffer -> {
                    try {
                        final byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                }).defaultIfEmpty(new byte[0])
                : Mono.just(cached);
            return body.flatMap(bytes -> {
                if (bytes.length > maxBodySize) {
                    return Mono.error(new DataBufferLimitException("Request body exceeds " + maxBodySize + " bytes"));
                }
                final ServerHttpRequest request = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return Flux.defer(() -> bytes.length == 0 ? Flux.empty()
                            : Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                    }
                };
                final Mono<MultiValueMap<String, String>> formData;
                if (isForm(exchange)) {
                    final var reader = new FormHttpMessageReader();
                    reader.setMaxInMemorySize(maxBodySize);
                    formData = reader.readMono(ResolvableType.forClass(MultiValueMap.class), request, Collections.emptyMap());
                } else {
                    formData = Mono.just(new LinkedMultiValueMap<>());
                }
                return formData.map(form -> {
                    final var contentType = request.getHeaders().getContentType();
                    final var declaredCharset = contentType != null ? contentType.getCharset() : null;
                    final var charset = declaredCharset != null ? declaredCharset : StandardCharsets.UTF_8;
                    exchange.getAttributes().put(CACHED_BODY_ATTRIBUTE, bytes);
                    exchange.getAttributes().put(SpringWebfluxWebContext.REQUEST_BODY_ATTRIBUTE, new String(bytes, charset));
                    exchange.getAttributes().put(FORM_PARAMETERS_ATTRIBUTE, form);
                    return (ServerWebExchange) new ServerWebExchangeDecorator(exchange) {
                        @Override
                        public ServerHttpRequest getRequest() {
                            return request;
                        }

                        @Override
                        public Mono<MultiValueMap<String, String>> getFormData() {
                            return Mono.just(form);
                        }
                    };
                });
            });
        }).onErrorMap(DataBufferLimitException.class,
            error -> new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Request body is too large", error));
    }

    private static boolean isForm(final ServerWebExchange exchange) {
        final var contentType = exchange.getRequest().getHeaders().getContentType();
        return contentType != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType);
    }
}
