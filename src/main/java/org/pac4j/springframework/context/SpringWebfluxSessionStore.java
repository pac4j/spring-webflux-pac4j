package org.pac4j.springframework.context;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import java.util.Map;
import java.util.Optional;

/**
 * <p>This is the specific <code>SessionStore</code> for Spring Webflux.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
public class SpringWebfluxSessionStore implements SessionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringWebfluxSessionStore.class);

    private final ServerWebExchange exchange;

    private WebSession session;

    private boolean loaded;
    private boolean subscribed;

    private int timeout = 20;

    private int timeoutIncrement = 3;

    private static long nbWaitCalls = 0;
    private static long nbWaitInterruptions = 0;
    private static long nbWaitErrors = 0;
    private static long waitedTime = 0;

    public SpringWebfluxSessionStore(final ServerWebExchange exchange) {
        this.exchange = exchange;
        loadSession();
    }

    protected void loadSession() {
        LOGGER.debug("<> Subscribing to session...");
        subscribed = true;
        loaded = false;
        exchange.getSession().subscribe(
                value -> {
                    LOGGER.debug("<> Retrieved session: {}", session);
                    session = value;
                    loaded = true;
                },
                error -> {
                    throw new TechnicalException("Cannot get session");
                },
                () -> {
                    LOGGER.debug("<> No session available");
                    loaded = true;
                }
        );
    }

    @Override
    public Optional<String> getSessionId(final WebContext context, final boolean createSession) {
        LOGGER.debug("Retrieving sessionId (createSession:{})", createSession);

        waitForSession();

        return session != null ? Optional.of(session.getId()) : Optional.empty();
    }

    @Override
    public Optional<Object> get(final WebContext context, final String key) {
        LOGGER.debug("Getting value for key: {}", key);

        waitForSession();

        if (session == null) {
            LOGGER.debug("Returning no value for key: {}", key);
            return Optional.empty();
        }
        Object value = session.getAttributes().get(key);
        LOGGER.debug("Returning value: {} for key: {}", value, key);
        return Optional.ofNullable(value);
    }

    @Override
    public void set(final WebContext context, final String key, final Object value) {
        LOGGER.debug("Setting value: {} for key: {}", value, key);

        waitForSession();

        if (session != null) {
            final Map<String, Object> attributes = session.getAttributes();
            if (value == null) {
                attributes.remove(key);
            } else {
                attributes.put(key, value);
            }
        }
    }

    @Override
    public boolean destroySession(final WebContext context) {
        LOGGER.debug("Invalidatin session...");

        waitForSession();

        if (session != null) {
            session.invalidate();
            subscribed = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean renewSession(final WebContext context) {
        LOGGER.debug("Renewing session...");

        waitForSession();

        if (session != null) {
            session.changeSessionId();
            subscribed = false;
            return true;
        }

        return false;
    }

    protected void waitForSession() {
        if (!subscribed) {
            loadSession();
        }

        nbWaitCalls++;
        int currentTimeout = 0;
        while (!loaded && currentTimeout <= timeout) {
            LOGGER.debug("<> WAITING for session, current timeout: {} ms ", currentTimeout);
            try {
                Thread.sleep(timeoutIncrement);
                waitedTime += timeoutIncrement;
                currentTimeout += timeoutIncrement;
            } catch (final InterruptedException e) {
                LOGGER.debug("<> Aborted wait: {}", e.getMessage());
                nbWaitInterruptions++;
                return;
            }
        }
        if (!loaded) {
            nbWaitErrors++;
        }
    }

    @Override
    public Optional<Object> getTrackableSession(final WebContext context) {
        return Optional.empty();
    }

    @Override
    public Optional<SessionStore> buildFromTrackableSession(final WebContext context, final Object trackableSession) {
        return Optional.empty();
    }

    public WebSession getSession() {
        return session;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public int getTimeoutIncrement() {
        return timeoutIncrement;
    }

    public void setTimeoutIncrement(final int timeoutIncrement) {
        this.timeoutIncrement = timeoutIncrement;
    }

    public static long getNbWaitCalls() {
        return nbWaitCalls;
    }

    public static long getNbWaitInterruptions() {
        return nbWaitInterruptions;
    }

    public static long getNbWaitErrors() {
        return nbWaitErrors;
    }

    public static long getWaitedTime() {
        return waitedTime;
    }

    @Override
    public String toString() {
        return CommonHelper.toNiceString(this.getClass(), "session", session, "loaded", loaded,
                "timeout", timeout, "timeoutIncrement", timeoutIncrement, "waitedTime", waitedTime,
                "nbWaitCalls", nbWaitCalls, "nbWaitErrors", nbWaitErrors, "nbWaitInterruptions", nbWaitInterruptions);
    }
}
