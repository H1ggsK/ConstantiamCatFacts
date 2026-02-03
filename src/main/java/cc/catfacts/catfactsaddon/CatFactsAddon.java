package cc.catfacts.catfactsaddon;

import cc.catfacts.catfactsaddon.commands.CatFactsCommand;
import cc.catfacts.catfactsaddon.hud.CatFactsHud;
import cc.catfacts.catfactsaddon.modules.CatFactsModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class CatFactsAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Cat Facts");
    public static final HudGroup HUD_GROUP = new HudGroup("Cat Facts");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Cat Facts Addon");
        Modules.get().add(new CatFactsModule());
        Commands.add(new CatFactsCommand());
        Hud.get().register(CatFactsHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "cc.catfacts.catfactsaddon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("H1ggsK", "ConstantiamCatFacts");
    }
}
