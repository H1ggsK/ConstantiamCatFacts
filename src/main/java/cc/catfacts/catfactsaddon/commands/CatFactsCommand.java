package cc.catfacts.catfactsaddon.commands;

import cc.catfacts.catfactsaddon.modules.CatFactsModule;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

/**
 * A command that reports the current status of the CatFactsModule
 * and allows manually triggering a cat fact on demand.
 */
public class CatFactsCommand extends Command {

    /**
     * Constructs the catfacts command.
     */
    public CatFactsCommand() {
        super("catfacts", "Check status or manually send a cat fact.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            CatFactsModule module = Modules.get().get(CatFactsModule.class);
            if (module == null) return 0;

            if (module.isActive()) {
                if (module.getIntervalMode() == CatFactsModule.IntervalMode.TIME) {
                    info("Time until next cat fact: " + module.getFormattedTimeLeft());
                } else {
                    info("Messages until next cat fact: " + module.getMessagesLeft());
                }
            } else {
                warning("Cat Facts module is currently disabled.");
            }
            return 1;
        });

        builder.then(literal("send").executes(context -> {
            CatFactsModule module = Modules.get().get(CatFactsModule.class);
            if (module == null) return 0;

            info("Fetching cat fact...");
            module.triggerCatFact();
            return 1;
        }));

        builder.then(literal("status").executes(context -> {
            CatFactsModule module = Modules.get().get(CatFactsModule.class);
            if (module == null) return 0;

            if (module.isActive()) {
                if (module.getIntervalMode() == CatFactsModule.IntervalMode.TIME) {
                    info("Mode: Time | Next fact in: " + module.getFormattedTimeLeft());
                } else {
                    info("Mode: Messages | Next fact in: " + module.getMessagesLeft() + " msgs");
                }
            } else {
                warning("Cat Facts module is currently disabled.");
            }
            return 1;
        }));
    }
}
