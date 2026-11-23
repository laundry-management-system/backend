package com.laundry.modules.booking.application.dto;

import com.laundry.modules.order.domain.DropoffMethod;
import com.laundry.modules.order.domain.ReturnMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateBookingIntakeRequest(
        @NotNull(message = "Thiếu mã yêu cầu")
        UUID requestId,

        @NotEmpty(message = "Vui lòng chọn ít nhất một dịch vụ")
        @Size(max = 20, message = "Tối đa 20 dịch vụ mỗi yêu cầu")
        List<@Valid Item> items,

        @NotNull(message = "Chọn hình thức giặt")
        Long washModeId,

        Boolean washWhitesSeparately,

        @NotNull(message = "Chọn hình thức gửi đồ")
        DropoffMethod dropoffMethod,

        @NotNull(message = "Chọn hình thức nhận lại")
        ReturnMethod returnMethod,

        Long pickupAreaId,

        Long deliveryAreaId,

        @Pattern(regexp = ".*\\S.*", message = "Nhập địa chỉ")
        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String pickupAddress,

        @Pattern(regexp = ".*\\S.*", message = "Nhập địa chỉ")
        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String deliveryAddress,

        @NotNull(message = "Chọn ngày giờ hẹn")
        Instant appointmentAt,

        Instant returnAt,

        @Pattern(regexp = ".*\\S.*", message = "Nhập họ và tên")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String contactName,

        String contactPhone,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note,

        @AssertTrue(message = "Cần đồng ý điều khoản để đặt đơn")
        Boolean termsAccepted
) {
    public record Item(
            @NotNull(message = "Chọn dịch vụ")
            Long serviceId
    ) {
    }
}
