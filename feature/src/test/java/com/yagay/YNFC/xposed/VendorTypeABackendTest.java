package com.yagay.YNFC.xposed;

import org.junit.Test;

import static org.junit.Assert.*;

public class VendorTypeABackendTest {
    @Test public void supportsValidUidsAndAtqaSak() {
        assertEquals(4, VendorTypeABackend.hex("A1B2C3D4", 4, 7, 10).length);
        assertEquals(7, VendorTypeABackend.hex("01020304050607", 4, 7, 10).length);
        assertEquals(10, VendorTypeABackend.hex("0102030405060708090A", 4, 7, 10).length);
        assertArrayEquals(new byte[]{(byte) 0x04, 0}, VendorTypeABackend.hex("0400", 2));
        assertArrayEquals(new byte[]{(byte) 0x08}, VendorTypeABackend.hex("08", 1));
    }

    @Test public void rejectsInvalidParametersBeforeNfcCall() {
        assertNull(VendorTypeABackend.hex("01", 4, 7, 10));
        assertNull(VendorTypeABackend.hex("AABBCCDD0", 4));
        assertNull(VendorTypeABackend.hex("00GG", 2));
        assertNull(VendorTypeABackend.hex(null, 2));
    }
}
