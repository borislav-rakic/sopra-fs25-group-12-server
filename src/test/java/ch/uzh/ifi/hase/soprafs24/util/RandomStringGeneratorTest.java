package ch.uzh.ifi.hase.soprafs24.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RandomStringGeneratorTest {

    @Test
    public void generateRandomString_hasCorrectLength() {
        int length = 6;
        String result = RandomStringGenerator.generateRandomString(length);
        assertNotNull(result);
        assertEquals(length, result.length(), "Generated string should have length " + length);
    }

    @RepeatedTest(10)
    public void generateRandomString_isAlphanumericOnly() {
        String result = RandomStringGenerator.generateRandomString(6);
        assertTrue(result.matches("^[A-Za-z0-9]+$"), "String should be alphanumeric: " + result);
    }

    @Test
    public void generateRandomString_lengthZero_returnsEmptyString() {
        String result = RandomStringGenerator.generateRandomString(0);
        assertEquals("", result, "Expected empty string when length is zero");
    }

    @Test
    public void generateRandomString_returnsDifferentStrings() {
        String result1 = RandomStringGenerator.generateRandomString(6);
        String result2 = RandomStringGenerator.generateRandomString(6);
        assertNotEquals(result1, result2, "Two consecutive strings should usually be different");
    }
}
