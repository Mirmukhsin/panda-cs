package com.cs2hub.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter annotation to inject the currently authenticated user in Spring MVC controller methods.
 * Supports parameter types {@link AuthenticatedUser} (returns null for unauthenticated guests)
 * or {@code Optional<AuthenticatedUser>}.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
