package org.pac4j.springframework.http;

import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.springframework.context.SpringWebfluxWebContext;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * <p>This is the specific <code>HttpActionAdapter</code> for Spring Webflux.</p>
 *
 * @author Jerome Leleu
 * @since 1.0.0
 */
public class SpringWebfluxHttpActionAdapter implements HttpActionAdapter {

    public static final SpringWebfluxHttpActionAdapter INSTANCE = new SpringWebfluxHttpActionAdapter();

    @Override
    public Object adapt(final HttpAction action, final WebContext context) {
        if (action != null) {
            var code = action.getCode();
            final var response = ((SpringWebfluxWebContext) context).getNativeResponse();
            response.setRawStatusCode(code);

            if (action instanceof WithLocationAction) {
                final var withLocationAction = (WithLocationAction) action;
                context.setResponseHeader(HttpConstants.LOCATION_HEADER, withLocationAction.getLocation());

            } else if (action instanceof WithContentAction) {
                final var withContentAction = (WithContentAction) action;
                final var content = withContentAction.getContent();

                if (content != null) {
                    final DataBuffer data = response.bufferFactory().wrap(content.getBytes(StandardCharsets.UTF_8));
                    response.writeWith(Mono.just(data));
                }
            }

            return null;
        }

        throw new TechnicalException("No action provided");
    }
}
