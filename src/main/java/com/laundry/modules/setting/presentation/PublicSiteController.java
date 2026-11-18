package com.laundry.modules.setting.presentation;

import com.laundry.modules.setting.application.PublicSiteService;
import com.laundry.modules.setting.application.dto.PublicServiceAreaResponse;
import com.laundry.modules.setting.application.dto.PublicServiceResponse;
import com.laundry.modules.setting.application.dto.PublicStoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Chỉ GET; SecurityConfig mở đúng từng path cho khách chưa đăng nhập (FR-036, FR-038)
@Tag(name = "Public site", description = "Dữ liệu công khai cho trang chủ, không cần đăng nhập")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicSiteController {

    private static final CacheControl CACHE = CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic();

    private final PublicSiteService publicSiteService;

    @Operation(summary = "Thông tin cửa hàng công khai (tên, địa chỉ, SĐT, giờ mở cửa, quy định)")
    @GetMapping("/store")
    public ResponseEntity<PublicStoreResponse> store() {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicSiteService.getStore());
    }

    @Operation(summary = "Bảng giá dịch vụ đang hoạt động")
    @GetMapping("/services")
    public ResponseEntity<List<PublicServiceResponse>> services() {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicSiteService.getServices());
    }

    @Operation(summary = "Khu vực đang phục vụ và phí giao nhận")
    @GetMapping("/service-areas")
    public ResponseEntity<List<PublicServiceAreaResponse>> serviceAreas() {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicSiteService.getServiceAreas());
    }
}
