package com.laundry.modules.customer.application;

import com.laundry.modules.customer.application.dto.CustomerAccountMailEvent;
import com.laundry.modules.notification.application.MailjetMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

/** Chỉ gửi thư khi giao dịch đã commit, để không báo kích hoạt / đặt lại cho thao tác bị rollback. */
@Component
@RequiredArgsConstructor
public class CustomerAccountMailListener {

    private final MailjetMailService mailjetMailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMail(CustomerAccountMailEvent event) {
        mailjetMailService.sendHtml(event.to(), event.subject(), event.html());
    }

    public static CustomerAccountMailEvent activated(String to, String fullName, String loginUrl) {
        return new CustomerAccountMailEvent(to, "Tài khoản của bạn đã được kích hoạt", layout(
                "Tài khoản của bạn đã được kích hoạt",
                "Xin chào " + HtmlUtils.htmlEscape(fullName) + ",",
                "Tiệm đã kích hoạt tài khoản của bạn. Bạn có thể đăng nhập bằng số điện thoại hoặc email cùng mật khẩu đã đặt.",
                loginUrl, "Đăng nhập"));
    }

    public static CustomerAccountMailEvent passwordReset(String to, String fullName, String resetUrl) {
        return new CustomerAccountMailEvent(to, "Đặt lại mật khẩu", layout(
                "Đặt lại mật khẩu",
                "Xin chào " + HtmlUtils.htmlEscape(fullName) + ",",
                "Bấm nút bên dưới để đặt mật khẩu mới. Link chỉ dùng được một lần và hết hạn sau 30 phút. Nếu bạn không yêu cầu, hãy bỏ qua thư này.",
                resetUrl, "Đặt lại mật khẩu"));
    }

    private static String layout(String title, String greeting, String body, String url, String cta) {
        String safeUrl = HtmlUtils.htmlEscape(url);
        return """
                <!DOCTYPE html>
                <html lang="vi"><head><meta charset="UTF-8"><title>%s</title></head>
                <body style="margin:0;padding:24px;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                    <tr><td align="center">
                      <table role="presentation" width="560" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;width:100%%;background:#ffffff;border-radius:8px;">
                        <tr><td style="padding:32px;font-size:15px;line-height:24px;">
                          <h1 style="margin:0 0 16px;font-size:20px;">%s</h1>
                          <p style="margin:0 0 12px;">%s</p>
                          <p style="margin:0 0 24px;">%s</p>
                          <a href="%s" style="display:inline-block;padding:12px 20px;background:#0f766e;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:600;">%s</a>
                          <p style="margin:24px 0 0;font-size:13px;color:#6b7280;">Nếu nút không bấm được, mở link: %s</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body></html>
                """.formatted(title, title, greeting, body, safeUrl, cta, safeUrl);
    }
}
