package cc.catfacts.catfactsaddon.hud;

import cc.catfacts.catfactsaddon.CatFactsAddon;
import cc.catfacts.catfactsaddon.modules.CatFactsModule;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * A HUD element that displays the remaining messages or time until the next cat fact is sent.
 * Adapts its display based on the active interval mode of the CatFactsModule.
 */
public class CatFactsHud extends HudElement {
    public static final HudElementInfo<CatFactsHud> INFO = new HudElementInfo<>(CatFactsAddon.HUD_GROUP, "cat-facts-hud", "Displays time or messages left until next cat fact.", CatFactsHud::new);

    /**
     * Constructs the CatFactsHud element.
     */
    public CatFactsHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        CatFactsModule module = Modules.get().get(CatFactsModule.class);

        String text;
        Color color;

        if (module != null && module.isActive()) {
            if (module.getIntervalMode() == CatFactsModule.IntervalMode.TIME) {
                text = "Next Fact: " + module.getFormattedTimeLeft();
            } else {
                text = "Next Fact: " + module.getMessagesLeft() + " msgs";
            }
            color = Color.WHITE;
        } else {
            text = "Cat Facts: Disabled";
            color = Color.RED;
        }

        setSize(renderer.textWidth(text, true), renderer.textHeight(true));
        renderer.text(text, x, y, color, true);
    }
}
