package org.pac4j.springframework.web;

import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.springframework.context.SpringWebFluxFrameworkParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>This controller handles the (application + identity provider) logout process.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
@Controller
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

    private static long consumedTime = 0;

    @RequestMapping("${pac4j.logout.path:/logout}")
    public Mono<Void> logout(final ServerWebExchange serverWebExchange) {

        final SpringWebFluxFrameworkParameters frameworkParameters = new SpringWebFluxFrameworkParameters(serverWebExchange);

        final long t0 = System.currentTimeMillis();
        try {

            FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);

            return (Mono<Void>) config.getLogoutLogic().perform(config, this.defaultUrl, this.logoutUrlPattern, this.localLogout, this.destroySession, this.centralLogout, frameworkParameters);

        } finally {
            final long t1 = System.currentTimeMillis();
            trackTime(t0, t1);
        }
    }

    protected void trackTime(final long t0, final long t1) {
        consumedTime += t1-t0;
    }

    public String getDefaultUrl() {
        return this.defaultUrl;
    }

    public void setDefaultUrl(final String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    public String getLogoutUrlPattern() {
        return logoutUrlPattern;
    }

    public void setLogoutUrlPattern(final String logoutUrlPattern) {
        this.logoutUrlPattern = logoutUrlPattern;
    }

    public LogoutLogic getLogoutLogic() {
        return logoutLogic;
    }

    public void setLogoutLogic(final LogoutLogic logoutLogic) {
        this.logoutLogic = logoutLogic;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(final Config config) {
        this.config = config;
    }

    public Boolean getLocalLogout() {
        return localLogout;
    }

    public void setLocalLogout(final Boolean localLogout) {
        this.localLogout = localLogout;
    }

    public Boolean getCentralLogout() {
        return centralLogout;
    }

    public void setCentralLogout(final Boolean centralLogout) {
        this.centralLogout = centralLogout;
    }

    public Boolean getDestroySession() {
        return destroySession;
    }

    public void setDestroySession(final Boolean destroySession) {
        this.destroySession = destroySession;
    }

    public static long getConsumedTime() {
        return consumedTime;
    }
}
