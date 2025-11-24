package com.kkst.mycinema.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Cache names used in the application
    public static final String MOVIES_CACHE = "movies";
    public static final String SHOWS_CACHE = "shows";
    public static final String SHOW_SEATS_CACHE = "showSeats";
}

