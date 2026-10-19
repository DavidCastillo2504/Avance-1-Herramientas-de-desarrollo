package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.CreateOrderRequest;
import com.sabortradicion.app.dto.DashboardResponse;
import com.sabortradicion.app.dto.OrderResponse;
import com.sabortradicion.app.entity.CustomerOrder;
import com.sabortradicion.app.entity.MenuItem;
import com.sabortradicion.app.entity.OrderItem;
import com.sabortradicion.app.entity.OrderStatus;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.exception.ResourceNotFoundException;
import com.sabortradicion.app.repository.CustomerOrderRepository;
import com.sabortradicion.app.repository.MenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final CustomerOrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderService(CustomerOrderRepository orderRepository, MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        request.items().forEach(item -> quantities.merge(item.menuItemId(), item.quantity(), Integer::sum));
        if (quantities.values().stream().anyMatch(quantity -> quantity > 20)) {
            throw new BusinessRuleException("La cantidad máxima por plato es 20");
        }

        Map<Long, MenuItem> menuById = menuItemRepository.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        if (menuById.size() != quantities.size()) {
            throw new ResourceNotFoundException("Uno de los platos seleccionados ya no existe");
        }

        CustomerOrder order = new CustomerOrder(request.customerName().trim(), request.phone().trim(),
                request.orderType(), normalizeNotes(request.notes()));
        quantities.forEach((menuItemId, quantity) -> {
            MenuItem menuItem = menuById.get(menuItemId);
            if (!menuItem.isAvailable()) {
                throw new BusinessRuleException(menuItem.getName() + " no está disponible en este momento");
            }
            order.addItem(new OrderItem(menuItem, quantity));
        });

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(int page, int size) {
        return orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 50)))
                .map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse changeStatus(java.util.UUID id, OrderStatus status) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        order.changeStatus(status);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        return new DashboardResponse(orderRepository.count(), menuItemRepository.countByAvailableTrue(),
                orderRepository.sumRegisteredSales());
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
