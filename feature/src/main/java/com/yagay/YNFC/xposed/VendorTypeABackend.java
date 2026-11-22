package com.yagay.YNFC.xposed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ColorOS/OxygenOS Type-A adapter. Called ONLY from the hooked com.android.nfc process.
 *
 * Uses the OEM public-to-system vendor facade, not raw NCI writes, hidden signature grants,
 * SE applets or routing-table overrides. A true return means the OEM API accepted the request,
 * NOT that an external reader confirmed the card or power-off operation.
 */
final class VendorTypeABackend {
    static final class Result {
        final boolean accepted;
        final String detail;
        Result(boolean accepted, String detail) {
            this.accepted = accepted;
            this.detail = detail;
        }
    }

    Result execute(ClassLoader nfcLoader, SimConfig cfg) {
        byte[] uid = hex(cfg.uid, 4, 7, 10);
        byte[] sak = hex(cfg.sak, 1);
        byte[] atqa = hex(cfg.atqa, 2);
        if (uid == null || sak == null || atqa == null) {
            return new Result(false, "Invalid NFC-A identity (UID=4/7/10 bytes, SAK=1, ATQA=2)");
        }
        try {
            Class<?> adapterClass = Class.forName(
                    "com.vendor.nfc.VendorNfcAdapter", false, nfcLoader);
            Method factory = adapterClass.getMethod("getVendorNfcAdapter");
            Object adapter = factory.invoke(null);
            if (adapter == null) {
                return new Result(false, "VendorNfcAdapter factory returned null");
            }
            Method method = adapterClass.getMethod(
                    "setHceTypeAConfig", boolean.class, byte[].class, byte[].class, byte[].class);
            Object result = method.invoke(adapter, cfg.active, atqa, sak, uid);
            if (Boolean.TRUE.equals(result)) {
                return new Result(true, cfg.active ?
                        "OEM Type-A API accepted APPLY; external reader verification required" :
                        "OEM Type-A API accepted STOP; verify stock card routing");
            }
            return new Result(false, "OEM setHceTypeAConfig returned false");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return new Result(false, cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } catch (ReflectiveOperationException | SecurityException | LinkageError e) {
            return new Result(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static byte[] hex(String value, int... lengths) {
        if (value == null || !value.matches("[0-9A-Fa-f]+") || (value.length() & 1) != 0)
            return null;
        int size = value.length() / 2;
        boolean accepted = false;
        for (int length : lengths) if (length == size) accepted = true;
        if (!accepted) return null;
        byte[] output = new byte[size];
        for (int i = 0; i < size; i++) {
            int hi = Character.digit(value.charAt(i * 2), 16);
            int lo = Character.digit(value.charAt(i * 2 + 1), 16);
            output[i] = (byte) ((hi << 4) | lo);
        }
        return output;
    }
}
