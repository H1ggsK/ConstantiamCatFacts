package cc.catfacts.catfactsaddon.hud;

import cc.catfacts.catfactsaddon.CatFactsAddon;
import cc.catfacts.catfactsaddon.modules.CatFactsModule;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class CatFactsHud extends HudElement {
    public static final HudElementInfo<CatFactsHud> INFO = new HudElementInfo<>(CatFactsAddon.HUD_GROUP, "cat-facts-hud", "Displays messages left until next cat fact.", CatFactsHud::new);

    public CatFactsHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        CatFactsModule module = Modules.get().get(CatFactsModule.class);

        String text;
        Color color;

        if (module != null && module.isActive()) {
            text = "Next Fact: " + module.getMessagesLeft() + " msgs";
            color = Color.WHITE;
        } else {
            text = "Cat Facts: Disabled";
            color = Color.RED;
        }

        setSize(renderer.textWidth(text, true), renderer.textHeight(true));
        renderer.text(text, x, y, color, true);
    }
}
