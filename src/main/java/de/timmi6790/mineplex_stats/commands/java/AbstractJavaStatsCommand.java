package de.timmi6790.mineplex_stats.commands.java;

import de.timmi6790.discord_framework.modules.command.CommandModule;
import de.timmi6790.discord_framework.modules.command.CommandParameters;
import de.timmi6790.discord_framework.modules.command.exceptions.CommandReturnException;
import de.timmi6790.discord_framework.utilities.DataUtilities;
import de.timmi6790.minecraft.mojang_api.MojangApi;
import de.timmi6790.minecraft.mojang_api.models.MojangUser;
import de.timmi6790.minecraft.utilities.JavaUtilities;
import de.timmi6790.mineplex_stats.commands.AbstractStatsCommand;
import de.timmi6790.mineplex_stats.commands.java.info.JavaGamesCommand;
import de.timmi6790.mineplex_stats.commands.java.info.JavaGroupsGroupsCommand;
import de.timmi6790.mineplex_stats.settings.JavaNameReplacementSetting;
import de.timmi6790.mineplex_stats.settings.NameReplacementSetting;
import de.timmi6790.mineplex_stats.statsapi.models.java.JavaBoard;
import de.timmi6790.mineplex_stats.statsapi.models.java.JavaGame;
import de.timmi6790.mineplex_stats.statsapi.models.java.JavaGroup;
import de.timmi6790.mineplex_stats.statsapi.models.java.JavaStat;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
public abstract class AbstractJavaStatsCommand extends AbstractStatsCommand {
    private static final List<String> STATS_TIME = Arrays.asList("Ingame Time", "Hub Time", "Time Playing");

    protected AbstractJavaStatsCommand(final String name,
                                       final String description,
                                       final String syntax,
                                       final String... aliasNames) {
        super(name, "MineplexStats - Java", description, syntax, aliasNames);
    }

    protected String getFormattedScore(final JavaStat stat, final long score) {
        if (STATS_TIME.contains(stat.getName())) {
            return this.getFormattedTime(score);
        }

        return this.getFormattedNumber(score);
    }

    protected CompletableFuture<BufferedImage> getPlayerSkin(@NonNull final UUID uuid) {
        return JavaUtilities.getPlayerSkin(uuid);
    }

    protected UUID getPlayerUUIDFromName(final CommandParameters commandParameters, final int argPos) {
        final String playerName = this.getPlayer(commandParameters, argPos);
        final Optional<MojangUser> mojangUser = MojangApi.getUser(playerName);
        if (mojangUser.isPresent()) {
            return mojangUser.get().getUuid();
        }

        throw new CommandReturnException(
                this.getEmbedBuilder(commandParameters)
                        .setTitle("Invalid User")
                        .appendDescription(
                                "The user %s does not exist.\n" +
                                        "Are you sure that you typed his name correctly?",
                                MarkdownUtil.monospace(playerName)
                        )
        );
    }

    // Arg Parsing
    protected JavaGame getGame(final CommandParameters commandParameters, final int argPos) {
        final String name = commandParameters.getArgs()[argPos];
        final Optional<JavaGame> game = this.getMineplexStatsModule().getJavaGame(name);
        if (game.isPresent()) {
            return game.get();
        }

        final List<JavaGame> similarGames = this.getMineplexStatsModule().getSimilarJavaGames(name, 0.6, 3);
        if (!similarGames.isEmpty() && commandParameters.getUserDb().hasAutoCorrection()) {
            return similarGames.get(0);
        }

        this.sendHelpMessage(
                commandParameters,
                name,
                argPos,
                "game",
                this.getMineplexStatsModule().getModuleOrThrow(CommandModule.class)
                        .getCommand(JavaGamesCommand.class)
                        .orElse(null),
                new String[0],
                similarGames.stream()
                        .map(JavaGame::getName)
                        .collect(Collectors.toList())
        );
        throw new CommandReturnException();
    }

    protected JavaStat getStat(final JavaGame game, final CommandParameters commandParameters, final int argPos) {
        final String name = commandParameters.getArgs()[argPos];
        final Optional<JavaStat> stat = game.getStat(name);
        if (stat.isPresent()) {
            return stat.get();
        }

        final List<JavaStat> similarStats = game.getSimilarStats(JavaGame.getCleanStat(name), 0.6, 3);
        if (!similarStats.isEmpty() && commandParameters.getUserDb().hasAutoCorrection()) {
            return similarStats.get(0);
        }

        this.sendHelpMessage(
                commandParameters,
                name,
                argPos,
                "stat",
                this.getMineplexStatsModule().getModuleOrThrow(CommandModule.class)
                        .getCommand(JavaGamesCommand.class)
                        .orElse(null),
                new String[]{game.getName()},
                similarStats.stream()
                        .map(JavaStat::getName)
                        .collect(Collectors.toList())
        );
        throw new CommandReturnException();
    }

    protected JavaStat getStat(final CommandParameters commandParameters, final int argPos) {
        final String name = commandParameters.getArgs()[argPos];
        final Optional<JavaStat> stat = this.getMineplexStatsModule().getJavaGames().values()
                .stream()
                .map(game -> game.getStat(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparingInt(javaStat -> javaStat.getBoards().size()));
        if (stat.isPresent()) {
            return stat.get();
        }

        this.sendTimedMessage(
                commandParameters,
                this.getEmbedBuilder(commandParameters)
                        .setTitle("Invalid Stat")
                        .setDescription(MarkdownUtil.monospace(name) + " is not a valid stat. " +
                                "\nTODO: Add help emotes." +
                                "\n In the meantime just use any valid stat name, if that is not working scream at me."),
                90
        );

        throw new CommandReturnException();
    }

    protected JavaBoard getBoard(final JavaGame game, final CommandParameters commandParameters, final int argPos) {
        final String name;
        if (argPos >= commandParameters.getArgs().length || commandParameters.getArgs()[argPos] == null) {
            name = "All";
        } else {
            name = commandParameters.getArgs()[argPos];
        }

        for (final JavaStat stat : game.getStats().values()) {
            final Optional<JavaBoard> javaBoardOpt = stat.getBoard(name);
            if (javaBoardOpt.isPresent()) {
                return javaBoardOpt.get();
            }
        }

        final List<String> similarBoards = DataUtilities.getSimilarityList(
                name,
                game.getStats()
                        .values()
                        .stream()
                        .flatMap(stat -> stat.getBoardNames().stream())
                        .collect(Collectors.toSet()),
                0.0,
                6
        );
        if (!similarBoards.isEmpty() && commandParameters.getUserDb().hasAutoCorrection()) {
            final String newBoard = similarBoards.get(0);
            final Optional<JavaBoard> similarBoard = game.getStats().values().stream()
                    .flatMap(stat -> stat.getBoards().values().stream())
                    .filter(board -> board.getName().equalsIgnoreCase(newBoard))
                    .findAny();

            if (similarBoard.isPresent()) {
                return similarBoard.get();
            }
        }

        this.sendHelpMessage(
                commandParameters,
                name,
                argPos,
                "board",
                this.getMineplexStatsModule().getModuleOrThrow(CommandModule.class)
                        .getCommand(JavaGamesCommand.class)
                        .orElse(null),
                new String[]{game.getName(), game.getStats().values().stream().findFirst().map(JavaStat::getName).orElse("")},
                similarBoards
        );
        throw new CommandReturnException();
    }

    protected JavaBoard getBoard(final JavaGame game,
                                 final JavaStat stat,
                                 final CommandParameters commandParameters,
                                 final int argPos) {
        final String name = argPos >= commandParameters.getArgs().length ? "All" : commandParameters.getArgs()[argPos];
        final Optional<JavaBoard> board = stat.getBoard(name);
        if (board.isPresent()) {
            return board.get();
        }

        final List<JavaBoard> similarBoards = stat.getSimilarBoard(name, 0.0, 6);
        if (!similarBoards.isEmpty() && commandParameters.getUserDb().hasAutoCorrection()) {
            return similarBoards.get(0);
        }

        this.sendHelpMessage(
                commandParameters,
                name,
                argPos,
                "board",
                this.getMineplexStatsModule().getModuleOrThrow(CommandModule.class)
                        .getCommand(JavaGamesCommand.class)
                        .orElse(null),
                new String[]{game.getName(), stat.getName()},
                similarBoards.stream()
                        .map(JavaBoard::getName)
                        .collect(Collectors.toList())
        );
        throw new CommandReturnException();
    }

    protected JavaBoard getBoard(final JavaStat stat, final CommandParameters commandParameters, final int argPos) {
        final String name = argPos >= commandParameters.getArgs().length ? "All" : commandParameters.getArgs()[argPos];
        final Optional<JavaBoard> board = stat.getBoard(name);
        if (board.isPresent()) {
            return board.get();
        }

        this.sendTimedMessage(
                commandParameters,
                this.getEmbedBuilder(commandParameters)
                        .setTitle("Invalid Board")
                        .setDescription(MarkdownUtil.monospace(name) + " is not a valid board. " +
                                "\nTODO: Add help emotes." +
                                "\nHow did you do this?! Just pick all, yearly, weekly, daily or monthly."),
                90
        );

        throw new CommandReturnException();
    }

    protected String getPlayer(final CommandParameters commandParameters, final int argPos) {
        String name = commandParameters.getArgs()[argPos];

        // Check for setting
        if (name.equalsIgnoreCase(NameReplacementSetting.getKeyword())) {
            final String settingName = commandParameters.getUserDb().getSettingOrDefault(JavaNameReplacementSetting.class, "");
            if (!settingName.isEmpty()) {
                name = settingName;
            }
        }

        if (JavaUtilities.isValidName(name)) {
            return name;
        }

        throw new CommandReturnException(
                this.getEmbedBuilder(commandParameters)
                        .setTitle("Invalid Name")
                        .setDescription(MarkdownUtil.monospace(name) + " is not a minecraft name.")
        );
    }

    public JavaGroup getJavaGroup(final CommandParameters commandParameters, final int argPos) {
        final String name = commandParameters.getArgs()[argPos];
        final Optional<JavaGroup> group = this.getMineplexStatsModule().getJavaGroup(name);
        if (group.isPresent()) {
            return group.get();
        }

        final List<JavaGroup> similarGroup = this.getMineplexStatsModule().getSimilarJavaGroups(name, 0.6, 3);
        if (!similarGroup.isEmpty() && commandParameters.getUserDb().hasAutoCorrection()) {
            return similarGroup.get(0);
        }

        this.sendHelpMessage(
                commandParameters,
                name,
                argPos,
                "group",
                this.getMineplexStatsModule().getModuleOrThrow(CommandModule.class)
                        .getCommand(JavaGroupsGroupsCommand.class)
                        .orElse(null),
                new String[]{},
                similarGroup.stream()
                        .map(JavaGroup::getName)
                        .collect(Collectors.toList())
        );
        throw new CommandReturnException();
    }

    public JavaStat getJavaStat(final JavaGroup group, final CommandParameters commandParameters, final int argPos) {
        final String name = commandParameters.getArgs()[argPos];

        for (final JavaGame game : group.getGames()) {
            final Optional<JavaStat> statOpt = game.getStat(name);
            if (statOpt.isPresent()) {
                return statOpt.get();
            }
        }

        final List<JavaStat> similarStats = DataUtilities.getSimilarityList(
                JavaGame.getCleanStat(name),
                group.getStats(),
                JavaStat::getName,
                0.6,
                3
        );
        if (!similarStats.isEmpty() && commandParameters.getUserDb().hasAutoCorrection()) {
            return similarStats.get(0);
        }

        this.sendHelpMessage(
                commandParameters,
                name,
                argPos,
                "stat",
                this.getMineplexStatsModule().getModuleOrThrow(CommandModule.class)
                        .getCommand(JavaGroupsGroupsCommand.class)
                        .orElse(null),
                new String[]{group.getName()},
                similarStats.stream()
                        .map(JavaStat::getName)
                        .collect(Collectors.toList())
        );
        throw new CommandReturnException();
    }
}
