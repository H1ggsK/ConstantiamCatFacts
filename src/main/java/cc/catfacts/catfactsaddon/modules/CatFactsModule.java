package cc.catfacts.catfactsaddon.modules;

import cc.catfacts.catfactsaddon.CatFactsAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A module that periodically sends cat facts in chat.
 * Supports both message-count-based and time-based interval modes.
 * Fetches facts from catfacts.cc, meowfacts.herokuapp.com, or randomly picks between both.
 */
public class CatFactsModule extends Module {

    /**
     * Determines whether facts are sent based on received message count or elapsed time.
     */
    public enum IntervalMode {
        MESSAGES,
        TIME
    }

    /**
     * The unit of time used when the interval mode is set to TIME.
     */
    public enum TimeUnit {
        SECONDS,
        MINUTES,
        HOURS
    }

    /**
     * Determines which API source to fetch cat facts from.
     */
    public enum FactSource {
        CATFACTS_CC,
        MEOWFACTS,
        RANDOM
    }

    private static final String CATFACTS_CC_URL = "https://catfacts.cc/fact";
    private static final String MEOWFACTS_URL = "https://meowfacts.herokuapp.com/";
    private static final Random RANDOM = new Random();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    /**
     * Selects which API to fetch cat facts from.
     */
    private final Setting<FactSource> factSource = sgGeneral.add(new EnumSetting.Builder<FactSource>()
        .name("fact-source")
        .description("Which API to fetch cat facts from.")
        .defaultValue(FactSource.RANDOM)
        .build()
    );

    /**
     * The fallback message sent when the API fails to return a valid fact.
     */
    private final Setting<String> fallbackMessage = sgGeneral.add(new StringSetting.Builder()
        .name("fallback-message")
        .description("Message to send if the API fails to return a fact.")
        .defaultValue("Cats are mysterious...")
        .build()
    );

    /**
     * Selects whether to use message count or time as the interval trigger.
     */
    private final Setting<IntervalMode> intervalMode = sgGeneral.add(new EnumSetting.Builder<IntervalMode>()
        .name("interval-mode")
        .description("Whether to send facts based on message count or elapsed time.")
        .defaultValue(IntervalMode.MESSAGES)
        .build()
    );

    /**
     * Number of received messages to wait before sending a fact. Only visible when interval mode is MESSAGES.
     */
    private final Setting<Integer> messageInterval = sgGeneral.add(new IntSetting.Builder()
        .name("message-interval")
        .description("Messages to wait before sending a fact.")
        .defaultValue(10000)
        .min(1)
        .sliderMax(20000)
        .visible(() -> intervalMode.get() == IntervalMode.MESSAGES)
        .build()
    );

    /**
     * The amount of time to wait before sending a fact. Only visible when interval mode is TIME.
     */
    private final Setting<Integer> timeInterval = sgGeneral.add(new IntSetting.Builder()
        .name("time-interval")
        .description("Amount of time to wait before sending a fact.")
        .defaultValue(5)
        .min(1)
        .sliderMax(120)
        .visible(() -> intervalMode.get() == IntervalMode.TIME)
        .build()
    );

    /**
     * The unit for the time interval. Only visible when interval mode is TIME.
     */
    private final Setting<TimeUnit> timeUnit = sgGeneral.add(new EnumSetting.Builder<TimeUnit>()
        .name("time-unit")
        .description("The unit of time for the time interval.")
        .defaultValue(TimeUnit.MINUTES)
        .visible(() -> intervalMode.get() == IntervalMode.TIME)
        .build()
    );

    private int messageCounter = 0;
    private long lastFactTimestamp = 0;

    /**
     * Constructs the CatFactsModule and registers it under the CatFactsAddon category.
     */
    public CatFactsModule() {
        super(CatFactsAddon.CATEGORY, "cat-facts-spammer", "Sends a cat fact periodically.");
    }

    @Override
    public void onActivate() {
        messageCounter = 0;
        lastFactTimestamp = System.currentTimeMillis();
    }

    /**
     * Handles incoming chat messages and increments the message counter.
     * Sends a cat fact when the message threshold is reached in MESSAGES mode.
     *
     * @param event the received message event
     */
    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (intervalMode.get() != IntervalMode.MESSAGES) return;

        messageCounter++;
        if (messageCounter >= (messageInterval.get()+1)) {
            messageCounter = 0;
            sendCatFact();
        }
    }

    /**
     * Handles tick events to check elapsed time in TIME mode.
     * Sends a cat fact when the configured time interval has passed.
     *
     * @param event the post-tick event
     */
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (intervalMode.get() != IntervalMode.TIME) return;

        long now = System.currentTimeMillis();
        long elapsedMs = now - lastFactTimestamp;
        long intervalMs = getTimeIntervalMillis();

        if (elapsedMs >= intervalMs) {
            lastFactTimestamp = now;
            sendCatFact();
        }
    }

    /**
     * Converts the configured time interval and unit into milliseconds.
     *
     * @return the time interval in milliseconds
     */
    private long getTimeIntervalMillis() {
        long value = timeInterval.get();
        switch (timeUnit.get()) {
            case HOURS:
                return value * 3600000L;
            case MINUTES:
                return value * 60000L;
            case SECONDS:
            default:
                return value * 1000L;
        }
    }

    /**
     * Resolves the API URL based on the configured fact source setting.
     *
     * @return the URL string to fetch a cat fact from
     */
    private String resolveApiUrl() {
        switch (factSource.get()) {
            case CATFACTS_CC:
                return CATFACTS_CC_URL;
            case MEOWFACTS:
                return MEOWFACTS_URL;
            case RANDOM:
            default:
                return RANDOM.nextBoolean() ? CATFACTS_CC_URL : MEOWFACTS_URL;
        }
    }

    /**
     * Manually triggers a cat fact to be fetched and sent in chat.
     * Can be called regardless of whether the module is active.
     */
    public void triggerCatFact() {
        sendCatFact();
    }

    /**
     * Asynchronously fetches a cat fact from the resolved API and sends it in chat.
     * Falls back to the configured fallback message on failure.
     */
    private void sendCatFact() {
        CompletableFuture.runAsync(() -> {
            try {
                String fact = fetchFact();
                if (fact != null && !fact.isEmpty()) {
                    ChatUtils.sendPlayerMsg(fact);
                } else {
                    ChatUtils.sendPlayerMsg(fallbackMessage.get());
                }
            } catch (Exception e) {
                warning("Failed to fetch cat fact, sending fallback.");
                ChatUtils.sendPlayerMsg(fallbackMessage.get());
                e.printStackTrace();
            }
        });
    }

    /**
     * Fetches a cat fact from the resolved API host.
     *
     * @return the cat fact string parsed from the API response
     * @throws Exception if the HTTP request or parsing fails
     */
    private String fetchFact() throws Exception {
        String apiUrl = resolveApiUrl();
        URL url = URI.create(apiUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Meteor Client Addon");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();

        return extractFactFromJson(content.toString());
    }

    /**
     * Extracts a fact string from a JSON response using regex matching.
     * Supports both catfacts.cc and meowfacts response formats.
     *
     * @param json the raw JSON string from the API
     * @return the extracted fact, or null if parsing fails
     */
    private String extractFactFromJson(String json) {
        Pattern pattern = Pattern.compile("\"data\"\\s*:\\s*\\[\\s*\"(.*?)\"\\s*]");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Returns the currently configured interval mode.
     *
     * @return the active interval mode
     */
    public IntervalMode getIntervalMode() {
        return intervalMode.get();
    }

    /**
     * Returns the number of messages remaining before the next fact is sent.
     * Only meaningful when interval mode is MESSAGES.
     *
     * @return the number of messages left until the next fact
     */
    public int getMessagesLeft() {
        return Math.max(0, (messageInterval.get()+1) - messageCounter);
    }

    /**
     * Returns the number of milliseconds remaining before the next fact is sent.
     * Only meaningful when interval mode is TIME.
     *
     * @return the time remaining in milliseconds until the next fact
     */
    public long getTimeLeft() {
        long elapsedMs = System.currentTimeMillis() - lastFactTimestamp;
        return Math.max(0, getTimeIntervalMillis() - elapsedMs);
    }

    /**
     * Formats the remaining time into a human-readable string (e.g. "2m 30s").
     *
     * @return a formatted string representing the time left until the next fact
     */
    public String getFormattedTimeLeft() {
        long ms = getTimeLeft();
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
