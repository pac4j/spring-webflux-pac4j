package org.pac4j.springframework.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.pac4j.core.context.FrameworkParameters;
import org.springframework.web.server.ServerWebExchange;

/**
 * Specific WebFlux parameters.
 *
 * @author Marvin Kienitz
 * @since 3.0.0
 */
@RequiredArgsConstructor
@Getter
public class SpringWebFluxFrameworkParameters implements FrameworkParameters {

    private final ServerWebExchange exchange;
}
