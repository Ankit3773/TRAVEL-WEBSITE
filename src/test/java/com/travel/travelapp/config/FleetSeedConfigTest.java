package com.travel.travelapp.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FleetSeedConfigTest {

    @Test
    void departureSpacingShouldFollowBusCountRules() {
        assertThat(FleetSeedConfig.departureSpacingMinutes(1)).isEqualTo(0);
        assertThat(FleetSeedConfig.departureSpacingMinutes(3)).isEqualTo(180);
        assertThat(FleetSeedConfig.departureSpacingMinutes(5)).isEqualTo(90);
        assertThat(FleetSeedConfig.departureSpacingMinutes(6)).isEqualTo(60);
    }
}
