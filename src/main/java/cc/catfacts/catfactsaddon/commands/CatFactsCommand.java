package cc.catfacts.catfactsaddon.commands;

import cc.catfacts.catfactsaddon.modules.CatFactsModule;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

/**
 * A command that reports the current status of the CatFactsModule,
 * displaying either remaining messages or remaining time depending on the active interval mode.
 */
public class CatFactsCommand extends Command {

    /**
     * Constructs the catfacts status command.
     */
    public CatFactsCommand() {
        super("catfacts", "Checks status of the cat facts spammer.");
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
    }
}
