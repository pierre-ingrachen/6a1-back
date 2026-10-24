package com.takima.backskeleton.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

public class CatalogCacheControlFilter extends OncePerRequestFilter {
    private final String cacheControl;

    public CatalogCacheControlFilter(Duration maxAge) {
        this.cacheControl = CacheControl.maxAge(maxAge).cachePublic().getHeaderValue();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.GET.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (HttpStatus.valueOf(response.getStatus()).is2xxSuccessful()) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
        }
    }
}
