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
            return VoucherValidationResponse.invalid("", "Voucher code must not be blank");
        }
        String cleanCode = code.trim().toUpperCase();

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(cleanCode).orElse(null);
        if (voucher == null) {
            return VoucherValidationResponse.invalid(cleanCode, "Voucher code '" + cleanCode + "' not found");
        }

        if (!voucher.isActive()) {
            return VoucherValidationResponse.invalid(cleanCode, "Voucher '" + cleanCode + "' is disabled (BR-G05)");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(voucher.getStartDate())) {
            return VoucherValidationResponse.invalid(cleanCode, "Voucher '" + cleanCode + "' is not yet active (effective from " + voucher.getStartDate() + ") (BR-G01)");
        }
        if (today.isAfter(voucher.getEndDate())) {
            return VoucherValidationResponse.invalid(cleanCode, "Voucher '" + cleanCode + "' has expired on " + voucher.getEndDate() + " (BR-G01)");
        }

        if (!voucher.hasAvailableUses()) {
            return VoucherValidationResponse.invalid(cleanCode, "Voucher '" + cleanCode + "' has reached its usage limit (" + voucher.getMaxUses() + ") (BR-G03)");
        }

        if (!voucher.satisfiesMinOrderValue(orderValue)) {
            return VoucherValidationResponse.invalid(cleanCode, "Order total (" + orderValue + ") does not meet the minimum requirement of " + voucher.getMinOrderValue() + " (BR-G02)");
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

    @Transactional
    public void releaseVoucher(Long voucherId) {
        if (voucherId == null) {
            return;
        }
        voucherRepository.findById(voucherId).ifPresent(voucher -> {
            voucher.decrementUsedCount();
            voucherRepository.save(voucher);
        });
    }
}
