package org.pac4j.springframework.context;

import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.context.session.SessionStoreFactory;

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
    public SessionStore newSessionStore(FrameworkParameters parameters) {
        WebFluxFrameworkParameters webFluxParameters = (WebFluxFrameworkParameters) parameters;
        return new SpringWebfluxSessionStore(webFluxParameters.getExchange());
    }
}
