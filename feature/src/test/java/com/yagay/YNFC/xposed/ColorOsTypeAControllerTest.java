package com.yagay.YNFC.xposed;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColorOsTypeAControllerTest {
    @Test public void acceptsFourSevenAndTenByteUids() {
        assertArrayEquals(new byte[] {(byte) 0xc1, (byte) 0xb0, (byte) 0xbc, 0x1b},
                ColorOsTypeAController.parseHex("C1B0BC1B", 4, 7, 10));
        assertEquals(7, ColorOsTypeAController.parseHex("04 A1 B2 C3 D4 E5 F6", 4, 7, 10).length);
        assertEquals(10, ColorOsTypeAController.parseHex("0102030405060708090A", 4, 7, 10).length);
    }

    @Test public void preservesOemAtqaAndSakByteOrder() {
        assertArrayEquals(new byte[] {0x00, 0x04}, ColorOsTypeAController.parseHex("0004", 2));
        assertArrayEquals(new byte[] {0x08}, ColorOsTypeAController.parseHex("08", 1));
        assertArrayEquals(new byte[] {0x04, 0x00}, ColorOsTypeAController.parseHex("0400", 2));
    }

    @Test public void rejectsInvalidLengths() {
        try {
            ColorOsTypeAController.parseHex("C1B0BC", 4, 7, 10);
            fail("Too short UID must fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Expected"));
        }
    }
}
