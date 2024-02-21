package org.pac4j.springframework.context;

import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * <p>This is the specific <code>WebContext</code> for Spring Webflux.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
public class SpringWebfluxWebContext implements WebContext {
    public static final String SAML_BODY_ATTRIBUTE = "PAC4J_REQUEST_CONTENT";

    private final ServerWebExchange exchange;

    private final ServerHttpRequest request;

    private final ServerHttpResponse response;

    public SpringWebfluxWebContext(final ServerWebExchange exchange) {
        this.exchange = exchange;
        this.request = exchange.getRequest();
        this.response = exchange.getResponse();
    }

    public ServerHttpRequest getNativeRequest() {
        return request;
    }

    public ServerHttpResponse getNativeResponse() {
        return response;
    }

    @Override
    public Optional<String> getRequestParameter(final String name) {
        return Optional.ofNullable(request.getQueryParams().getFirst(name));
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        final Map<String, String[]> parameters = new HashMap<>();
        request.getQueryParams().entrySet().forEach(entry -> parameters.put(entry.getKey(), entry.getValue().toArray(new String[0])));
        return parameters;
    }

    @Override
    public Optional getRequestAttribute(final String name) {
        return Optional.ofNullable(exchange.getAttribute(name));
    }

    @Override
    public void setRequestAttribute(final String name, final Object value) {
        exchange.getAttributes().put(name, value);
    }

    @Override
    public Optional<String> getRequestHeader(final String name) {
        return Optional.ofNullable(request.getHeaders().getFirst(name));
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod().name();
    }

    @Override
    public String getRemoteAddr() {
        final InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            final InetAddress address = remoteAddress.getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
        }
        return null;
    }

    @Override
    public void setResponseHeader(final String name, final String value) {
        response.getHeaders().add(name, value);
    }

    @Override
    public Optional<String> getResponseHeader(final String name) {
        return Optional.ofNullable(response.getHeaders().getFirst(name));
    }

    @Override
    public void setResponseContentType(final String contentType) {
        setResponseHeader(HttpConstants.CONTENT_TYPE_HEADER, contentType);
    }

    @Override
    public String getServerName() {
        final InetSocketAddress address = request.getLocalAddress();
        if (address != null) {
            return address.getHostName();
        }
        return null;
    }

    @Override
    public int getServerPort() {
        final InetSocketAddress address = request.getLocalAddress();
        if (address != null) {
            return address.getPort();
        }
        return -1;
    }

    @Override
    public String getScheme() {
        return isSecure() ? "https" : "http";
    }

    @Override
    public boolean isSecure() {
        return request.getSslInfo() != null;
    }

    @Override
    public String getFullRequestURL() {
        return request.getURI().toString();
    }

    @Override
    public Collection<Cookie> getRequestCookies() {
        final List<Cookie> cookies = new ArrayList<>();
        request.getCookies().entrySet().forEach(entry -> {
            final List<HttpCookie> httpCookies = entry.getValue();
            for (HttpCookie httpCookie : httpCookies) {
                final Cookie cookie = new Cookie(httpCookie.getName(), httpCookie.getValue());
                cookies.add(cookie);
            }
        });
        return cookies;
    }

    @Override
    public void addResponseCookie(final Cookie cookie) {
        final String name = cookie.getName();
        final ResponseCookie responseCookie = ResponseCookie.from(name, cookie.getValue())
                .maxAge(cookie.getMaxAge()).domain(cookie.getDomain()).path(cookie.getPath())
                .secure(cookie.isSecure()).httpOnly(cookie.isHttpOnly()).sameSite(cookie.getSameSitePolicy())
                .build();
        response.getCookies().put(name, Collections.singletonList(responseCookie));
    }

    @Override
    public String getPath() {
        return request.getPath().value();
    }

    /**
     * Authentication mechanisms like SAML requires the request content
     * to extract relevant authentication parameters.
     * @return Callback request content.
     */
    @Override
    public  String getRequestContent() {
        final Map<String, Object> attributes = exchange.getAttributes();
        return (String) attributes.get(SAML_BODY_ATTRIBUTE);
    }
}
