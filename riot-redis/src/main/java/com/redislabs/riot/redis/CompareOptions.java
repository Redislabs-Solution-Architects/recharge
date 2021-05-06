package com.redislabs.riot.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import java.time.Duration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareOptions {

    public static final long DEFAULT_TTL_TOLERANCE_IN_SECONDS = 1;

    @Builder.Default
    @CommandLine.Option(names = "--ttl-tolerance", description = "Max TTL difference to use for dataset verification (default: ${DEFAULT-VALUE}).", paramLabel = "<sec>")
    private long ttlTolerance = DEFAULT_TTL_TOLERANCE_IN_SECONDS;
    @CommandLine.Option(names = "--show-diffs", description = "Print details of key mismatches during dataset verification")
    private boolean showDiffs;

    public Duration getTtlToleranceDuration() {
        return Duration.ofSeconds(ttlTolerance);
    }
}
