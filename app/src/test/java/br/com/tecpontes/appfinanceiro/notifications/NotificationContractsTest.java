package br.com.tecpontes.appfinanceiro.notifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

public class NotificationContractsTest {

    private static final String HISTORY_DEEP_LINK = "app://notifications/history";
    private static final String INVITATION_DEEP_LINK_PREFIX = "app://finance-space/invitation/";

    private boolean isUuid(String value) {
        if (value == null) return false;
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean isSupportedType(String type) {
        return "custom_reminder".equals(type)
                || "financial_due_date".equals(type)
                || "payment_confirmation".equals(type)
                || "delay_notice".equals(type)
                || "cancellation_notice".equals(type)
                || "finance_space_invitation".equals(type);
    }

    private boolean isAllowedDeepLink(String deepLink) {
        if (deepLink == null) return false;
        if (HISTORY_DEEP_LINK.equals(deepLink)) return true;
        if (!deepLink.startsWith(INVITATION_DEEP_LINK_PREFIX)) return false;
        String token = deepLink.substring(INVITATION_DEEP_LINK_PREFIX.length());
        return token.matches("[A-Za-z0-9_-]{40,128}");
    }

    private boolean isSupportedPriority(String priority) {
        return "low".equals(priority) || "medium".equals(priority)
                || "high".equals(priority) || "critical".equals(priority);
    }

    private boolean isValidFcmPayload(Map<String, String> data, String currentBindingId) {
        if (data == null || data.isEmpty()) return false;
        if (!"1".equals(data.get("schemaVersion"))) return false;

        String bindingId = data.get("bindingId");
        if (bindingId == null || !bindingId.equals(currentBindingId)) return false;

        String logId = data.get("logId");
        if (!isUuid(logId)) return false;

        String deepLink = data.get("deepLink");
        if (!isAllowedDeepLink(deepLink)) return false;

        String type = data.get("type");
        if (!isSupportedType(type)) return false;

        String priority = data.get("priority");
        if (!isSupportedPriority(priority)) return false;

        String title = data.get("title");
        String body = data.get("body");
        String safeTitle = title != null ? title.trim() : "";
        String safeBody = body != null ? body.trim() : "";
        return !safeTitle.isEmpty() || !safeBody.isEmpty();
    }

    @Test
    public void validPayload_acceptsValidFields() {
        String activeBindingId = "binding-account-1";
        Map<String, String> data = new HashMap<>();
        data.put("schemaVersion", "1");
        data.put("bindingId", activeBindingId);
        data.put("logId", UUID.randomUUID().toString());
        data.put("deepLink", "app://notifications/history");
        data.put("type", "financial_due_date");
        data.put("priority", "high");
        data.put("title", "Conta a vencer");
        data.put("body", "Sua conta vence hoje");

        assertTrue(isValidFcmPayload(data, activeBindingId));
    }

    @Test
    public void validPayload_acceptsFinanceSpaceInvitationDeepLink() {
        String activeBindingId = "binding-account-1";
        Map<String, String> data = new HashMap<>();
        data.put("schemaVersion", "1");
        data.put("bindingId", activeBindingId);
        data.put("logId", UUID.randomUUID().toString());
        data.put("deepLink", INVITATION_DEEP_LINK_PREFIX
                + "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_abcd");
        data.put("type", "finance_space_invitation");
        data.put("priority", "high");
        data.put("title", "Convite para compartilhar finanças");
        data.put("body", "Você recebeu um convite.");

        assertTrue(isValidFcmPayload(data, activeBindingId));
    }

    @Test
    public void invalidPayload_rejectsInvitationDeepLinkWithoutToken() {
        String bindingId = "binding-1";
        Map<String, String> data = new HashMap<>();
        data.put("schemaVersion", "1");
        data.put("bindingId", bindingId);
        data.put("logId", UUID.randomUUID().toString());
        data.put("deepLink", INVITATION_DEEP_LINK_PREFIX);
        data.put("type", "finance_space_invitation");
        data.put("priority", "high");
        data.put("title", "Convite");

        assertFalse(isValidFcmPayload(data, bindingId));
    }

    @Test
    public void mismatchedBindingId_rejectsPayloadFromPreviousAccount() {
        String oldBindingId = "binding-account-1";
        String currentBindingId = "binding-account-2";

        Map<String, String> data = new HashMap<>();
        data.put("schemaVersion", "1");
        data.put("bindingId", oldBindingId);
        data.put("logId", UUID.randomUUID().toString());
        data.put("deepLink", "app://notifications/history");
        data.put("type", "financial_due_date");
        data.put("priority", "high");
        data.put("title", "Conta a vencer");

        assertFalse(isValidFcmPayload(data, currentBindingId));
    }

    @Test
    public void invalidLogId_rejectsNonUuid() {
        String bindingId = "binding-1";
        Map<String, String> data = new HashMap<>();
        data.put("schemaVersion", "1");
        data.put("bindingId", bindingId);
        data.put("logId", "not-a-uuid");
        data.put("deepLink", "app://notifications/history");
        data.put("type", "custom_reminder");
        data.put("priority", "medium");
        data.put("title", "Lembrete");

        assertFalse(isValidFcmPayload(data, bindingId));
    }

    @Test
    public void unsupportedTypeOrPriority_rejectsPayload() {
        String bindingId = "binding-1";
        Map<String, String> data = new HashMap<>();
        data.put("schemaVersion", "1");
        data.put("bindingId", bindingId);
        data.put("logId", UUID.randomUUID().toString());
        data.put("deepLink", "app://notifications/history");
        data.put("type", "unknown_type");
        data.put("priority", "high");
        data.put("title", "Teste");

        assertFalse(isValidFcmPayload(data, bindingId));
    }

    @Test
    public void invalidDeepLink_rejectsArbitraryUrls() {
        String bindingId = "binding-1";
        Map<String, String> data = new HashMap<>();
        data.put("schemaVersion", "1");
        data.put("bindingId", bindingId);
        data.put("logId", UUID.randomUUID().toString());
        data.put("deepLink", "https://malicious-site.com");
        data.put("type", "financial_due_date");
        data.put("priority", "high");
        data.put("title", "Aviso");

        assertFalse(isValidFcmPayload(data, bindingId));
    }
}
