package org.pac4j.springframework.web;

import lombok.Getter;
import lombok.Setter;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.springframework.context.SpringWebFluxFrameworkParameters;
import org.pac4j.springframework.context.SpringWebfluxRequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * <p>This controller handles the (application + identity provider) logout process.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
@Controller
@Getter
@Setter
public class LogoutController {

    private LogoutLogic logoutLogic;

    @Value("${pac4j.logout.defaultUrl:#{null}}")
    private String defaultUrl;

    @Value("${pac4j.logout.logoutUrlPattern:#{null}}")
    private String logoutUrlPattern;

    @Value("${pac4j.logout.localLogout:#{null}}")
    private Boolean localLogout;

    @Value("${pac4j.logout.destroySession:#{null}}")
    private Boolean destroySession;

    @Value("${pac4j.logout.centralLogout:#{null}}")
    private Boolean centralLogout;

    @Autowired
    private Config config;

    private int maxBodySize = SpringWebfluxRequestBody.DEFAULT_MAX_BODY_SIZE;

    @Getter
    private static long consumedTime = 0;

    /**
     * Performs the configured local and/or identity-provider logout.
     * Loads the session before running the synchronous logout logic, including
     * any requested session destruction, on a worker thread.
     *
     * @param serverWebExchange the logout request and response
     * @return completion of logout processing and its response
     */
    @RequestMapping("${pac4j.logout.path:/logout}")
    public Mono<Void> logout(final ServerWebExchange serverWebExchange) {

        return SpringWebfluxRequestBody.prepareFormData(serverWebExchange, maxBodySize)
            .flatMap(exchange -> exchange.getSession().then(Mono.defer(() -> {
                final SpringWebFluxFrameworkParameters frameworkParameters = new SpringWebFluxFrameworkParameters(exchange);

                final long t0 = System.currentTimeMillis();
                try {

                    FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);

                    final var logic = logoutLogic != null ? logoutLogic : config.getLogoutLogic();
                    return (Mono<Void>) logic.perform(config, this.defaultUrl, this.logoutUrlPattern, this.localLogout, this.destroySession, this.centralLogout, frameworkParameters);

                } finally {
                    final long t1 = System.currentTimeMillis();
                    trackTime(t0, t1);
                }
            }).subscribeOn(Schedulers.boundedElastic())));
    }

    @Value("${pac4j.logout.maxBodySize:262144}")
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
