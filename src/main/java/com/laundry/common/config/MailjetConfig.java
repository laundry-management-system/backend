package com.laundry.common.config;


import com.laundry.modules.notification.domain.MailjetProperties;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailjetProperties.class)
public class MailjetConfig {
    @Bean
    public MailjetClient mailjetClient(MailjetProperties props) {
    ClientOptions options =
        ClientOptions.builder()
                .apiKey(props.apiKey())
                .apiSecretKey(props.secretKey()).build();
        return new MailjetClient(options);
    }
}
