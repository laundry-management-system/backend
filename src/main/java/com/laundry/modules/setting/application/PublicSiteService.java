package com.laundry.modules.setting.application;

import com.laundry.modules.service.application.LaundryServiceService;
import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.setting.application.dto.PublicBookingSettingsResponse;
import com.laundry.modules.setting.application.dto.PublicServiceAreaResponse;
import com.laundry.modules.setting.application.dto.PublicServiceResponse;
import com.laundry.modules.setting.application.dto.PublicStoreResponse;
import com.laundry.modules.setting.domain.ServiceAreaRepository;
import com.laundry.modules.setting.domain.SettingKey;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Dữ liệu cho trang chủ công khai, đọc từ snapshot trong bộ nhớ để lượt truy cập dồn dập không chạm DB (R5). */
@Service
@RequiredArgsConstructor
public class PublicSiteService {

    private static final Duration TTL = Duration.ofSeconds(60);

    private final SettingService settingService;
    private final LaundryServiceService laundryServiceService;
    private final ServiceAreaRepository serviceAreaRepository;

    // shortcut: cache trong 1 JVM, chạy nhiều instance thì chuyển sang Redis có sẵn
    // Không khoá: hai luồng có thể cùng nạp lại khi hết hạn, chấp nhận được vì dữ liệu chỉ đọc
    private volatile Snapshot snapshot;

    private record Snapshot(PublicStoreResponse store, List<PublicServiceResponse> services, List<PublicServiceAreaResponse> areas, PublicBookingSettingsResponse bookingSettings, Instant loadedAt) {}

    public PublicStoreResponse getStore() {
        return current().store();
    }

    public List<PublicServiceResponse> getServices() {
        return current().services();
    }

    public List<PublicServiceAreaResponse> getServiceAreas() {
        return current().areas();
    }

    public PublicBookingSettingsResponse getBookingSettings() {
        return current().bookingSettings();
    }

    private Snapshot current() {
        Snapshot s = snapshot;
        if (s == null || s.loadedAt().plus(TTL).isBefore(Instant.now())) {
            s = load();
            snapshot = s;
        }
        return s;
    }

    private Snapshot load() {
        PublicStoreResponse store = new PublicStoreResponse(
                settingService.getValue(SettingKey.STORE_NAME, String.class),
                settingService.getValue(SettingKey.STORE_ADDRESS, String.class),
                settingService.getValue(SettingKey.STORE_PHONE, String.class),
                // TIME parse theo HH:mm nên toString() luôn ra HH:mm
                settingService.getValue(SettingKey.OPENING_TIME, LocalTime.class).toString(),
                settingService.getValue(SettingKey.CLOSING_TIME, LocalTime.class).toString(),
                settingService.getValue(SettingKey.WHITES_FEE, Integer.class),
                settingService.getValue(SettingKey.COMPENSATION_MULTIPLIER, Integer.class),
                settingService.getValue(SettingKey.COMPENSATION_CAP_PER_ITEM, Integer.class),
                settingService.getValue(SettingKey.STORAGE_DAYS, Integer.class));

        // listActiveServices sắp theo serviceCode; bảng giá admin sắp theo id (FR-016)
        List<PublicServiceResponse> services = laundryServiceService.listActiveServices().stream()
                .sorted(Comparator.comparing(ServiceResponse::id))
                .map(PublicServiceResponse::from)
                .toList();

        List<PublicServiceAreaResponse> areas = serviceAreaRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(PublicServiceAreaResponse::from)
                .toList();

        PublicBookingSettingsResponse booking = new PublicBookingSettingsResponse(
                settingService.getValue(SettingKey.BOOKING_ENABLED, Boolean.class),
                settingService.getValue(SettingKey.BOOKING_DELIVERY_ENABLED, Boolean.class),
                settingService.getValue(SettingKey.BOOKING_MIN_LEAD_HOURS, Integer.class),
                settingService.getValue(SettingKey.BOOKING_RETURN_GAP_HOURS_NORMAL, Integer.class),
                settingService.getValue(SettingKey.BOOKING_RETURN_GAP_HOURS_EXPRESS, Integer.class));

        return new Snapshot(store, services, areas, booking, Instant.now());
    }
}
