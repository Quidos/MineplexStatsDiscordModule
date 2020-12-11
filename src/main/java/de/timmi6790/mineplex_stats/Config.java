package de.timmi6790.mineplex_stats;

import lombok.Data;

@Data
public class Config {
    private String apiName = "";
    private String apiPassword = "";
    private String apiUrl = "https://mpstats.timmi6790.de/";
    private int apiTimeout = 6_000;
}
