package com.laundry.modules.service.application;

import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditLogDetailResponse;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.AuditLogFilter;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.service.application.dto.PriceHistoryRowResponse;
import com.laundry.modules.service.application.dto.PriceIssuedResponse;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.PriceField;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc lịch sử thay đổi giá từ nhật ký hệ thống (action PRICE_CHANGE), không có bảng riêng. */
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final AuditLogService auditLogService;
    private final LaundryServiceRepository serviceRepository;

    // shortcut: lọc loại thay đổi + phân trang trong bộ nhớ, đủ cho vài trăm lần đổi / năm; vượt vài nghìn bản ghi mỗi kỳ thì chuyển native SQL jsonb_each
    @Transactional(readOnly = true)
    public Page<PriceHistoryRowResponse> list(
            Long serviceId, Long userId, PriceField field, Instant from, Instant to, int page, int size) {
        List<AuditLogDetailResponse> logs = auditLogService.listAll(
                new AuditLogFilter(AuditAction.PRICE_CHANGE, EntityType.SERVICE, serviceId, userId, from, to));

        Map<Long, LaundryService> services = serviceRepository
                .findAllById(logs.stream().map(AuditLogDetailResponse::entityId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(LaundryService::getId, Function.identity()));

        List<PriceHistoryRowResponse> rows = logs.stream()
                .flatMap(log -> toRows(log, services.get(log.entityId())))
                .filter(row -> field == null || row.field() == field)
                .toList();

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        int start = (int) Math.min(pageable.getOffset(), rows.size());
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }

    @Transactional(readOnly = true)
    public PriceIssuedResponse latestIssued() {
        return auditLogService.listAll(
                        new AuditLogFilter(AuditAction.PRICE_CHANGE, EntityType.SERVICE, null, null, null, null))
                .stream()
                .filter(log -> log.newData() != null
                        && (log.newData().containsKey(PriceField.PRICE.key())
                        || log.newData().containsKey(PriceField.EXPRESS_SURCHARGE.key())))
                .findFirst()
                .map(log -> new PriceIssuedResponse(log.createdAt(), log.fullName()))
                .orElse(new PriceIssuedResponse(null, null));
    }

    private static Stream<PriceHistoryRowResponse> toRows(AuditLogDetailResponse log, LaundryService service) {
        Map<String, Object> oldData = log.oldData() != null ? log.oldData() : Map.of();
        Map<String, Object> newData = log.newData() != null ? log.newData() : Map.of();
        return Stream.of(PriceField.values())
                .filter(f -> oldData.containsKey(f.key()) || newData.containsKey(f.key()))
                .map(f -> new PriceHistoryRowResponse(
                        log.id(),
                        log.createdAt(),
                        log.entityId(),
                        service != null ? service.getServiceCode() : null,
                        service != null ? service.getName() : "—",
                        service != null ? service.getUnit() : null,
                        f,
                        text(oldData.get(f.key())),
                        text(newData.get(f.key())),
                        log.userId(),
                        log.fullName(),
                        log.reason()));
    }

    // jsonb trả số dạng Integer / Long / BigDecimal / Double tùy giá trị; quy về chuỗi gọn "25000"
    private static String text(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
        return value.toString();
    }
}
