package dev.orders;

import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
class OrderController {
    record NewOrder(@NotBlank @Size(max = 80) String sku, @Positive int quantity) {}
    record OrderStatus(UUID id, String status) {}

    private final OrderStore store;
    private final StatusLookup lookup;

    OrderController(OrderStore store, StatusLookup lookup) {
        this.store = store;
        this.lookup = lookup;
    }

    @PostMapping
    ResponseEntity<OrderStatus> create(@Valid @RequestBody NewOrder request) {
        UUID id = store.create(request.sku().strip(), request.quantity());
        return ResponseEntity.created(URI.create("/orders/" + id)).body(new OrderStatus(id, "PENDING"));
    }

    @GetMapping("/{id}")
    ResponseEntity<OrderStatus> status(@PathVariable UUID id) {
        return ResponseEntity.of(lookup.find(id).map(status -> new OrderStatus(id, status)));
    }
}
