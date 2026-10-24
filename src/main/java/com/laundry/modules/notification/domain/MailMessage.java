package com.laundry.modules.notification.domain;

import java.util.List;
import lombok.Builder;

@Builder
public record MailMessage(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String htmlContent,
        String textContent
) {}
