package org.pac4j.springframework.web;

import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.springframework.context.SpringWebFluxFrameworkParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

import static org.pac4j.springframework.context.SpringWebfluxWebContext.REQUEST_BODY_ATTRIBUTE;

/**
 * <p>This controller finishes the login process for an indirect client.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
@Controller
public class CallbackController {

    private CallbackLogic callbackLogic;

    @Value("${pac4j.callback.defaultUrl:#{null}}")
    private String defaultUrl;

    @Value("${pac4j.callback.renewSession:#{null}}")
    private Boolean renewSession;

    @Value("${pac4j.callback.defaultClient:#{null}}")
    private String defaultClient;

    @Autowired
    private Config config;

    private static long consumedTime = 0;

    /**
     * Processes the identity provider's callback, with or without a request body.
     * Stores the raw body for clients that need it and loads the session before
     * running the synchronous callback logic on a worker thread.
     *
     * @param serverWebExchange the callback request and response
     * @return completion of callback processing and its response
     */
    @RequestMapping("${pac4j.callback.path:/callback}")
    public Mono<Void> callback(final ServerWebExchange serverWebExchange) {

        return DataBufferUtils.join(serverWebExchange.getRequest().getBody())
            .map(buffer -> {
                try {
                    return buffer.toString(StandardCharsets.UTF_8);
                } finally {
                    DataBufferUtils.release(buffer);
                }
            })
            .defaultIfEmpty("")
            .flatMap(content -> {
                serverWebExchange.getAttributes().put(REQUEST_BODY_ATTRIBUTE, content);
                return serverWebExchange.getSession().then(Mono.defer(() -> {
                    final long t0 = System.currentTimeMillis();
                    try {
                        FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);
                        return (Mono<Void>) config.getCallbackLogic().perform(config, this.defaultUrl,
                            this.renewSession, this.defaultClient,
                            new SpringWebFluxFrameworkParameters(serverWebExchange));
                    } finally {
                        trackTime(t0, System.currentTimeMillis());
                    }
                }).subscribeOn(Schedulers.boundedElastic()));
            });
    }


    protected void trackTime(final long t0, final long t1) {
        consumedTime += t1 - t0;
    }

    /**
     * Delegates callbacks whose URL includes a client-name path segment to
     * {@link #callback(ServerWebExchange)}, preserving the original request path.
     *
     * @param serverWebExchange the callback request and response
     * @param cn the client name captured from the callback path
     * @return completion of callback processing and its response
     */
    @RequestMapping("${pac4j.callback.path/{cn}:/callback/{cn}}")
    public Mono<Void> callbackWithClientName(final ServerWebExchange serverWebExchange, @PathVariable("cn") final String cn) {

        return callback(serverWebExchange);
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public void setDefaultUrl(final String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    public CallbackLogic getCallbackLogic() {
        return callbackLogic;
    }

    public void setCallbackLogic(final CallbackLogic callbackLogic) {
        this.callbackLogic = callbackLogic;
    }

    public Boolean getRenewSession() {
        return renewSession;
    }

    public void setRenewSession(final Boolean renewSession) {
        this.renewSession = renewSession;
    }

    public String getDefaultClient() {
        return defaultClient;
    }

    public void setDefaultClient(final String client) {
        this.defaultClient = client;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(final Config config) {
        this.config = config;
    }

    public static long getConsumedTime() {
        return consumedTime;
    }
}
