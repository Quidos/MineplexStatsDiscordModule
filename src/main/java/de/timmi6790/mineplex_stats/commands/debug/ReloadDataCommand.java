package de.timmi6790.mineplex_stats.commands.debug;

import de.timmi6790.commons.utilities.EnumUtilities;
import de.timmi6790.discord_framework.modules.command.CommandParameters;
import de.timmi6790.discord_framework.modules.command.CommandResult;
import de.timmi6790.discord_framework.modules.command.property.properties.MinArgCommandProperty;
import de.timmi6790.mineplex_stats.commands.AbstractStatsCommand;
import net.dv8tion.jda.api.utils.MarkdownUtil;

public class ReloadDataCommand extends AbstractStatsCommand {
    public ReloadDataCommand() {
        super("sReload", "Debug", "", "<javaGame|javaGroup|bedrockGame>", "sr");

        this.addProperties(
                new MinArgCommandProperty(1)
        );
    }

    @Override
    protected CommandResult onCommand(final CommandParameters commandParameters) {
        final ValidArgs0 arg0 = this.getFromEnumIgnoreCaseThrow(commandParameters, 0, ValidArgs0.values());

        switch (arg0) {
            case JAVA_GAME:
                this.getMineplexStatsModule().loadJavaGames();
                break;

            case JAVA_GROUP:
                this.getMineplexStatsModule().loadJavaGroups();
                break;

            case BEDROCK_GAME:
                this.getMineplexStatsModule().loadBedrockGames();
                break;
        }

        sendTimedMessage(
                commandParameters,
                getEmbedBuilder(commandParameters)
                        .setTitle("Reloaded data")
                        .setDescription("Reloaded " + MarkdownUtil.monospace(EnumUtilities.getPrettyName(arg0))),
                90
        );
        return CommandResult.SUCCESS;
    }

    private enum ValidArgs0 {
        JAVA_GAME,
        JAVA_GROUP,
        BEDROCK_GAME
    }
}
