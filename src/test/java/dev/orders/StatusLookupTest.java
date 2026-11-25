package dev.orders;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusLookupTest {
    @Mock OrderStore store;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    final UUID id = UUID.randomUUID();

    @Test
    void servesFinalStatusFromCache() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("order:status:" + id)).thenReturn("PROCESSED");

        assertEquals(Optional.of("PROCESSED"), new StatusLookup(redis, store).find(id));
        verifyNoInteractions(store);
    }

    @Test
    void servesPendingOrderFromDatabaseWhenRedisIsUnavailable() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("order:status:" + id)).thenThrow(new RedisConnectionFailureException("offline"));
        when(store.status(id)).thenReturn(Optional.of("PENDING"));

        assertEquals(Optional.of("PENDING"), new StatusLookup(redis, store).find(id));
        verify(values).get("order:status:" + id);
        verifyNoMoreInteractions(values);
    }
}
