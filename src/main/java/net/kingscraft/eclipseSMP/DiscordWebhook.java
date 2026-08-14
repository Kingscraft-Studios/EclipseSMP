package net.kingscraft.eclipseSMP;

import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Asynchronous Discord webhook notifier. Never blocks the main thread;
 * failures are logged at most once per minute.
 */
public final class DiscordWebhook {

    private final Settings settings;
    private final HttpClient client;
    private long lastErrorLog;

    public DiscordWebhook(Settings settings) {
        this.settings = settings;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void sendWarning(int seconds) {
        sendEmbed("☀ THE ECLIPSE APPROACHES ☾",
                "A Blood Eclipse begins in **" + seconds + "s**.\nPvP shard drops and doubled death penalties start soon — bank your shards.",
                true);
    }

    public void sendActive(int duration) {
        sendEmbed("☀☾ BLOOD ECLIPSE ACTIVE ☽☀",
                "The Blood Eclipse is here for **" + duration + "s**!\n☀ Sol & Luna powers combine. Kills now drop **Eclipse Shards**.\nDeath is twice as costly. Good hunting.",
                true);
    }

    public void sendEnded(long dropped) {
        sendEmbed("☀ The Eclipse has lifted ☾",
                dropped > 0
                        ? "The Blood Eclipse is over.\n**" + dropped + " Eclipse Shards** were claimed this cycle."
                        : "The Blood Eclipse is over. No shards were claimed this cycle.",
                false);
    }

    public void sendTest() {
        sendEmbed("✅ Debug test webhook",
                "EclipseSMP's Discord webhook is working correctly.\nYou will receive eclipse alerts here.",
                false);
    }

    private void sendEmbed(String title, String description, boolean ping) {
        if (!settings.isWebhookEnabled()) return;
        String url = settings.getWebhookUrl();
        if (url == null || url.isBlank()) return;

        String content = "";
        if (ping && !settings.getWebhookRoleId().isBlank()) {
            content = "<@&" + settings.getWebhookRoleId() + ">";
        }

        String json = "{\"username\":\"" + escape(settings.getWebhookUsername())
                + "\",\"content\":\"" + escape(content)
                + "\",\"embeds\":[{\"title\":\"" + escape(title)
                + "\",\"description\":\"" + escape(description)
                + "\",\"color\":" + settings.getWebhookColor()
                + ",\"timestamp\":\"" + Instant.now() + "\"}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> {
                    long now = System.currentTimeMillis();
                    if (now - lastErrorLog > 60_000) {
                        lastErrorLog = now;
                        Bukkit.getLogger().warning("Discord webhook failed: " + ex.getMessage());
                    }
                    return null;
                });
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
