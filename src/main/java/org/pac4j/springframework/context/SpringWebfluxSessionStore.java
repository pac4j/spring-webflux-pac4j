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

    private WebSession session;

    private int timeout = 25;

    private int timeoutIncrement = 5;

    private long waitedTime = 0;

    public SpringWebfluxSessionStore(final ServerWebExchange exchange) {
        exchange.getSession().subscribe(
                value -> {
                    LOGGER.debug("Retrieved session: {}", session);
                    session = value;
                    },
                error -> { throw new TechnicalException("Cannot get session"); },
                () -> {
                    LOGGER.debug("No session available");
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

    protected void waitForSession() {
        int currentTimeout = 0;
        while (session == null && currentTimeout <= timeout) {
            LOGGER.debug("WAITING for session, current timeout: {} ms ", currentTimeout);
            try {
                Thread.sleep(timeoutIncrement);
                currentTimeout += timeoutIncrement;
            } catch (final InterruptedException e) {
                LOGGER.debug("Aborted wait: {}", e.getMessage());
                return;
            }
        }
    }

    @Override
    public boolean destroySession(WebContext context) {
        return false;
    }

    @Override
    public Optional<Object> getTrackableSession(WebContext context) {
        return Optional.empty();
    }

    @Override
    public Optional<SessionStore> buildFromTrackableSession(WebContext context, Object trackableSession) {
        return Optional.empty();
    }

    @Override
    public boolean renewSession(WebContext context) {
        return false;
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

    @Override
    public String toString() {
        return CommonHelper.toNiceString(this.getClass(), "timeout", timeout, "timeoutIncrement", timeoutIncrement,
                "waitedTime", waitedTime);
    }
}
