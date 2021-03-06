package de.timmi6790.mineplex_stats.statsapi.models.bedrock;

import de.timmi6790.mineplex_stats.statsapi.models.ResponseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class BedrockPlayerStats extends ResponseModel {
    private final Info info;
    private final Map<String, Stats> stats;


    @Data
    public static class Info {
        private final String name;
        private final boolean filter;
    }

    @Data
    public static class Stats {
        private final String game;
        private final int position;
        private final int score;
        private final int unix;
    }
}
