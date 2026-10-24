package com.laundry.modules.notification.application;

import com.laundry.modules.notification.domain.MailMessage;
import com.laundry.modules.notification.domain.MailjetProperties;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MailjetMailService{

    MailjetClient client;
    MailjetProperties props;

    static final int MAX_ATTEMPTS = 3;

    public void send(MailMessage message) {
        try {
            JSONObject email = new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                            .put("Email", props.senderEmail())
                            .put("Name", props.senderName() == null ? "" : props.senderName()))
                    .put(Emailv31.Message.TO, toRecipients(message.to()))
                    .put(Emailv31.Message.SUBJECT, message.subject());

            if (message.cc() != null && !message.cc().isEmpty()) {
                email.put(Emailv31.Message.CC, toRecipients(message.cc()));
            }
            if (message.bcc() != null && !message.bcc().isEmpty()) {
                email.put(Emailv31.Message.BCC, toRecipients(message.bcc()));
            }
            if (message.htmlContent() != null) {
                email.put(Emailv31.Message.HTMLPART, message.htmlContent());
            }
            if (message.textContent() != null) {
                email.put(Emailv31.Message.TEXTPART, message.textContent());
            }

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray().put(email));

            MailjetResponse response = client.post(request);

            if (response.getStatus() == 200) {
                String messageId = extractMessageId(response);
                log.info("Mailjet sent OK to={} messageId={}", message.to(), messageId);
            }else log.error("Mailjet failed status={} body={}", response.getStatus(), response.getData());

        } catch (Exception e) {
            log.error("Mailjet exception to={}", message.to(), e);
            throw new MailSendException("Không gửi được email: " + e.getMessage(), e);
        }
    }


    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String html) {
        MailMessage message = MailMessage.builder()
                .to(List.of(to)).subject(subject).htmlContent(html).build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                send(message);
                return;
            } catch (MailjetSendException e) {
                boolean last = attempt == MAX_ATTEMPTS;
                if (!e.isRetryable() || last) {
                    log.error("Gửi mail thất bại (lần {}/{}, status={})",
                            attempt, MAX_ATTEMPTS, e.getStatus(), e);
                    return;
                }
                log.warn("Gửi mail lần {} thất bại (status={}), thử lại", attempt, e.getStatus());
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private JSONArray toRecipients(List<String> emails) {
        JSONArray arr = new JSONArray();
        emails.forEach(e -> arr.put(new JSONObject().put("Email", e)));
        return arr;
    }

    private String extractMessageId(MailjetResponse response) {
        try {
            return String.valueOf(response.getData().getJSONObject(0)
                    .getJSONArray("To").getJSONObject(0).get("MessageID"));
        } catch (Exception e) {
            return null;
        }
    }

    static class MailjetSendException extends MailSendException {
        private final int status;

        MailjetSendException(String msg, int status, Throwable cause) {
            super(msg, cause);
            this.status = status;
        }
        int getStatus() { return status; }

        // 4xx (sai key, sai dữ liệu) thử lại cũng vô ích, trừ 429 (rate limit)
        boolean isRetryable() { return status == -1 || status == 429 || status >= 500; }
    }
}
