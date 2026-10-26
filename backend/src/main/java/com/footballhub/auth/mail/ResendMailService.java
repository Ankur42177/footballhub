package com.footballhub.auth.mail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.json.JsonMapper;

/**
 * Sends plain-text email through Resend's HTTPS API (works on hosts that block SMTP).
 * Plain text only: user-written content can never become HTML or script in the reader's
 * mail client. Failures are logged without the message body and never break the request.
 */
public class ResendMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ResendMailService.class);
    private static final URI ENDPOINT = URI.create("https://api.resend.com/emails");

    private final String apiKey;
    private final String from;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public ResendMailService(String apiKey, String from, JsonMapper json) {
        this.apiKey = apiKey;
        this.from = from;
        this.json = json;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            String payload = json.writeValueAsString(Map.of(
                    "from", from,
                    "to", List.of(to),
                    "subject", singleLine(subject),
                    "text", body));
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                log.warn("[mail] Resend rejected a message: status={} subject=\"{}\"", response.statusCode(),
                        singleLine(subject));
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[mail] interrupted while sending \"{}\"", singleLine(subject));
        }
        catch (Exception ex) {
            log.warn("[mail] could not send \"{}\": {}", singleLine(subject), ex.getClass().getSimpleName());
        }
    }

    /** Subjects never contain line breaks (no header injection). */
    static String singleLine(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}
