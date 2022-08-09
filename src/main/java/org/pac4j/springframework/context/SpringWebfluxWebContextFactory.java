package org.pac4j.springframework.context;

import org.pac4j.core.context.WebContextFactory;
import org.springframework.web.server.ServerWebExchange;

/**
 * Build a Spring Webflux context from parameters.
 *
 * @author Jerome LELEU
 * @since 1.0.0
 */
public class SpringWebfluxWebContextFactory implements WebContextFactory {

    public static final SpringWebfluxWebContextFactory INSTANCE = new SpringWebfluxWebContextFactory();

    @Override
    public SpringWebfluxWebContext newContext(final Object... parameters) {
        return new SpringWebfluxWebContext((ServerWebExchange) parameters[0]);
    }
}
