package com.laundry.modules.machine.presentation;

import com.laundry.modules.machine.application.MachineService;
import com.laundry.modules.machine.application.dto.ChangeMachineStatusRequest;
import com.laundry.modules.machine.application.dto.MachineRequest;
import com.laundry.modules.machine.application.dto.MachineResponse;
import com.laundry.modules.machine.application.dto.RetireMachineRequest;
import com.laundry.modules.machine.domain.MachineStatus;
import com.laundry.modules.machine.domain.MachineType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Machine", description = "Máy giặt & máy sấy (UC14, UC18 / BM8 / QĐ10)")
@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @Operation(summary = "Danh sách máy, lọc theo loại và trạng thái")
    @GetMapping
    public List<MachineResponse> list(@RequestParam(required = false) MachineType type,
                                      @RequestParam(required = false) MachineStatus status) {
        return machineService.list(type, status);
    }

    @Operation(summary = "Chi tiết máy")
    @GetMapping("/{id}")
    public MachineResponse get(@PathVariable Long id) {
        return machineService.get(id);
    }

    @Operation(summary = "Thêm máy (Admin). Mã MGxx / MSxx sinh tự động")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MachineResponse create(@Valid @RequestBody MachineRequest request) {
        return machineService.create(request);
    }

    @Operation(summary = "Sửa thông tin máy (Admin)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MachineResponse update(@PathVariable Long id, @Valid @RequestBody MachineRequest request) {
        return machineService.update(id, request);
    }

    @Operation(summary = "Ngừng sử dụng máy (Admin). Không có xóa")
    @PatchMapping("/{id}/retire")
    @PreAuthorize("hasRole('ADMIN')")
    public MachineResponse retire(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) RetireMachineRequest request) {
        return machineService.retire(id, request);
    }

    @Operation(summary = "Chuyển Sẵn sàng ⇄ Bảo trì (NV và Admin). Bắt buộc lý do khi chuyển sang Bảo trì")
    @PatchMapping("/{id}/status")
    public MachineResponse changeStatus(@PathVariable Long id,
                                        @Valid @RequestBody ChangeMachineStatusRequest request) {
        return machineService.changeStatus(id, request);
    }
}
