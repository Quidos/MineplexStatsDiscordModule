package de.timmi6790.mineplex_stats.statsapi.models.java;

import de.timmi6790.mineplex_stats.statsapi.models.ResponseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class JavaPlayerStats extends ResponseModel {
    private final Info info;
    private final Map<String, WebsiteStat> websiteStats;
    private final Map<String, Stat> stats;


    @Data
    @AllArgsConstructor
    public static class Info {
        private final UUID uuid;
        private final String name;
        private final String game;
        private final String board;
        private final boolean filter;
    }

    @Data
    @AllArgsConstructor
    public static class Stat {
        private final String statName;
        private final int position;
        private final long score;
        private final int unix;
    }

    @Data
    @AllArgsConstructor
    public static class WebsiteStat {
        private final String stat;
        private final long score;
    }
}
