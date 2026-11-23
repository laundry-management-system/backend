package com.laundry.modules.incident.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.exception.ResourceConflictException;
import com.laundry.modules.document.application.DocumentService;
import com.laundry.modules.document.application.dto.DocumentResponse;
import com.laundry.modules.document.domain.Document;
import com.laundry.modules.incident.application.dto.ApproveIncidentRequest;
import com.laundry.modules.incident.application.dto.CreateIncidentRequest;
import com.laundry.modules.incident.application.dto.IncidentDetailResponse;
import com.laundry.modules.incident.application.dto.IncidentStatsResponse;
import com.laundry.modules.incident.application.dto.IncidentSummaryResponse;
import com.laundry.modules.incident.application.dto.PayoutIncidentRequest;
import com.laundry.modules.incident.application.dto.RejectIncidentRequest;
import com.laundry.modules.incident.application.dto.UpdateIncidentRequest;
import com.laundry.modules.incident.domain.Incident;
import com.laundry.modules.incident.domain.IncidentFilter;
import com.laundry.modules.incident.domain.IncidentImage;
import com.laundry.modules.incident.domain.IncidentMath;
import com.laundry.modules.incident.domain.IncidentRepository;
import com.laundry.modules.incident.domain.IncidentResolution;
import com.laundry.modules.incident.domain.IncidentSearchQuery;
import com.laundry.modules.incident.domain.IncidentStatus;
import com.laundry.modules.incident.domain.IncidentType;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderImage;
import com.laundry.modules.order.domain.OrderItem;
import com.laundry.modules.order.domain.OrderRepository;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.application.CodeSequenceService;
import com.laundry.modules.setting.application.SettingService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_IMAGES = 6;

    private final IncidentRepository incidentRepository;
    private final IncidentSearchQuery incidentSearchQuery;
    private final OrderRepository orderRepository;
    private final DocumentService documentService;
    private final SettingService settingService;
    private final CodeSequenceService codeSequenceService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public IncidentDetailResponse create(CreateIncidentRequest request, String username) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", request.orderId()));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Không lập phiếu cho đơn đã hủy", "orderId");
        }
        if (order.getStatus().isBeforeDropoff()) {
            throw new BusinessException(Order.BEFORE_DROPOFF_MESSAGE, "orderId");
        }
        Incident incident = Incident.builder()
                .code(codeSequenceService.getNextIncidentCode())
                .order(order)
                .createdBy(username)
                .status(IncidentStatus.PENDING)
                .build();
        applyContent(incident, request.orderItemId(), request.affectedCount(), request.type(),
                request.description(), request.resolution(), request.amount(), request.imageIds());
        return toDetail(incidentRepository.saveAndFlush(incident));
    }

    @Transactional
    public IncidentDetailResponse update(Long id, UpdateIncidentRequest request) {
        Incident incident = lockIncident(id);
        incident.requirePending();
        applyContent(incident, request.orderItemId(), request.affectedCount(), request.type(),
                request.description(), request.resolution(), request.amount(), request.imageIds());
        return toDetail(incidentRepository.saveAndFlush(incident));
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse get(Long id) {
        return toDetail(findIncident(id));
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> list(IncidentStatus status, String q, IncidentType type,
                                              LocalDate createdFrom, LocalDate createdTo, Long orderId,
                                              Pageable pageable) {
        return incidentSearchQuery.search(filter(status, q, type, createdFrom, createdTo, orderId), pageable)
                .map(IncidentSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public IncidentStatsResponse stats(String q, IncidentType type, LocalDate createdFrom, LocalDate createdTo) {
        IncidentSearchQuery.Stats stats = incidentSearchQuery.stats(filter(null, q, type, createdFrom, createdTo, null));
        long pending = stats.counts().get(IncidentStatus.PENDING);
        long approved = stats.counts().get(IncidentStatus.APPROVED);
        long rejected = stats.counts().get(IncidentStatus.REJECTED);
        return new IncidentStatsResponse(pending, approved, rejected, pending + approved + rejected);
    }

    @Transactional
    public IncidentDetailResponse approve(Long id, ApproveIncidentRequest request, String username) {
        Incident incident = lockIncident(id);
        requireUnreviewed(incident);
        IncidentResolution resolution = request.resolution();
        if (resolution != IncidentResolution.REWASH && incident.getOrderItemId() == null) {
            throw new BusinessException("Chọn món đồ liên quan trước khi duyệt giảm giá hoặc bồi thường", "resolution");
        }
        BigDecimal requested = request.amount() == null ? BigDecimal.ZERO : request.amount();
        boolean overLimit = resolution == IncidentResolution.COMPENSATION
                && requested.compareTo(incident.getMaxCompensation()) > 0;
        String overLimitReason = trimToNull(request.overLimitReason());
        if (overLimit && overLimitReason == null) {
            throw new BusinessException("Nhập lý do duyệt vượt mức", "overLimitReason");
        }
        BigDecimal amount = IncidentMath.normalizeAmount(resolution, requested, incident.getLineTotal(),
                incident.getMaxCompensation(), true);

        Map<String, Object> oldData = reviewSnapshot(incident);
        Order order = orderRepository.findByIdForUpdate(incident.getOrder().getId())
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", incident.getOrder().getId()));
        BigDecimal deducted = order.applyIncidentDiscount(amount, incident.getCode(), username);
        orderRepository.save(order);

        incident.setResolution(resolution);
        incident.setAmount(amount);
        incident.setDeductedAmount(deducted);
        incident.setPayoutDue(amount.subtract(deducted));
        incident.setStatus(IncidentStatus.APPROVED);
        incident.setReviewedBy(username);
        incident.setReviewedAt(Instant.now());
        incident.setReviewNote(trimToNull(request.note()));
        incident.setOverLimitReason(overLimit ? overLimitReason : null);
        Incident saved = incidentRepository.saveAndFlush(incident);

        Map<String, Object> newData = reviewSnapshot(saved);
        newData.put("deductedAmount", deducted);
        newData.put("payoutDue", saved.getPayoutDue());
        logReview(saved, oldData, newData, overLimit ? overLimitReason : saved.getReviewNote());
        return toDetail(saved);
    }

    @Transactional
    public IncidentDetailResponse reject(Long id, RejectIncidentRequest request, String username) {
        Incident incident = lockIncident(id);
        requireUnreviewed(incident);
        Map<String, Object> oldData = reviewSnapshot(incident);
        incident.setStatus(IncidentStatus.REJECTED);
        incident.setReviewedBy(username);
        incident.setReviewedAt(Instant.now());
        incident.setReviewNote(request.note().trim());
        Incident saved = incidentRepository.saveAndFlush(incident);
        logReview(saved, oldData, reviewSnapshot(saved), saved.getReviewNote());
        return toDetail(saved);
    }

    @Transactional
    public IncidentDetailResponse payout(Long id, PayoutIncidentRequest request, String username) {
        Incident incident = lockIncident(id);
        if (incident.getStatus() != IncidentStatus.APPROVED || incident.getPayoutDue().signum() <= 0) {
            throw new BusinessException("Phiếu không có khoản cần chi");
        }
        if (incident.getPaidAt() != null) {
            throw new ResourceConflictException("Phiếu đã ghi nhận chi bồi thường");
        }
        Object[] shift = lockOpenShift();
        long shiftId = ((Number) shift[0]).longValue();
        String shiftCode = (String) shift[1];

        incident.setPayoutMethod(request.method());
        incident.setPayoutAmount(incident.getPayoutDue());
        incident.setPaidAt(Instant.now());
        incident.setPaidBy(username);
        incident.setPayoutShiftId(shiftId);
        Incident saved = incidentRepository.saveAndFlush(incident);

        Map<String, Object> oldData = new LinkedHashMap<>();
        oldData.put("payoutDue", saved.getPayoutDue());
        Map<String, Object> newData = new LinkedHashMap<>();
        newData.put("method", saved.getPayoutMethod().name());
        newData.put("amount", saved.getPayoutAmount());
        newData.put("shiftCode", shiftCode);
        auditLogService.log(new AuditEntry(AuditAction.INCIDENT_PAYOUT, EntityType.INCIDENT, saved.getId(),
                oldData, newData, null));
        return toDetail(saved);
    }


    private void applyContent(Incident incident, Long orderItemId, Integer affectedCount, IncidentType type,
                              String description, IncidentResolution resolution, BigDecimal amount,
                              List<Long> imageIds) {
        Order order = incident.getOrder();
        OrderItem item = null;
        if (orderItemId != null) {
            item = order.getItems().stream().filter(i -> orderItemId.equals(i.getId())).findFirst()
                    .orElseThrow(() -> new BusinessException("Món đồ không thuộc đơn này", "orderItemId"));
        }
        if (item == null && (resolution == IncidentResolution.DISCOUNT || resolution == IncidentResolution.COMPENSATION)) {
            throw new BusinessException("Chọn món đồ liên quan", "orderItemId");
        }
        int count = affectedCount == null ? 1 : affectedCount;
        if (item == null) {
            incident.setOrderItemId(null);
            incident.setItemLabel(null);
            incident.setItemUnit(null);
            incident.setItemQuantity(null);
            incident.setUnitPrice(null);
            incident.setLineTotal(null);
            incident.setAffectedCount(Math.max(count, 1));
            incident.setMaxCompensation(BigDecimal.ZERO);
        } else {
            BigDecimal quantity = itemQuantity(item);
            IncidentMath.validateAffectedCount(item.getUnit(), quantity, count);
            incident.setOrderItemId(item.getId());
            incident.setItemLabel(item.getItemType());
            incident.setItemUnit(item.getUnit());
            incident.setItemQuantity(quantity);
            incident.setUnitPrice(item.getUnitPrice());
            incident.setLineTotal(item.getLineTotal());
            incident.setAffectedCount(count);
            incident.setMaxCompensation(IncidentMath.maxCompensation(count, item.getUnitPrice(),
                    multiplierOf(order), capOf(order)));
        }
        incident.setType(type);
        incident.setDescription(description.trim());
        incident.setResolution(resolution);
        incident.setAmount(IncidentMath.normalizeAmount(resolution, amount, incident.getLineTotal(),
                incident.getMaxCompensation(), false));
        incident.replaceImages(toImages(imageIds));
    }

    private List<IncidentImage> toImages(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return List.of();
        }
        if (imageIds.size() > MAX_IMAGES) {
            throw new BusinessException("Tối đa 6 ảnh hiện tại", "imageIds");
        }
        List<IncidentImage> images = new ArrayList<>();
        for (Long documentId : imageIds) {
            Document document = documentService.requireImage(documentId);
            images.add(IncidentImage.builder().document(document).imageUrl(DocumentResponse.urlOf(document)).build());
        }
        return images;
    }

    private static BigDecimal itemQuantity(OrderItem item) {
        return item.getActualQuantity() != null ? item.getActualQuantity() : item.getQuantity();
    }

    private int multiplierOf(Order order) {
        return order.effectiveMultiplier(settingService.getCompensationMultiplier());
    }

    private BigDecimal capOf(Order order) {
        return order.effectiveCap(settingService.getCompensationCap());
    }


    private static void requireUnreviewed(Incident incident) {
        if (!incident.isPending()) {
            throw new ResourceConflictException("Phiếu đã được xử lý");
        }
    }

    private static Map<String, Object> reviewSnapshot(Incident incident) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", incident.getCode());
        data.put("status", incident.getStatus().name());
        data.put("resolution", incident.getResolution() == null ? null : incident.getResolution().name());
        data.put("amount", incident.getAmount());
        return data;
    }

    private void logReview(Incident incident, Map<String, Object> oldData, Map<String, Object> newData, String reason) {
        auditLogService.log(new AuditEntry(AuditAction.INCIDENT_APPROVE, EntityType.INCIDENT, incident.getId(),
                oldData, newData, reason));
    }

    private Object[] lockOpenShift() {
        List<?> rows = entityManager.createNativeQuery("SELECT id, code FROM shifts WHERE status = 'OPEN' FOR UPDATE")
                .getResultList();
        if (rows.isEmpty()) {
            throw new ResourceConflictException("Chưa có ca đang mở, vui lòng mở ca trước khi ghi nhận chi bồi thường");
        }
        return (Object[]) rows.get(0);
    }


    private Incident findIncident(Long id) {
        return incidentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Phiếu sự cố", id));
    }

    private Incident lockIncident(Long id) {
        return incidentRepository.findByIdForUpdate(id).orElseThrow(() -> NotFoundException.of("Phiếu sự cố", id));
    }

    private static IncidentFilter filter(IncidentStatus status, String q, IncidentType type,
                                         LocalDate createdFrom, LocalDate createdTo, Long orderId) {
        Instant from = createdFrom == null ? null : createdFrom.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant to = createdTo == null ? null : createdTo.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        return new IncidentFilter(status, q, type, from, to, orderId);
    }

    private IncidentDetailResponse toDetail(Incident i) {
        Order o = i.getOrder();
        Optional<OrderItem> item = i.getOrderItemId() == null ? Optional.empty()
                : o.getItems().stream().filter(it -> i.getOrderItemId().equals(it.getId())).findFirst();
        List<IncidentDetailResponse.ImageDto> initialImages;
        String initialNote;
        if (i.getOrderItemId() == null) {
            initialImages = o.getItems().stream().flatMap(it -> it.getImages().stream()).map(IncidentService::imageOf).toList();
            initialNote = null;
        } else {
            initialImages = item.map(it -> it.getImages().stream().map(IncidentService::imageOf).toList()).orElse(List.of());
            initialNote = item.map(OrderItem::getConditionNote).orElse(null);
        }
        List<IncidentDetailResponse.ItemOption> items = o.getItems().stream()
                .map(it -> new IncidentDetailResponse.ItemOption(it.getId(), it.getItemType(), it.getUnit(),
                        itemQuantity(it), it.getUnitPrice(), it.getLineTotal()))
                .toList();
        List<IncidentDetailResponse.ImageDto> images = i.getImages().stream()
                .map(img -> new IncidentDetailResponse.ImageDto(
                        img.getDocument() != null ? img.getDocument().getId() : img.getId(), img.getImageUrl(), null))
                .toList();
        IncidentDetailResponse.OrderBlock orderBlock = new IncidentDetailResponse.OrderBlock(o.getStatus(),
                o.getPaymentStatus(), o.getSubtotal(), o.getDiscountAmount(), o.getIncidentDiscount(), o.getTotalAmount());
        return new IncidentDetailResponse(i.getId(), i.getCode(), o.getId(), o.getOrderCode(), o.getCustomerName(),
                o.getCustomerPhone(), i.getType(), i.getResolution(), i.getAmount(), i.getStatus(),
                i.getDeductedAmount(), i.getPayoutDue(), i.getPaidAt(), i.getPayoutMethod(), i.getCreatedBy(),
                i.getCreatedAt(), i.getReviewedBy(), i.getReviewedAt(), i.getOrderItemId(), i.getItemLabel(),
                i.getItemUnit(), i.getItemQuantity(), i.getUnitPrice(), i.getLineTotal(), i.getAffectedCount(),
                i.getMaxCompensation(), multiplierOf(o), capOf(o), i.getDescription(), i.getReviewNote(),
                i.getOverLimitReason(), i.getPaidBy(), i.getPayoutAmount(), i.getPayoutShiftId(),
                shiftCode(i.getPayoutShiftId()), orderBlock, items, initialImages, initialNote, images);
    }

    private static IncidentDetailResponse.ImageDto imageOf(OrderImage image) {
        return new IncidentDetailResponse.ImageDto(image.getId(), image.getImageUrl(), image.getNote());
    }

    private String shiftCode(Long shiftId) {
        if (shiftId == null) {
            return null;
        }
        List<?> rows = entityManager.createNativeQuery("SELECT code FROM shifts WHERE id = :id")
                .setParameter("id", shiftId)
                .getResultList();
        return rows.isEmpty() ? null : (String) rows.get(0);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
