package org.pac4j.framework.adapter;

import org.pac4j.core.adapter.DefaultFrameworkAdapter;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.springframework.context.SpringWebfluxSessionStoreFactory;
import org.pac4j.springframework.context.SpringWebfluxWebContextFactory;
import org.pac4j.springframework.http.SpringWebfluxHttpActionAdapter;

/**
 * The WebFlux framework adapter.
 * <p>
 * Needs to be called "FrameworkAdapterImpl",
 * so that static initializer of {@link FrameworkAdapter} can discover it.
 *
 * @author Marvin Kienitz
 * @since 3.0.0
 */
public class FrameworkAdapterImpl extends DefaultFrameworkAdapter {

    /** {@inheritDoc} */
    @Override
    public void applyDefaultSettingsIfUndefined(final Config config) {
        super.applyDefaultSettingsIfUndefined(config);

        config.setWebContextFactoryIfUndefined(SpringWebfluxWebContextFactory.INSTANCE);
        config.setSessionStoreFactoryIfUndefined(SpringWebfluxSessionStoreFactory.INSTANCE);
        config.setHttpActionAdapterIfUndefined(SpringWebfluxHttpActionAdapter.INSTANCE);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "SpringWebFlux";
    }
}