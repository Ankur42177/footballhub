package com.footballhub.auth.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback sender when no email provider is configured: records that a message was sent.
 * Bodies can contain one-time links (verification, password reset, review approval), so they
 * are written to the log only when explicitly enabled for local development.
 */
public class LoggingMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

    private final boolean logBodies;

    public LoggingMailService(boolean logBodies) {
        this.logBodies = logBodies;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (logBodies) {
            log.info("[mail] to={} subject=\"{}\"\n{}", to, subject, body);
        }
        else {
            log.info("[mail] not sent (no email provider configured): subject=\"{}\"", subject);
        }
    }
}
