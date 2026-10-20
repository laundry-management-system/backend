package com.laundry.modules.payments.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payment.vietqr")
public class VietQrProperties {

    private String bankId = "";
    private String accountNo = "";
    private String accountName = "";
    private String template = "compact2";
    private String imageBaseUrl = "https://img.vietqr.io/image";
}
