package com.footballhub.auth;

import java.util.Hashtable;
import java.util.Locale;
import java.util.function.Predicate;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Checks that an email's domain can receive mail: it must have MX records, or at least an
 * address record (the fallback mail servers use). Catches typos such as "gmial.co" and
 * invented domains at sign-up. If DNS itself is unreachable the check lets the address
 * through rather than blocking every sign-up.
 */
@Component
public class EmailDomainChecker {

    private static final Logger log = LoggerFactory.getLogger(EmailDomainChecker.class);

    private final Predicate<String> domainExists;

    public EmailDomainChecker() {
        this(EmailDomainChecker::dnsLookup);
    }

    EmailDomainChecker(Predicate<String> domainExists) {
        this.domainExists = domainExists;
    }

    public boolean canReceiveMail(String email) {
        int at = email == null ? -1 : email.lastIndexOf('@');
        if (at < 1 || at == email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".") || domain.length() > 253) {
            return false;
        }
        return domainExists.test(domain);
    }

    private static boolean dnsLookup(String domain) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        InitialDirContext dns = null;
        try {
            dns = new InitialDirContext(env);
            Attributes records = dns.getAttributes(domain, new String[] { "MX", "A", "AAAA" });
            return records.size() > 0;
        }
        catch (NameNotFoundException ex) {
            return false;
        }
        catch (NamingException ex) {
            log.info("DNS check for an email domain failed ({}); allowing it", ex.getClass().getSimpleName());
            return true;
        }
        finally {
            if (dns != null) {
                try {
                    dns.close();
                }
                catch (NamingException ignored) {
                    // nothing to clean up
                }
            }
        }
    }
}
