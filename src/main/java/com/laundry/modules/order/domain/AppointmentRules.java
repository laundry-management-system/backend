package com.laundry.modules.order.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class AppointmentRules {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private AppointmentRules() {
    }

    /** Hẹn trả = nhận + giờ hoàn thành, làm tròn lên 30 phút, rồi kéo vào giờ mở cửa [open, close) (VN). */
    public static Instant dueAtFor(Instant receivedAt, int completionHours, LocalTime open, LocalTime close) {
        ZonedDateTime exact = receivedAt.plusSeconds(completionHours * 3600L).atZone(VN);
        ZonedDateTime d = exact.truncatedTo(ChronoUnit.HOURS).plusMinutes(exact.getMinute() / 30 * 30L);
        if (d.isBefore(exact)) {
            d = d.plusMinutes(30);
        }
        LocalTime t = d.toLocalTime();
        if (t.isBefore(open)) {
            d = d.with(open);
        } else if (!t.isBefore(close)) {
            d = d.plusDays(1).with(open);
        }
        return d.toInstant();
    }

    /** Trả câu lỗi đầu tiên vi phạm quy tắc giờ hẹn, hoặc null nếu hợp lệ (R9). */
    public static String appointmentError(Instant appt, Instant now, LocalTime open, LocalTime close,
                                          int minLeadHours) {
        ZonedDateTime a = appt.atZone(VN);
        if (a.getMinute() != 0 || a.getSecond() != 0 || a.getNano() != 0) {
            return "Giờ hẹn phải là giờ tròn";
        }
        LocalDate today = now.atZone(VN).toLocalDate();
        if (!a.toLocalDate().equals(today) && !a.toLocalDate().equals(today.plusDays(1))) {
            return "Chỉ nhận hẹn trong hôm nay hoặc ngày mai";
        }
        LocalTime t = a.toLocalTime();
        if (t.isBefore(open) || !t.isBefore(close) || appt.isBefore(now.plusSeconds(minLeadHours * 3600L))) {
            return "Giờ hẹn phải nằm trong giờ mở cửa " + open + " – " + close
                    + " và sau thời điểm đặt ít nhất " + minLeadHours + " giờ";
        }
        return null;
    }

    /** Lỗi đầu tiên của giờ nhận lại, hoặc null nếu hợp lệ. Caller bỏ qua khi giờ gửi đã sai. */
    public static String returnError(Instant returnAt, Instant dropoffAt, LocalTime open, LocalTime close,
                                     int gapHours) {
        if (returnAt == null) {
            return "Chọn giờ nhận lại";
        }
        ZonedDateTime r = returnAt.atZone(VN);
        if (r.getMinute() != 0 || r.getSecond() != 0 || r.getNano() != 0) {
            return "Giờ nhận lại phải là giờ tròn";
        }
        LocalTime t = r.toLocalTime();
        if (t.isBefore(open) || !t.isBefore(close)) {
            return "Giờ nhận lại phải nằm trong giờ mở cửa " + open + " – " + close;
        }
        LocalDate dropoffDate = dropoffAt.atZone(VN).toLocalDate();
        if (r.toLocalDate().isBefore(dropoffDate) || r.toLocalDate().isAfter(dropoffDate.plusDays(2))) {
            return "Giờ nhận lại chỉ trong vòng 2 ngày sau ngày gửi";
        }
        if (returnAt.isBefore(dropoffAt.plusSeconds(gapHours * 3600L))) {
            return "Giờ nhận lại phải sau giờ gửi ít nhất " + gapHours + " giờ";
        }
        return null;
    }
}
