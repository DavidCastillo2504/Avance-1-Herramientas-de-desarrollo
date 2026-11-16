package com.sabortradicion.app.controller;

import com.sabortradicion.app.dto.MenuItemRequest;
import com.sabortradicion.app.dto.MenuItemResponse;
import com.sabortradicion.app.dto.UpdateAvailabilityRequest;
import com.sabortradicion.app.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** Público. */
    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> findMenu(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(menuService.findMenu(category));
    }

    /** Administración (requiere token). */
    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        MenuItemResponse created = menuService.create(request);
        return ResponseEntity.created(URI.create("/api/menu-items/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MenuItemResponse update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return menuService.update(id, request);
    }

    @PatchMapping("/{id}/availability")
    public MenuItemResponse changeAvailability(@PathVariable Long id,
                                               @Valid @RequestBody UpdateAvailabilityRequest request) {
        return menuService.changeAvailability(id, request.available());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
