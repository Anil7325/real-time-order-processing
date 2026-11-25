package dev.orders;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OrderEvents {
    private static final Logger log = LoggerFactory.getLogger(OrderEvents.class);
    private final OrderStore store;
    private final KafkaTemplate<String, String> kafka;

    OrderEvents(OrderStore store, KafkaTemplate<String, String> kafka) {
        this.store = store;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelay = 1000)
    void publishPending() throws InterruptedException, ExecutionException {
        for (UUID id : store.unpublished()) {
            kafka.send(OrderApplication.TOPIC, id.toString(), id.toString()).get();
            store.markPublished(id);
        }
    }

    @KafkaListener(topics = OrderApplication.TOPIC)
    void process(String value) {
        UUID id = UUID.fromString(value);
        if (store.process(id) == 1) {
            log.info("Processed order {}", id);
        }
    }
}
