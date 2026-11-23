package com.laundry.modules.service.presentation;

import com.laundry.modules.service.application.WashModeService;
import com.laundry.modules.service.application.dto.WashModeDetailResponse;
import com.laundry.modules.service.application.dto.WashModeRequest;
import com.laundry.modules.service.application.dto.WashModeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wash Modes", description = "Dynamic wash modes (Admin & Staff read, Admin write; no delete)")
@RestController
@RequestMapping("/api/wash-modes")
@RequiredArgsConstructor
public class WashModeController {

    private final WashModeService washModeService;

    @Operation(summary = "All wash modes incl. inactive, default first, then completionHours desc, name, id (Staff & Admin)")
    @GetMapping
    public List<WashModeResponse> list() {
        return washModeService.listAll().stream().map(WashModeResponse::from).toList();
    }

    @Operation(summary = "Wash mode detail with service prices (Staff & Admin)")
    @GetMapping("/{id}")
    public WashModeDetailResponse get(@PathVariable Long id) {
        return washModeService.get(id);
    }

    @Operation(summary = "Create wash mode (Admin only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public WashModeDetailResponse create(@Valid @RequestBody WashModeRequest request) {
        return washModeService.create(request);
    }

    @Operation(summary = "Update wash mode, also deactivate/reactivate via active (Admin only)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WashModeDetailResponse update(@PathVariable Long id, @Valid @RequestBody WashModeRequest request) {
        return washModeService.update(id, request);
    }
}
