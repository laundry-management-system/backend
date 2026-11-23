package com.laundry.modules.voucher.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.voucher.application.dto.CreateVoucherRequest;
import com.laundry.modules.voucher.application.dto.UpdateVoucherRequest;
import com.laundry.modules.voucher.application.dto.VoucherResponse;
import com.laundry.modules.voucher.application.dto.VoucherValidationResponse;
import com.laundry.modules.voucher.domain.Voucher;
import com.laundry.modules.voucher.domain.VoucherRepository;
import com.laundry.modules.voucher.domain.VoucherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request, String createdBy) {
        String cleanCode = request.code().trim().toUpperCase();

        if (voucherRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new BusinessException("Voucher code '" + cleanCode + "' already exists");
        }

        Voucher voucher = Voucher.builder()
                .code(cleanCode)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .minOrderValue(request.minOrderValue() != null ? request.minOrderValue() : BigDecimal.ZERO)
                .maxUses(request.maxUses())
                .usedCount(0)
                .status(VoucherStatus.ACTIVE)
                .createdBy(createdBy)
                .build();

        voucher.validateBusinessRules();

        Voucher saved = voucherRepository.save(voucher);
        return VoucherResponse.from(saved);
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, UpdateVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Voucher not found with ID: " + id));

        voucher.updateDetails(
                request.discountType(),
                request.discountValue(),
                request.startDate(),
                request.endDate(),
                request.minOrderValue(),
                request.maxUses()
        );

        Voucher saved = voucherRepository.save(voucher);
        return VoucherResponse.from(saved);
    }

    @Transactional
    public VoucherResponse changeStatus(Long id, VoucherStatus status) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Voucher not found with ID: " + id));

        if (status == VoucherStatus.DISABLED) {
            voucher.disable();
        } else if (status == VoucherStatus.ACTIVE) {
            voucher.activate();
        }

        Voucher saved = voucherRepository.save(voucher);
        return VoucherResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long id) {
        return voucherRepository.findById(id)
                .map(VoucherResponse::from)
                .orElseThrow(() -> new NotFoundException("Voucher not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return voucherRepository.findByCodeIgnoreCase(cleanCode)
                .map(VoucherResponse::from)
                .orElseThrow(() -> new NotFoundException("Voucher not found with code: " + cleanCode));
    }

    @Transactional(readOnly = true)
    public Page<VoucherResponse> listVouchers(String search, VoucherStatus status, Pageable pageable) {
        String cleanSearch = search != null ? search.trim() : "";
        return voucherRepository.searchVouchers(cleanSearch, status, pageable)
                .map(VoucherResponse::from);
    }

    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(String code, BigDecimal orderValue) {
        if (code == null || code.isBlank()) {
            return VoucherValidationResponse.invalid("", "Vui lòng nhập mã voucher");
        }
        String cleanCode = code.trim().toUpperCase();

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(cleanCode).orElse(null);
        if (voucher == null) {
            return VoucherValidationResponse.invalid(cleanCode, "Mã voucher không hợp lệ");
        }

        String reason = voucher.rejectionReason(orderValue, LocalDate.now(), true);
        if (reason != null) {
            return VoucherValidationResponse.invalid(cleanCode, reason);
        }

        BigDecimal discountAmount = voucher.calculateDiscount(orderValue);
        return VoucherValidationResponse.valid(voucher, orderValue, discountAmount);
    }

    @Transactional
    public VoucherResponse applyVoucher(String code, BigDecimal orderValue) {
        VoucherValidationResponse validation = validateVoucher(code, orderValue);
        if (!validation.valid()) {
            throw new BusinessException(validation.message());
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Voucher not found: " + code));

        voucher.incrementUsedCount();
        Voucher saved = voucherRepository.save(voucher);
        return VoucherResponse.from(saved);
    }

    /**
     * Áp voucher cho đơn khi lưu phân loại: kiểm tra, tính tiền giảm trên tiền hàng và giữ một lượt.
     * Đơn đang giữ lượt của chính voucher này thì không tính thêm lượt; đổi sang mã khác thì trả lượt mã cũ.
     */
    @Transactional
    public AppliedVoucher applyToOrder(String code, BigDecimal orderValue, Long currentVoucherId) {
        String cleanCode = code.trim().toUpperCase();
        Voucher voucher = voucherRepository.lockByCode(cleanCode)
                .orElseThrow(() -> new BusinessException("Mã voucher không hợp lệ", "voucherCode"));
        boolean alreadyHeld = voucher.getId().equals(currentVoucherId);
        String reason = voucher.rejectionReason(orderValue, LocalDate.now(), !alreadyHeld);
        if (reason != null) {
            throw new BusinessException(reason, "voucherCode");
        }
        if (!alreadyHeld) {
            releaseVoucher(currentVoucherId);
            voucher.incrementUsedCount();
        }
        return new AppliedVoucher(voucher.getId(), voucher.getCode(), voucher.calculateDiscount(orderValue));
    }

    public record AppliedVoucher(Long voucherId, String code, BigDecimal discountAmount) {}

    @Transactional
    public void releaseVoucher(Long voucherId) {
        if (voucherId == null) {
            return;
        }
        voucherRepository.lockById(voucherId).ifPresent(Voucher::decrementUsedCount);
    }
}
