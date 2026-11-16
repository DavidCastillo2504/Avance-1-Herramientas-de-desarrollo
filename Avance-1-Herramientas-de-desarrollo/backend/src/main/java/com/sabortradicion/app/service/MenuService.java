package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.MenuItemRequest;
import com.sabortradicion.app.dto.MenuItemResponse;
import com.sabortradicion.app.entity.MenuItem;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.exception.ResourceNotFoundException;
import com.sabortradicion.app.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {
    static final String DEFAULT_EMOJI = "🍽️";

    private final MenuItemRepository menuItemRepository;

    public MenuService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> findMenu(String category) {
        var items = category == null || category.isBlank()
                ? menuItemRepository.findAllByOrderByCategoryAscNameAsc()
                : menuItemRepository.findByCategoryIgnoreCaseOrderByNameAsc(category.trim());
        return items.stream().map(MenuItemResponse::from).toList();
    }

    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        String name = request.name().trim();
        if (menuItemRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("Ya existe un plato llamado " + name);
        }
        MenuItem item = new MenuItem(name, request.category().trim(), request.price(), emoji(request.emoji()),
                request.description().trim(), request.available() == null || request.available(),
                blankToNull(request.imageUrl()));
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem item = find(id);
        String name = request.name().trim();
        if (menuItemRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException("Ya existe un plato llamado " + name);
        }
        item.update(name, request.category().trim(), request.price(), emoji(request.emoji()),
                request.description().trim(), request.available() == null ? item.isAvailable() : request.available(),
                blankToNull(request.imageUrl()));
        return MenuItemResponse.from(item);
    }

    @Transactional
    public MenuItemResponse changeAvailability(Long id, boolean available) {
        MenuItem item = find(id);
        item.changeAvailability(available);
        return MenuItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        MenuItem item = find(id);
        if (menuItemRepository.hasOrders(id)) {
            throw new BusinessRuleException(item.getName()
                    + " ya figura en pedidos registrados; márcalo como agotado en lugar de eliminarlo");
        }
        menuItemRepository.delete(item);
    }

    private MenuItem find(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plato no encontrado"));
    }

    private static String emoji(String value) {
        return value == null || value.isBlank() ? DEFAULT_EMOJI : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
