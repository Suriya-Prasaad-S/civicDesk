package com.civicdesk.citizen.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the {@link CitizenStatus} code mapping. */
class CitizenStatusTest {

    @Test
    @DisplayName("each constant exposes its single-character code")
    void codes() {
        assertThat(CitizenStatus.Active.getCode()).isEqualTo("A");
        assertThat(CitizenStatus.Verified.getCode()).isEqualTo("V");
        assertThat(CitizenStatus.Flagged.getCode()).isEqualTo("F");
    }

    @Test
    @DisplayName("fromCode resolves a code case-insensitively")
    void fromCodeCaseInsensitive() {
        assertThat(CitizenStatus.fromCode("v")).isEqualTo(CitizenStatus.Verified);
        assertThat(CitizenStatus.fromCode("A")).isEqualTo(CitizenStatus.Active);
    }

    @Test
    @DisplayName("fromCode throws for an unknown code")
    void fromCodeUnknown() {
        assertThatThrownBy(() -> CitizenStatus.fromCode("Z"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("allowedCodes lists every code")
    void allowedCodes() {
        assertThat(CitizenStatus.allowedCodes()).isEqualTo("A, V, F");
    }
}
