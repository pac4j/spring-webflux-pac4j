package org.pac4j.springframework.web;

import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.util.security.SecurityEndpoint;
import org.pac4j.core.util.security.SecurityEndpointBuilder;
import org.pac4j.springframework.context.SpringWebFluxFrameworkParameters;
import org.pac4j.springframework.context.SpringWebfluxWebContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * <p>This filter protects an URL.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
public class SecurityFilter implements WebFilter, SecurityEndpoint {

    private static final Object ACCESS_GRANTED = new Object();

    private String clients;

    private String authorizers;

    private String matchers;

    private Config config;

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
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {

        final SpringWebFluxFrameworkParameters frameworkParameters = new SpringWebFluxFrameworkParameters(serverWebExchange);

        final long t0 = System.currentTimeMillis();
        try {

            FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);

            final SpringWebfluxWebContext context = (SpringWebfluxWebContext) config.getWebContextFactory().newContext(frameworkParameters);

            final Object result = config.getSecurityLogic().perform(config, (ctx, session, profiles) -> ACCESS_GRANTED, clients, authorizers, matchers, frameworkParameters);
            if (result == ACCESS_GRANTED) {
                return webFilterChain.filter(serverWebExchange);
            }

            return context.getResult();

        } finally {
            final long t1 = System.currentTimeMillis();
            trackTime(t0, t1);
        }
    }

    protected void trackTime(final long t0, final long t1) {
        consumedTime += t1-t0;
    }

    public String getClients() {
        return clients;
    }

    @Override
    public void setClients(final String clients) {
        this.clients = clients;
    }

    public String getAuthorizers() {
        return authorizers;
    }

    @Override
    public void setAuthorizers(final String authorizers) {
        this.authorizers = authorizers;
    }

    public String getMatchers() {
        return matchers;
    }

    @Override
    public void setMatchers(final String matchers) {
        this.matchers = matchers;
    }

    public Config getConfig() {
        return config;
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
    }

    public static long getConsumedTime() {
        return consumedTime;
    }
}
