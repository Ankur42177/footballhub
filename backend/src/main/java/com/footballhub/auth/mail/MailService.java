package com.footballhub.auth.mail;

/**
 * Outbound email. The default implementation only logs messages; plug in an SMTP or
 * provider-backed implementation before relying on verification or password reset in
 * production.
 */
public interface MailService {

    void send(String to, String subject, String body);
}
