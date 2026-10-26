package com.footballhub;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.footballhub.auth.mail.MailService;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the real mail sender in tests and keeps every message for assertions. */
@TestConfiguration(proxyBeanMethods = false)
public class CapturingMailConfiguration {

    public record SentMail(String to, String subject, String body) {
    }

    public static class CapturingMailService implements MailService {

        private final List<SentMail> sent = new CopyOnWriteArrayList<>();

        @Override
        public void send(String to, String subject, String body) {
            sent.add(new SentMail(to, subject, body));
        }

        public List<SentMail> sent() {
            return sent;
        }

        public void clear() {
            sent.clear();
        }
    }

    @Bean
    @Primary
    CapturingMailService capturingMailService() {
        return new CapturingMailService();
    }
}
