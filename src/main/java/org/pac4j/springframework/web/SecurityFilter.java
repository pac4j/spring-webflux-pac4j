package org.pac4j.springframework.web;

import lombok.Getter;
import lombok.Setter;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.util.security.SecurityEndpoint;
import org.pac4j.core.util.security.SecurityEndpointBuilder;
import org.pac4j.springframework.context.SpringWebFluxFrameworkParameters;
import org.pac4j.springframework.context.SpringWebfluxRequestBody;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * <p>This filter protects an URL.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
@Getter
@Setter
public class SecurityFilter implements WebFilter, SecurityEndpoint {

    private static final Object ACCESS_GRANTED = new Object();

    private String clients;

    private String authorizers;

    private String matchers;

    private Config config;

    private int maxBodySize = SpringWebfluxRequestBody.DEFAULT_MAX_BODY_SIZE;

    @Getter
    private static long consumedTime = 0;

    public SecurityFilter() {}

    public SecurityFilter(final Config config) {
        this.config = config;
    }

    public SecurityFilter(final Config config, final String clients) {
        this(config);
        this.clients = clients;
    }

    public SecurityFilter(final Config config, final String clients, final String authorizers) {
        this(config, clients);
        this.authorizers = authorizers;
    }

    public SecurityFilter(final Config config, final String clients, final String authorizers, final String matchers) {
        this(config, clients, authorizers);
        this.matchers = matchers;
    }

    public static SecurityFilter build(Object... parameters) {
        final SecurityFilter securityFilter = new SecurityFilter();
        SecurityEndpointBuilder.buildConfig(securityFilter, parameters);
        return securityFilter;
    }

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange serverWebExchange, @NonNull WebFilterChain webFilterChain) {

        return SpringWebfluxRequestBody.prepareFormData(serverWebExchange, maxBodySize)
            .flatMap(exchange -> exchange.getSession().then(Mono.defer(() -> {
                final SpringWebFluxFrameworkParameters frameworkParameters = new SpringWebFluxFrameworkParameters(exchange);

                final long t0 = System.currentTimeMillis();
                try {

                    FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);

                    final Object result = config.getSecurityLogic().perform(config, (ctx, session, profiles) -> ACCESS_GRANTED, clients, authorizers, matchers, frameworkParameters);
                    if (result == ACCESS_GRANTED) {
                        return webFilterChain.filter(exchange);
                    }

                    return (Mono<Void>) result;

                } finally {
                    final long t1 = System.currentTimeMillis();
                    trackTime(t0, t1);
                }
            }).subscribeOn(Schedulers.boundedElastic())));
    }

    public void setMaxBodySize(final int maxBodySize) {
        if (maxBodySize <= 0) {
            throw new IllegalArgumentException("maxBodySize must be positive");
        }
        this.maxBodySize = maxBodySize;
    }

    protected void trackTime(final long t0, final long t1) {
        consumedTime += t1-t0;
    }
}
