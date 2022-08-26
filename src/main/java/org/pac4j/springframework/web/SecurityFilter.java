package org.pac4j.springframework.web;

import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.FindBest;
import org.pac4j.core.util.security.SecurityEndpoint;
import org.pac4j.core.util.security.SecurityEndpointBuilder;
import org.pac4j.springframework.context.SpringWebfluxSessionStore;
import org.pac4j.springframework.context.SpringWebfluxWebContext;
import org.pac4j.springframework.context.SpringWebfluxWebContextFactory;
import org.pac4j.springframework.http.SpringWebfluxHttpActionAdapter;
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

    private SecurityLogic securityLogic;

    private HttpActionAdapter httpActionAdapter;

    private String clients;

    private String authorizers;

    private String matchers;

    private Config config;

    private static long consumedTime = 0;

    public SecurityFilter(final Config config, final Object... parameters) {
        this.config = config;
        SecurityEndpointBuilder.buildConfig(this, config, parameters);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {

        final long t0 = System.currentTimeMillis();
        try {

            final SessionStore bestSessionStore = FindBest.sessionStore(null, config, new SpringWebfluxSessionStore(serverWebExchange));
            final HttpActionAdapter bestAdapter = FindBest.httpActionAdapter(null, config, SpringWebfluxHttpActionAdapter.INSTANCE);
            final SecurityLogic bestLogic = FindBest.securityLogic(securityLogic, config, DefaultSecurityLogic.INSTANCE);

            final SpringWebfluxWebContext context = (SpringWebfluxWebContext) FindBest.webContextFactory(null, config, SpringWebfluxWebContextFactory.INSTANCE).newContext(serverWebExchange);

            final Object result = bestLogic.perform(context, bestSessionStore, config, (ctx, session, profiles, parameters) -> ACCESS_GRANTED, bestAdapter, clients, authorizers, matchers);
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

    public SecurityLogic getSecurityLogic() {
        return securityLogic;
    }

    @Override
    public void setSecurityLogic(SecurityLogic securityLogic) {
        this.securityLogic = securityLogic;
    }

    public HttpActionAdapter getHttpActionAdapter() {
        return httpActionAdapter;
    }

    @Override
    public void setHttpActionAdapter(final HttpActionAdapter httpActionAdapter) {
        this.httpActionAdapter = httpActionAdapter;
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

    public void setConfig(Config config) {
        this.config = config;
    }

    public static long getConsumedTime() {
        return consumedTime;
    }
}
