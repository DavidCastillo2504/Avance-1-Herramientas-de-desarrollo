package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.CreateOrderItemRequest;
import com.sabortradicion.app.dto.CreateOrderRequest;
import com.sabortradicion.app.entity.CustomerOrder;
import com.sabortradicion.app.entity.MenuItem;
import com.sabortradicion.app.entity.OrderType;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.repository.CustomerOrderRepository;
import com.sabortradicion.app.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private MenuItem menuItem;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, menuItemRepository);
    }

    @Test
    void calculatesTheTotalUsingDatabasePrices() {
        when(menuItem.getId()).thenReturn(1L);
        when(menuItem.getName()).thenReturn("Seco de cabrito");
        when(menuItem.getPrice()).thenReturn(new BigDecimal("28.00"));
        when(menuItem.isAvailable()).thenReturn(true);
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(menuItem));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateOrderRequest("Ana", "999 111 222", OrderType.TAKEAWAY, null,
                List.of(new CreateOrderItemRequest(1L, 2)));

        var response = orderService.create(request);

        assertThat(response.total()).isEqualByComparingTo("56.00");
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void rejectsUnavailableItems() {
        when(menuItem.getId()).thenReturn(1L);
        when(menuItem.getName()).thenReturn("Sudado de pescado");
        when(menuItem.isAvailable()).thenReturn(false);
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(menuItem));

        var request = new CreateOrderRequest("Ana", "999111222", OrderType.DINE_IN, "Sin ají",
                List.of(new CreateOrderItemRequest(1L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está disponible");
    }
}
