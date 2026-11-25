package dev.orders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class OrderStore {
    private final JdbcClient db;

    OrderStore(JdbcClient db) {
        this.db = db;
    }

    UUID create(String sku, int quantity) {
        UUID id = UUID.randomUUID();
        db.sql("INSERT INTO orders(id, sku, quantity, status) VALUES (?, ?, ?, 'PENDING')")
            .params(id, sku, quantity).update();
        return id;
    }

    Optional<String> status(UUID id) {
        return db.sql("SELECT status FROM orders WHERE id = ?").param(id).query(String.class).optional();
    }

    List<UUID> unpublished() {
        return db.sql("SELECT id FROM orders WHERE NOT published ORDER BY created_at LIMIT 50")
            .query(UUID.class).list();
    }

    void markPublished(UUID id) {
        db.sql("UPDATE orders SET published = TRUE WHERE id = ?").param(id).update();
    }

    int process(UUID id) {
        return db.sql("UPDATE orders SET status = 'PROCESSED' WHERE id = ? AND status = 'PENDING'")
            .param(id).update();
    }
}
