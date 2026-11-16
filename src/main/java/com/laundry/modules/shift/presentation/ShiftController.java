package com.laundry.modules.shift.presentation;

import com.laundry.modules.shift.application.ShiftService;
import com.laundry.modules.shift.application.dto.ApproveShiftRequest;
import com.laundry.modules.shift.application.dto.CloseShiftRequest;
import com.laundry.modules.shift.application.dto.CurrentShiftResponse;
import com.laundry.modules.shift.application.dto.OpenShiftRequest;
import com.laundry.modules.shift.application.dto.ShiftDetailResponse;
import com.laundry.modules.shift.application.dto.ShiftRowResponse;
import com.laundry.modules.shift.application.dto.ShiftSummaryResponse;
import com.laundry.modules.shift.domain.ShiftStatus;
import com.laundry.modules.shift.application.dto.ShiftTransactionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shifts")
@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'EMPLOYEE')")
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping("/current")
    public CurrentShiftResponse current() {
        return shiftService.current();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftDetailResponse open(@Valid @RequestBody OpenShiftRequest request,
                                    Authentication authentication,
                                    HttpServletRequest httpRequest) {
        return shiftService.open(request, authentication.getName(), httpRequest.getRemoteAddr());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ShiftRowResponse> list(@RequestParam String from, @RequestParam String to,
                                       @RequestParam(required = false) ShiftStatus status,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return shiftService.list(from, to, status, pageable);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ShiftSummaryResponse summary(@RequestParam String from, @RequestParam String to) {
        return shiftService.summary(from, to);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ShiftDetailResponse approve(@PathVariable long id,
                                       @Valid @RequestBody ApproveShiftRequest request,
                                       Authentication authentication,
                                       HttpServletRequest httpRequest) {
        return shiftService.approve(id, request, authentication.getName(), httpRequest.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public ShiftDetailResponse detail(@PathVariable long id, Authentication authentication) {
        return shiftService.detail(id, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{id}/transactions")
    public Page<ShiftTransactionResponse> transactions(@PathVariable long id,
                                                       @PageableDefault(size = 20) Pageable pageable,
                                                       Authentication authentication) {
        return shiftService.transactions(id, pageable, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{id}/close")
    public ShiftDetailResponse close(@PathVariable long id,
                                     @Valid @RequestBody CloseShiftRequest request,
                                     Authentication authentication,
                                     HttpServletRequest httpRequest) {
        return shiftService.close(id, request, authentication.getName(), isAdmin(authentication),
                httpRequest.getRemoteAddr());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
