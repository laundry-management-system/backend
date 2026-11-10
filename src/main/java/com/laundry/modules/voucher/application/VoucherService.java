package com.laundry.modules.voucher.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.voucher.application.dto.VoucherCheckResponse;
import com.laundry.modules.voucher.application.dto.VoucherRequest;
import com.laundry.modules.voucher.application.dto.VoucherResponse;
import com.laundry.modules.voucher.domain.DiscountType;
import com.laundry.modules.voucher.domain.Voucher;
import com.laundry.modules.voucher.domain.VoucherCheckReason;
import com.laundry.modules.voucher.domain.VoucherRepository;
import com.laundry.modules.voucher.domain.VoucherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VoucherRepository voucherRepository;
    private final ActorIdResolver actorIdResolver;
    private final AuditLogService auditLogService;

    /** Voucher đã áp vào đơn. */
    public record AppliedVoucher(Long voucherId, String code, BigDecimal discount) {
    }

    // ---------- Quản lý (Admin) ----------

    @Transactional(readOnly = true)
    public Page<VoucherResponse> list(String search, VoucherStatus status, Pageable pageable) {
        String term = search == null || search.isBlank() ? null : search.trim();
        LocalDate today = today();
        return voucherRepository.search(term, status, pageable).map(v -> VoucherResponse.from(v, today));
    }

    @Transactional(readOnly = true)
    public VoucherResponse get(Long id) {
        return VoucherResponse.from(findById(id), today());
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request, String username) {
        String code = normalizeCode(request.code());
        validateRules(request);
        if (voucherRepository.existsByCode(code)) {
            throw new BusinessException("Mã voucher " + code + " đã tồn tại", "code");
        }
        Voucher voucher = Voucher.builder()
                .code(code)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .minOrderValue(orZero(request.minOrderValue()))
                .maxUses(request.maxUses())
                .description(trimToNull(request.description()))
                .status(VoucherStatus.ACTIVE)
                .createdBy(actorIdResolver.resolveId(username))
                .build();
        try {
            voucher = voucherRepository.saveAndFlush(voucher);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Mã voucher " + code + " đã tồn tại", "code");
        }
        audit(voucher, null, snapshot(voucher), null);
        return VoucherResponse.from(voucher, today());
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        Voucher voucher = findById(id);
        String code = normalizeCode(request.code());
        validateRules(request);
        boolean used = voucher.getUsedCount() > 0;
        if (!code.equals(voucher.getCode())) {
            if (used) throw new BusinessException("Không đổi được mã của voucher đã có đơn sử dụng", "code");
            if (voucherRepository.existsByCode(code)) {
                throw new BusinessException("Mã voucher " + code + " đã tồn tại", "code");
            }
        }
        if (used && request.discountType() != voucher.getDiscountType()) {
            throw new BusinessException("Không đổi được loại giảm giá của voucher đã có đơn sử dụng", "discountType");
        }
        if (request.maxUses() < voucher.getUsedCount()) {
            throw new BusinessException("Số lần tối đa không được nhỏ hơn số lần đã dùng ("
                    + voucher.getUsedCount() + ")", "maxUses");
        }
        Map<String, Object> before = snapshot(voucher);
        voucher.setCode(code);
        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(request.discountValue());
        voucher.setStartDate(request.startDate());
        voucher.setEndDate(request.endDate());
        voucher.setMinOrderValue(orZero(request.minOrderValue()));
        voucher.setMaxUses(request.maxUses());
        voucher.setDescription(trimToNull(request.description()));
        voucher = voucherRepository.saveAndFlush(voucher);
        audit(voucher, before, snapshot(voucher), null);
        return VoucherResponse.from(voucher, today());
    }

    @Transactional
    public VoucherResponse changeStatus(Long id, VoucherStatus status) {
        Voucher voucher = findById(id);
        if (voucher.getStatus() == status) {
            throw new BusinessException(status == VoucherStatus.ACTIVE
                    ? "Voucher đang được bật" : "Voucher đã bị tắt", "status");
        }
        Map<String, Object> before = snapshot(voucher);
        voucher.setStatus(status);
        voucher = voucherRepository.saveAndFlush(voucher);
        audit(voucher, before, snapshot(voucher), null);
        return VoucherResponse.from(voucher, today());
    }

    // ---------- Kiểm tra mã (NV, ô áp mã) ----------

    @Transactional(readOnly = true)
    public VoucherCheckResponse check(String rawCode, BigDecimal subtotal) {
        BigDecimal amount = orZero(subtotal);
        if (amount.signum() < 0) throw new BusinessException("Tiền hàng không được âm", "subtotal");
        Optional<Voucher> found = rawCode == null || rawCode.isBlank()
                ? Optional.empty() : voucherRepository.findByCode(normalizeCode(rawCode));
        if (found.isEmpty()) {
            return new VoucherCheckResponse(false, BigDecimal.ZERO, VoucherCheckReason.NOT_FOUND,
                    VoucherCheckReason.NOT_FOUND.getMessage(), null);
        }
        Voucher voucher = found.get();
        LocalDate today = today();
        VoucherCheckReason reason = voucher.evaluate(amount, today, false);
        VoucherResponse dto = VoucherResponse.from(voucher, today);
        return reason == null
                ? new VoucherCheckResponse(true, voucher.discountFor(amount), null, null, dto)
                : new VoucherCheckResponse(false, BigDecimal.ZERO, reason, reason.getMessage(), dto);
    }

    // ---------- Dùng bởi module đơn hàng ----------

    /**
     * Áp mã vào đơn lúc phân loại: kiểm tra BR-G01→G03, giữ một lượt (atomic) và trả lượt của mã cũ nếu đổi mã.
     * Đơn đã giữ lượt của chính mã này thì không trừ thêm.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AppliedVoucher applyToOrder(String rawCode, BigDecimal subtotal, Long currentVoucherId) {
        String code = normalizeCode(rawCode);
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(VoucherCheckReason.NOT_FOUND.getMessage(), "voucherCode"));
        boolean held = voucher.getId().equals(currentVoucherId);
        VoucherCheckReason reason = voucher.evaluate(subtotal, today(), held);
        if (reason != null) {
            throw new BusinessException(reason.getMessage(), "voucherCode");
        }
        if (!held) {
            if (voucherRepository.reserve(voucher.getId()) == 0) {
                throw new BusinessException(VoucherCheckReason.USED_UP.getMessage(), "voucherCode");
            }
            if (currentVoucherId != null) {
                voucherRepository.release(currentVoucherId);
            }
        }
        return new AppliedVoucher(voucher.getId(), voucher.getCode(), voucher.discountFor(subtotal));
    }

    /** Trả lượt khi đơn bỏ mã, bị hủy hoặc Admin sửa giá. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long voucherId) {
        if (voucherId != null) {
            voucherRepository.release(voucherId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> findCode(Long voucherId) {
        return voucherId == null ? Optional.empty() : voucherRepository.findById(voucherId).map(Voucher::getCode);
    }

    // ---------- helpers ----------

    private Voucher findById(Long id) {
        return voucherRepository.findById(id).orElseThrow(() -> NotFoundException.of("Voucher", id));
    }

    private static void validateRules(VoucherRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Ngày kết thúc phải từ ngày bắt đầu trở đi", "endDate");
        }
        if (request.discountType() == DiscountType.PERCENT
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Giảm theo phần trăm tối đa 100%", "discountValue");
        }
    }

    private void audit(Voucher voucher, Map<String, Object> before, Map<String, Object> after, String reason) {
        auditLogService.log(new AuditEntry(AuditAction.VOUCHER_MANAGE, EntityType.VOUCHER, voucher.getId(),
                before, after, reason, voucher.getCode()));
    }

    private static Map<String, Object> snapshot(Voucher v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", v.getCode());
        m.put("discountType", v.getDiscountType().name());
        m.put("discountValue", v.getDiscountValue());
        m.put("startDate", v.getStartDate().toString());
        m.put("endDate", v.getEndDate().toString());
        m.put("minOrderValue", v.getMinOrderValue());
        m.put("maxUses", v.getMaxUses());
        m.put("status", v.getStatus().name());
        return m;
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDate today() {
        return LocalDate.now(VIETNAM_ZONE);
    }
}
