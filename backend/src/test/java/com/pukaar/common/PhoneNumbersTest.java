package com.pukaar.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumbersTest {

    @Test
    void acceptsValidIndianMobile() {
        assertEquals("+919840090000", PhoneNumbers.toE164("9840090000"));
        assertTrue(PhoneNumbers.isDeliverable("9840090000"));
    }

    @Test
    void rejectsNineDigitNumbers() {
        assertFalse(PhoneNumbers.isDeliverable("985500044"));
        assertThrows(ApiException.class, () -> PhoneNumbers.toE164("985500044"));
    }

    @Test
    void sameNumberMatchesMixedFormats() {
        assertTrue(PhoneNumbers.sameNumber("+919840090000", "9840090000"));
        assertTrue(PhoneNumbers.sameNumber("919840090000", "+91 98400 90000"));
    }
}
