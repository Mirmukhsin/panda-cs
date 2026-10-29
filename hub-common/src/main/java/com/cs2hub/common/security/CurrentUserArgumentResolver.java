package com.cs2hub.common.security;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }

        Class<?> paramType = parameter.getParameterType();
        if (AuthenticatedUser.class.isAssignableFrom(paramType)) {
            return true;
        }

        if (Optional.class.isAssignableFrom(paramType)) {
            ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
            Class<?> genericType = resolvableType.getGeneric(0).resolve();
            return genericType != null && AuthenticatedUser.class.isAssignableFrom(genericType);
        }

        return false;
    }

    @Override
    @Nullable
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AuthenticatedUser user = null;
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            user = principal;
        }

        if (Optional.class.isAssignableFrom(parameter.getParameterType())) {
            return Optional.ofNullable(user);
        }

        return user;
    }
}
