package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.MenuItemRequest;
import com.sabortradicion.app.entity.MenuItem;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {
    @Mock
    private MenuItemRepository repository;
    private MenuService service;

    @BeforeEach
    void setUp() {
        service = new MenuService(repository);
    }

    private MenuItemRequest request(String name, String emoji, Boolean available, String imageUrl) {
        return new MenuItemRequest(name, " Criollos ", new BigDecimal("25.50"), emoji, " Rico plato ", available, imageUrl);
    }

    @Test
    void createsADishWithDefaultsAndTrimmedValues() {
        when(repository.existsByNameIgnoreCase("Tacu tacu")).thenReturn(false);
        when(repository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request("  Tacu tacu ", " ", null, "  "));

        assertThat(response.name()).isEqualTo("Tacu tacu");
        assertThat(response.category()).isEqualTo("Criollos");
        assertThat(response.emoji()).isEqualTo(MenuService.DEFAULT_EMOJI);
        assertThat(response.available()).isTrue();
        assertThat(response.imageUrl()).isNull();
    }

    @Test
    void rejectsDuplicatedNames() {
        when(repository.existsByNameIgnoreCase("Lomo saltado")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Lomo saltado", "🥩", true, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya existe");
        verify(repository, never()).save(any());
    }

    @Test
    void updatesTheImageUrlAndKeepsAvailabilityWhenNotSent() {
        MenuItem item = new MenuItem("Ceviche", "Marinos", new BigDecimal("30.00"), "🐟", "Fresco", false, null);
        when(repository.findById(3L)).thenReturn(Optional.of(item));
        when(repository.existsByNameIgnoreCaseAndIdNot("Ceviche", 3L)).thenReturn(false);

        var response = service.update(3L, request("Ceviche", "🐟", null,
                "https://bucket.s3.us-east-1.amazonaws.com/menu/ceviche.jpg"));

        assertThat(response.available()).isFalse();
        assertThat(response.imageUrl()).endsWith("menu/ceviche.jpg");
    }

    @Test
    void doesNotDeleteDishesThatAppearInOrders() {
        MenuItem item = new MenuItem("Ceviche", "Marinos", new BigDecimal("30.00"), "🐟", "Fresco", true, null);
        when(repository.findById(3L)).thenReturn(Optional.of(item));
        when(repository.hasOrders(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("agotado");
        verify(repository, never()).delete(any());
    }
}
