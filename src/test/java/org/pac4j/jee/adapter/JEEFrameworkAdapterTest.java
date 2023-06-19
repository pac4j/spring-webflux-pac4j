package org.pac4j.jee.adapter;

import org.junit.jupiter.api.Test;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.springframework.context.SpringWebfluxSessionStoreFactory;
import org.pac4j.springframework.context.SpringWebfluxWebContextFactory;
import org.pac4j.springframework.http.SpringWebfluxHttpActionAdapter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class JEEFrameworkAdapterTest {

    @Test
    public void testCorrectInstance() {
        assertThat(FrameworkAdapter.INSTANCE).isExactlyInstanceOf(JEEFrameworkAdapter.class);
    }

    @Test
    public void testAppliesDefaultsCorrectly() {
        Config config = new Config();

        assertThat(config.getWebContextFactory()).isNull();
        assertThat(config.getSessionStoreFactory()).isNull();
        assertThat(config.getHttpActionAdapter()).isNull();

        FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);

        assertThat(config.getWebContextFactory()).isEqualTo(SpringWebfluxWebContextFactory.INSTANCE);
        assertThat(config.getSessionStoreFactory()).isEqualTo(SpringWebfluxSessionStoreFactory.INSTANCE);
        assertThat(config.getHttpActionAdapter()).isEqualTo(SpringWebfluxHttpActionAdapter.INSTANCE);
    }
}