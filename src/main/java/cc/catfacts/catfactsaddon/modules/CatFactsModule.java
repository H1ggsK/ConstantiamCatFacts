package cc.catfacts.catfactsaddon.modules;

import cc.catfacts.catfactsaddon.CatFactsAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatFactsModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> apiHost = sgGeneral.add(new StringSetting.Builder()
        .name("api-host")
        .description("The URL to fetch facts from.")
        .defaultValue("https://catfacts.cc/fact")
        .build()
    );

    private final Setting<Integer> messageInterval = sgGeneral.add(new IntSetting.Builder()
        .name("message-interval")
        .description("Messages to wait before sending a fact.")
        .defaultValue(10000)
        .min(1)
        .sliderMax(20000)
        .build()
    );

    private int messageCounter = 0;

    public CatFactsModule() {
        super(CatFactsAddon.CATEGORY, "cat-facts-spammer", "Sends a cat fact periodically.");
    }

    @Override
    public void onActivate() {
        messageCounter = 0;
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        messageCounter++;

        if (messageCounter >= (messageInterval.get()+1)) {
            messageCounter = 0;
            sendCatFact();
        }
    }

    private void sendCatFact() {
        CompletableFuture.runAsync(() -> {
            try {
                String fact = fetchFact();
                if (fact != null && !fact.isEmpty()) {
                    ChatUtils.sendPlayerMsg(fact);
                }
            } catch (Exception e) {
                warning("Failed to fetch cat fact.");
                e.printStackTrace();
            }
        });
    }

    private String fetchFact() throws Exception {
        URL url = new URL(apiHost.get());
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

    private String extractFactFromJson(String json) {
        Pattern pattern = Pattern.compile("\"data\"\\s*:\\s*\\[\\s*\"(.*?)\"\\s*]");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Cats are mysterious (Error parsing fact).";
    }

    public int getMessagesLeft() {
        return Math.max(0, (messageInterval.get()+1) - messageCounter);
    }
}
