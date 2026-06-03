package com.ecommerce.nivora.service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface RedisService {

    void set(String key, Object value);

    void set(String key, Object value, long timeout, TimeUnit timeUnit);

    Object get(String key);

    boolean hasKey(String key);

    void delete(String key);

    void delete(Collection<String> keys);

    void expire(String key, long timeout, TimeUnit timeUnit);

    Long getExpire(String key);
}
