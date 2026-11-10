package com.laundry.modules.incident.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import com.laundry.modules.document.application.DocumentService;
import com.laundry.modules.document.application.dto.DocumentResponse;
import com.laundry.modules.incident.application.dto.CreateIncidentRequest;
import com.laundry.modules.incident.application.dto.IncidentResponse;
import com.laundry.modules.incident.domain.Incident;
import com.laundry.modules.incident.domain.IncidentRepository;
import com.laundry.modules.incident.domain.IncidentResolution;
import com.laundry.modules.incident.domain.IncidentStatus;
import com.laundry.modules.incident.domain.IncidentType;
import com.laundry.modules.incident.domain.RedoStage;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.machine.domain.Machine;
import com.laundry.modules.machine.domain.MachineRepository;
import com.laundry.modules.order.application.CodeSequenceService;
import com.laundry.modules.order.domain.Order;
import com.laundry.modules.order.domain.OrderImage;
import com.laundry.modules.order.domain.OrderItem;
import com.laundry.modules.order.domain.OrderItemRepository;
import com.laundry.modules.order.domain.OrderRepository;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.PaymentStatus;
import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.domain.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final CodeSequenceService codeSequenceService;
    private final SettingService settingService;
    private final ActorIdResolver actorIdResolver;
    private final AuditLogService auditLogService;

    // ---------- Lập phiếu (NV) ----------

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request, String username) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", request.orderId()));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Không lập phiếu sự cố cho đơn đã hủy", "orderId");
        }
        if (request.orderItemId() != null) {
            orderItemRepository.findById(request.orderItemId())
                    .filter(item -> item.getOrder().getId().equals(order.getId()))
                    .orElseThrow(() -> new BusinessException("Dòng đồ không thuộc đơn này", "orderItemId"));
        }
        if (request.machineId() != null && !machineRepository.existsById(request.machineId())) {
            throw new BusinessException("Không tìm thấy máy", "machineId");
        }
        if (request.staffInvolvedId() != null && !userRepository.existsById(request.staffInvolvedId())) {
            throw new BusinessException("Không tìm thấy nhân viên", "staffInvolvedId");
        }
        BigDecimal amount = request.compensationAmount() == null ? BigDecimal.ZERO : request.compensationAmount();
        RedoStage redoStage = request.redoStage() == null ? RedoStage.NONE : request.redoStage();
        validateResolution(request.resolution(), redoStage, amount, order);

        List<Long> documentIds = request.documentIds() == null ? List.of() : List.copyOf(request.documentIds());
        List<String> imageUrls = documentIds.stream()
                .map(id -> DocumentResponse.urlOf(documentService.requireImage(id)))
                .toList();

        Incident incident = Incident.builder()
                .incidentCode(codeSequenceService.getNextIncidentCode())
                .orderId(order.getId())
                .orderItemId(request.orderItemId())
                .type(request.type())
                .description(request.description().trim())
                .machineId(request.machineId())
                .staffInvolvedId(request.staffInvolvedId())
                .documentIds(documentIds)
                .imageUrls(imageUrls)
                .resolution(request.resolution())
                .redoStage(request.resolution() == IncidentResolution.REWASH ? redoStage : RedoStage.NONE)
                .compensationAmount(request.resolution() == IncidentResolution.REWASH ? BigDecimal.ZERO : amount)
                .status(IncidentStatus.PENDING)
                .createdBy(actorIdResolver.resolveId(username))
                .build();
        incident = incidentRepository.saveAndFlush(incident);
        audit(AuditAction.INCIDENT_CREATE, incident, null, snapshot(incident), null);
        return toResponse(incident);
    }

    // ---------- Duyệt / từ chối (Admin) ----------

    /** Duyệt phiếu và thực hiện phương án: REWASH mở lần xử lý mới, DISCOUNT trừ vào đơn, COMPENSATE ghi nhận. */
    @Transactional
    public IncidentResponse approve(Long id, String note, String username) {
        Incident incident = findById(id);
        Order order = orderRepository.findById(incident.getOrderId())
                .orElseThrow(() -> NotFoundException.of("Đơn hàng", incident.getOrderId()));
        Map<String, Object> before = snapshot(incident);
        validateResolution(incident.getResolution(), incident.getRedoStage(), incident.getCompensationAmount(), order);

        switch (incident.getResolution()) {
            case REWASH -> {
                order.reopenForIncident(incident.getIncidentCode(), incident.getRedoStage().getDescription(), username);
                incident.setOpenedProcessingCount(order.getProcessingCount());
            }
            case DISCOUNT -> order.applyIncidentDiscount(incident.getCompensationAmount(),
                    incident.getIncidentCode(), username);
            case COMPENSATE -> { /* chỉ ghi nhận: tiền bồi thường chi ngoài hóa đơn */ }
        }
        incident.approve(actorIdResolver.resolveId(username), trimToNull(note));
        orderRepository.save(order);
        incidentRepository.saveAndFlush(incident);
        audit(AuditAction.INCIDENT_APPROVE, incident, before, snapshot(incident), trimToNull(note));
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse reject(Long id, String reason, String username) {
        Incident incident = findById(id);
        Map<String, Object> before = snapshot(incident);
        incident.reject(actorIdResolver.resolveId(username), reason);
        incidentRepository.saveAndFlush(incident);
        audit(AuditAction.INCIDENT_APPROVE, incident, before, snapshot(incident), incident.getDecisionNote());
        return toResponse(incident);
    }

    // ---------- Tra cứu ----------

    @Transactional(readOnly = true)
    public Page<IncidentResponse> list(IncidentStatus status, IncidentType type, Instant from, Instant to,
                                       Pageable pageable) {
        Specification<Incident> spec = Specification.allOf(Stream.of(
                        IncidentRepository.hasStatus(status),
                        IncidentRepository.hasType(type),
                        IncidentRepository.createdFrom(from),
                        IncidentRepository.createdBefore(to))
                .filter(Objects::nonNull)
                .toList());
        Page<Incident> page = incidentRepository.findAll(spec, pageable);
        Map<Long, IncidentResponse> mapped = toResponses(page.getContent()).stream()
                .collect(Collectors.toMap(IncidentResponse::id, Function.identity()));
        return page.map(i -> mapped.get(i.getId()));
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listByOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) throw NotFoundException.of("Đơn hàng", orderId);
        return toResponses(incidentRepository.findByOrderIdOrderByCreatedAtDesc(orderId));
    }

    // ---------- helpers ----------

    private void validateResolution(IncidentResolution resolution, RedoStage redoStage, BigDecimal amount, Order order) {
        switch (resolution) {
            case REWASH -> {
                if (redoStage == RedoStage.NONE) {
                    throw new BusinessException("Phương án Giặt lại phải chọn công đoạn cần làm lại", "redoStage");
                }
                if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.COMPLETED) {
                    throw new BusinessException("Chỉ giặt lại được đơn Đang xử lý hoặc Hoàn thành (chưa giao)", "resolution");
                }
            }
            case DISCOUNT -> {
                requirePositive(amount);
                if (order.getPaymentStatus() == PaymentStatus.PAID || order.getStatus().isTerminal()) {
                    throw new BusinessException("Chỉ giảm giá được đơn chưa thanh toán và chưa giao; hãy chọn Bồi thường",
                            "resolution");
                }
                if (amount.compareTo(order.getTotalAmount()) > 0) {
                    throw new BusinessException("Số tiền giảm không vượt tổng tiền đơn ("
                            + order.getTotalAmount().toPlainString() + "đ)", "compensationAmount");
                }
            }
            case COMPENSATE -> {
                requirePositive(amount);
                compensationCap().ifPresent(cap -> {
                    if (amount.compareTo(cap) > 0) {
                        throw new BusinessException("Mức bồi thường tối đa theo điều khoản là "
                                + cap.toPlainString() + "đ", "compensationAmount");
                    }
                });
            }
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Vui lòng nhập số tiền lớn hơn 0", "compensationAmount");
        }
    }

    /** COMPENSATION_TERMS là văn bản; nếu chỉ chứa một số thì dùng làm mức bồi thường tối đa. */
    private Optional<BigDecimal> compensationCap() {
        String raw = settingService.getString(SettingKey.COMPENSATION_TERMS, null);
        if (raw == null) return Optional.empty();
        String digits = raw.replaceAll("[.,\\s]", "");
        return digits.matches("\\d{1,12}") ? Optional.of(new BigDecimal(digits)) : Optional.empty();
    }

    private Incident findById(Long id) {
        return incidentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Phiếu sự cố", id));
    }

    private IncidentResponse toResponse(Incident incident) {
        return toResponses(List.of(incident)).getFirst();
    }

    /** Nạp theo lô đơn, dòng đồ, máy, người dùng liên quan để tránh truy vấn từng phiếu. */
    private List<IncidentResponse> toResponses(List<Incident> incidents) {
        if (incidents.isEmpty()) return List.of();
        Map<Long, Order> orders = byId(orderRepository.findAllById(ids(incidents, Incident::getOrderId)), Order::getId);
        Map<Long, OrderItem> items = byId(orderItemRepository.findAllById(ids(incidents, Incident::getOrderItemId)),
                OrderItem::getId);
        Map<Long, Machine> machines = byId(machineRepository.findAllById(ids(incidents, Incident::getMachineId)),
                Machine::getId);
        Set<Long> userIds = incidents.stream()
                .flatMap(i -> Stream.of(i.getCreatedBy(), i.getDecidedBy(), i.getStaffInvolvedId()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        return incidents.stream().map(i -> {
            Order order = orders.get(i.getOrderId());
            OrderItem item = i.getOrderItemId() == null ? null : items.get(i.getOrderItemId());
            Machine machine = i.getMachineId() == null ? null : machines.get(i.getMachineId());
            List<String> originals = item == null || item.getImages() == null ? List.of()
                    : item.getImages().stream().map(OrderImage::getImageUrl).filter(Objects::nonNull).toList();
            return IncidentResponse.from(i, new IncidentResponse.Refs(
                    order == null ? null : order.getOrderCode(),
                    order == null ? null : order.getCustomerName(),
                    order == null ? null : order.getCustomerPhone(),
                    item == null ? null : item.getItemType(),
                    originals,
                    item == null ? null : item.getConditionNote(),
                    machine == null ? null : machine.getMachineCode(),
                    users.get(i.getStaffInvolvedId()),
                    users.get(i.getCreatedBy()),
                    users.get(i.getDecidedBy())));
        }).toList();
    }

    private static Set<Long> ids(Collection<Incident> incidents, Function<Incident, Long> getter) {
        return incidents.stream().map(getter).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> id) {
        return list.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private void audit(AuditAction action, Incident incident, Map<String, Object> before, Map<String, Object> after,
                       String reason) {
        auditLogService.log(new AuditEntry(action, EntityType.INCIDENT, incident.getId(), before, after, reason,
                incident.getIncidentCode()));
    }

    private static Map<String, Object> snapshot(Incident i) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", i.getOrderId());
        data.put("type", i.getType().name());
        data.put("resolution", i.getResolution().name());
        data.put("redoStage", i.getRedoStage().name());
        data.put("compensationAmount", i.getCompensationAmount());
        data.put("status", i.getStatus().name());
        data.put("decisionNote", i.getDecisionNote());
        return data;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
