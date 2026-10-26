package com.footballhub.auth.mail;

import com.footballhub.config.AppProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

/** Picks the mail sender: Resend when RESEND_API_KEY is set, otherwise the logging fallback. */
@Configuration(proxyBeanMethods = false)
public class MailConfig {

    @Bean
    MailService mailService(AppProperties properties, JsonMapper jsonMapper) {
        AppProperties.Mail mail = properties.mail();
        if (!mail.resendApiKey().isEmpty()) {
            return new ResendMailService(mail.resendApiKey(), mail.from(), jsonMapper);
        }
        return new LoggingMailService(mail.logBodies());
    }
}
