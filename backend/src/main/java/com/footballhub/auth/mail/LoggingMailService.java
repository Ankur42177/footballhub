package com.footballhub.auth.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/** Development mail sender: writes the message to the application log instead of sending it. */
@Component
public class LoggingMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[mail] to={} subject=\"{}\"\n{}", to, subject, body);
    }
}
