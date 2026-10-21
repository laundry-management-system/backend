package com.laundry.modules.notification.domain;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "mailjet")
@Validated
public record MailjetProperties(
        @NotBlank String apiKey,
        @NotBlank String secretKey,
        @NotBlank String senderEmail,
        String senderName
) {}
