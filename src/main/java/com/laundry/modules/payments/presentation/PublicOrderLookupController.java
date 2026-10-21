package com.laundry.modules.payments.presentation;

import com.laundry.modules.payments.application.PublicOrderLookupService;
import com.laundry.modules.payments.application.dto.PublicOrderLookupRequest;
import com.laundry.modules.payments.application.dto.PublicOrderLookupResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public order lookup")
@RestController
@RequestMapping("/api/public/orders")
@RequiredArgsConstructor
public class PublicOrderLookupController {

    private final PublicOrderLookupService lookupService;

    @PostMapping("/lookup")
    public PublicOrderLookupResponse lookup(@Valid @RequestBody PublicOrderLookupRequest request,
                                            HttpServletRequest httpRequest) {
        return lookupService.lookup(request, httpRequest.getRemoteAddr());
    }
}
