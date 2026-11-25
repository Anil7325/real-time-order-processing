package dev.orders;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventsTest {
    @Mock OrderStore store;
    @Mock KafkaTemplate<String, String> kafka;
    final UUID id = UUID.randomUUID();

    @Test
    void successfulPublishMarksOrderAsSent() throws Exception {
        when(store.unpublished()).thenReturn(List.of(id));
        when(kafka.send(OrderApplication.TOPIC, id.toString(), id.toString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        new OrderEvents(store, kafka).publishPending();

        verify(store).markPublished(id);
    }

    @Test
    void failedPublishLeavesOrderEligibleForRetry() {
        when(store.unpublished()).thenReturn(List.of(id));
        when(kafka.send(OrderApplication.TOPIC, id.toString(), id.toString()))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        assertThrows(ExecutionException.class, () -> new OrderEvents(store, kafka).publishPending());
        verify(store, never()).markPublished(id);
    }
}
