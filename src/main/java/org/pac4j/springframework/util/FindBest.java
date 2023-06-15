package org.pac4j.springframework.util;

import org.pac4j.core.util.CommonHelper;

import java.util.function.Supplier;

/**
 * Syntactic sugar util class.
 *
 * @author Marvin Kienitz
 * @since 3.0.0
 */
public class FindBest {
    public static <T> T findBest(final T local, final Supplier<T> provider, final T defaultValue) {
        if (local != null) {
            return local;
        } else if (provider != null && provider.get() != null) {
            return provider.get();
        } else {
            CommonHelper.assertNotNull("defaultValue", defaultValue);
            return defaultValue;
        }
    }
}
