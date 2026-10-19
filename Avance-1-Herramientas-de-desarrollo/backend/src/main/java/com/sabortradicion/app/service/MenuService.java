package com.sabortradicion.app.service;

import com.sabortradicion.app.dto.MenuItemResponse;
import com.sabortradicion.app.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {
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
}
