package com.laundry.modules.auth.application;

import com.laundry.modules.auth.application.dto.PasswordSetupRequestedEvent;
import com.laundry.modules.notification.application.MailjetMailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordSetupListener {

    MailjetMailService mailjetMailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(PasswordSetupRequestedEvent e) {
        String html = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Tài khoản của bạn đã được tạo</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f6f8;padding:24px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="560" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;width:100%%;background-color:#ffffff;border-radius:8px;overflow:hidden;">
            
                      <!-- Header -->
                      <tr>
                        <td style="background-color:#2563eb;padding:24px 32px;">
                          <h1 style="margin:0;font-size:20px;line-height:28px;color:#ffffff;font-weight:600;">
                            Tài khoản của bạn đã được tạo
                          </h1>
                        </td>
                      </tr>
            
                      <!-- Body -->
                      <tr>
                        <td style="padding:32px;color:#1f2937;font-size:15px;line-height:24px;">
                          <p style="margin:0 0 16px;">Xin chào,</p>
                          <p style="margin:0 0 20px;">
                            Quản trị viên đã tạo tài khoản cho bạn. Dưới đây là thông tin đăng nhập:
                          </p>
            
                          <!-- Username box -->
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                                 style="background-color:#f3f4f6;border-left:4px solid #2563eb;border-radius:4px;margin:0 0 24px;">
                            <tr>
                              <td style="padding:14px 16px;">
                                <div style="font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:0.5px;">Tên đăng nhập</div>
                                <div style="font-size:17px;font-weight:bold;color:#111827;margin-top:2px;">%s</div>
                              </td>
                            </tr>
                          </table>
            
                          <p style="margin:0 0 24px;">
                            Để bắt đầu sử dụng, vui lòng đặt mật khẩu cho tài khoản.
                            Bạn <b>chưa thể đăng nhập</b> cho đến khi hoàn tất bước này.
                          </p>
            
                          <!-- CTA button -->
                          <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" style="margin:0 auto 24px;">
                            <tr>
                              <td align="center" style="background-color:#2563eb;border-radius:6px;">
                                <a href="%s"
                                   style="display:inline-block;padding:14px 32px;font-size:16px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:6px;">
                                  Đặt mật khẩu
                                </a>
                              </td>
                            </tr>
                          </table>
            
                          <!-- Expiry note -->
                          <p style="margin:0 0 16px;font-size:13px;color:#b45309;background-color:#fffbeb;border:1px solid #fde68a;border-radius:4px;padding:10px 14px;">
                            ⏰ Liên kết có hiệu lực trong <b>24 giờ</b>. Nếu liên kết hết hạn, vui lòng liên hệ quản trị viên để được gửi lại.
                          </p>
            
                          <!-- Fallback link -->
                          <p style="margin:0;font-size:12px;color:#6b7280;word-break:break-all;">
                            Nếu nút bấm không hoạt động, hãy sao chép đường dẫn sau vào trình duyệt:<br>
                            <a href="%s" style="color:#2563eb;">%s</a>
                          </p>
                        </td>
                      </tr>
            
                      <!-- Footer -->
                      <tr>
                        <td style="background-color:#f9fafb;padding:20px 32px;border-top:1px solid #e5e7eb;font-size:12px;line-height:18px;color:#9ca3af;">
                          Đây là email tự động, vui lòng không trả lời email này.<br>
                          Nếu bạn không yêu cầu tài khoản này, hãy bỏ qua email hoặc liên hệ quản trị viên.
                        </td>
                      </tr>
            
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(e.username(), e.link(), e.link(), e.link());

        mailjetMailService.sendTemplate(e.email(), "Thiết lập mật khẩu tài khoản", html);
    }
}
