package org.pac4j.springframework.context;

import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.WebContextFactory;

/**
 * Build a Spring Webflux context from parameters.
 *
 * @author Jerome LELEU
 * @since 1.0.0
 */
public class SpringWebfluxWebContextFactory implements WebContextFactory {

    public static final SpringWebfluxWebContextFactory INSTANCE = new SpringWebfluxWebContextFactory();

    @Override
    public WebContext newContext(FrameworkParameters parameters) {
        WebFluxFrameworkParameters webFluxParameters = (WebFluxFrameworkParameters) parameters;
        return new SpringWebfluxWebContext(webFluxParameters.getExchange());
    }
}
