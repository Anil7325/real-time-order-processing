package dev.orders;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
class StatusLookup {
    private static final Logger log = LoggerFactory.getLogger(StatusLookup.class);
    private final StringRedisTemplate redis;
    private final OrderStore store;

    StatusLookup(StringRedisTemplate redis, OrderStore store) {
        this.redis = redis;
        this.store = store;
    }

    Optional<String> find(UUID id) {
        String key = "order:status:" + id;
        try {
            if ("PROCESSED".equals(redis.opsForValue().get(key))) {
                return Optional.of("PROCESSED");
            }
        } catch (DataAccessException ex) {
            log.warn("Cache read failed for {}", id);
        }

        Optional<String> status = store.status(id);
        if (status.filter("PROCESSED"::equals).isPresent()) {
            try {
                redis.opsForValue().set(key, "PROCESSED", Duration.ofMinutes(10));
            } catch (DataAccessException ex) {
                log.warn("Cache write failed for {}", id);
            }
        }
        return status;
    }
}
