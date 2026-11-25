package dev.orders;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {
    final OrderStore store = mock(OrderStore.class);
    final StatusLookup lookup = mock(StatusLookup.class);
    final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OrderController(store, lookup)).build();

    @Test
    void createsAnOrderAndReturnsStatusUrl() throws Exception {
        UUID id = UUID.randomUUID();
        when(store.create("BOOK-1", 2)).thenReturn(id);
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(""" 
                    {"sku":"BOOK-1","quantity":2}
                    """))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/orders/" + id))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsZeroQuantity() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sku":"BOOK-1","quantity":0}
                    """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(store);
    }

    @Test
    void missingOrderReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(lookup.find(id)).thenReturn(Optional.empty());
        mvc.perform(get("/orders/" + id)).andExpect(status().isNotFound());
    }
}
