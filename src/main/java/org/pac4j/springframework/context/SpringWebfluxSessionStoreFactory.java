package org.pac4j.springframework.context;

import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.context.session.SessionStoreFactory;
import org.springframework.web.server.ServerWebExchange;

/**
 * Build the session store for Spring Webflux.
 *
 * @author Jerome LELEU
 * @since 1.1.O
 */
public class SpringWebfluxSessionStoreFactory implements SessionStoreFactory {

    public static final SpringWebfluxSessionStoreFactory INSTANCE = new SpringWebfluxSessionStoreFactory();

    private SpringWebfluxSessionStoreFactory() {}

    @Override
    public SessionStore newSessionStore(final Object... objects) {
        final ServerWebExchange exchange = (ServerWebExchange) objects[0];
        return new SpringWebfluxSessionStore(exchange);
    }
}
