package com.takima.backskeleton.config;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.time.Duration;

@Configuration
public class CatalogCacheConfig {
    private static final String[] CATALOG_PATHS = {"/seasons", "/teams", "/formations"};

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> catalogEtagFilter() {
        ShallowEtagHeaderFilter etagFilter = new ShallowEtagHeaderFilter();
        etagFilter.setWriteWeakETag(true);
        return registration(etagFilter, Ordered.LOWEST_PRECEDENCE - 1);
    }

    @Bean
    public FilterRegistrationBean<CatalogCacheControlFilter> catalogCacheControlFilter(
            @Value("${catalog.cache.max-age:PT1H}") Duration maxAge) {
        return registration(new CatalogCacheControlFilter(maxAge), Ordered.LOWEST_PRECEDENCE);
    }

    private <T extends Filter> FilterRegistrationBean<T> registration(T filter, int order) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns(CATALOG_PATHS);
        registration.setOrder(order);
        return registration;
    }
}
