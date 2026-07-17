package com.civicdesk.citizen.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link NationalIdUtil} (hashing and masking of the national ID). */
class NationalIdUtilTest {

    @Test
    @DisplayName("hash is a stable 64-char lowercase hex SHA-256 digest")
    void hashIsStableSha256() {
        String hash = NationalIdUtil.hash("ABCD12345678");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
        // Deterministic: same input -> same hash.
        assertThat(hash).isEqualTo(NationalIdUtil.hash("ABCD12345678"));
    }

    @Test
    @DisplayName("different inputs produce different hashes")
    void differentInputsDifferentHashes() {
        assertThat(NationalIdUtil.hash("ABCD12345678"))
                .isNotEqualTo(NationalIdUtil.hash("ABCD12345679"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("hash returns null for null/blank input")
    void hashNullForBlank(String input) {
        assertThat(NationalIdUtil.hash(input)).isNull();
    }

    @Test
    @DisplayName("maskLast4 masks all but the last four characters")
    void maskLast4Masks() {
        assertThat(NationalIdUtil.maskLast4("ABCD12345678")).isEqualTo("********5678");
    }

    @Test
    @DisplayName("maskLast4 returns the value unchanged when four characters or fewer")
    void maskLast4ShortValue() {
        assertThat(NationalIdUtil.maskLast4("1234")).isEqualTo("1234");
        assertThat(NationalIdUtil.maskLast4("12")).isEqualTo("12");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("maskLast4 returns null for null/blank input")
    void maskLast4NullForBlank(String input) {
        assertThat(NationalIdUtil.maskLast4(input)).isNull();
    }
}
