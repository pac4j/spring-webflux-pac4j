package org.pac4j.springframework.web;

import org.springframework.core.annotation.AliasFor;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring Boot 4.x {@link WebFluxTest}, so that the same tests run with Spring Boot 3 and 4.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebFluxTest
public @interface WebFluxSliceTest {

    @AliasFor(annotation = WebFluxTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};
}
