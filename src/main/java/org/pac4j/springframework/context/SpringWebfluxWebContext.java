package org.pac4j.springframework.context;

import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
    /** The exchange attribute containing the raw HTTP request body. */
    public static final String REQUEST_BODY_ATTRIBUTE = "PAC4J_REQUEST_CONTENT";

    /**
     * @deprecated Use {@link #REQUEST_BODY_ATTRIBUTE} instead.
     */
    @Deprecated
    public static final String SAML_BODY_ATTRIBUTE = REQUEST_BODY_ATTRIBUTE;

    private final ServerWebExchange exchange;

    private final ServerHttpRequest request;

    private final ServerHttpResponse response;

    private MultiValueMap<String, String> requestParameters;

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
        return Optional.ofNullable(requestParameters().getFirst(name));
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        final Map<String, String[]> parameters = new HashMap<>();
        requestParameters().entrySet().forEach(entry -> parameters.put(entry.getKey(), entry.getValue().toArray(new String[0])));
        return parameters;
    }

    private MultiValueMap<String, String> requestParameters() {
        if (requestParameters == null) {
            final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.addAll(request.getQueryParams());
            final MultiValueMap<String, String> form = exchange.getAttribute(SpringWebfluxRequestBody.FORM_PARAMETERS_ATTRIBUTE);
            if (form != null) {
                parameters.addAll(form);
            }
            requestParameters = parameters;
        }
        return requestParameters;
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
        return request.getURI().getHost();
    }

    @Override
    public int getServerPort() {
        final int port = request.getURI().getPort();
        if (port != -1) {
            return port;
        }
        return "https".equalsIgnoreCase(getScheme()) ? HttpConstants.DEFAULT_HTTPS_PORT : HttpConstants.DEFAULT_HTTP_PORT;
    }

    @Override
    public String getScheme() {
        final String scheme = request.getURI().getScheme();
        return scheme != null ? scheme : (request.getSslInfo() != null ? "https" : "http");
    }

    @Override
    public boolean isSecure() {
        return "https".equalsIgnoreCase(getScheme());
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
        return (String) attributes.get(REQUEST_BODY_ATTRIBUTE);
    }
}
