package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Request sửa giờ hẹn gửi / giờ hẹn nhận của đơn chưa nhận đồ (ít nhất một)")
public record RescheduleAppointmentRequest(Instant appointmentAt, Instant returnAt) {
}
