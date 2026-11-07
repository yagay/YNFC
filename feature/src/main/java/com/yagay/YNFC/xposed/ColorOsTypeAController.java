package com.yagay.YNFC.xposed;

import android.os.IBinder;

import java.lang.reflect.Method;

/**
 * ColorOS 17 NFC-A card configuration adapter, based on the device's installed NfcNci APK.
 *
 * The verified ColorOS 17 method ABI is:
 *   com.oplus.nfc.IVendorNfcAdapter.setHceTypeAConfig(
 *       boolean enabled, byte[] atqa, byte[] sak, byte[] uid)
 *
 * This is deliberately separate from the generic RF_CONFIG_WRITE hook. It only calls an
 * existing OEM API through its actual runtime AIDL interface; no raw/guessed NCI opcodes,
 * transaction numbers, permission bypasses or region-gate overrides are used.
 */
final class ColorOsTypeAController {
    private static final String SERVICE = "oplus_nfc_service";
    private static final String DESCRIPTOR = "com.oplus.nfc.IVendorNfcAdapter";
    private static final String STUB = "com.oplus.nfc.IVendorNfcAdapter$Stub";

    static final class Result {
        final boolean available;
        final boolean accepted;
        final String stage;
        final String detail;

        private Result(boolean available, boolean accepted, String stage, String detail) {
            this.available = available;
            this.accepted = accepted;
            this.stage = stage;
            this.detail = detail;
        }

        static Result unsupported(String detail) {
            return new Result(false, false, "OEM_UNAVAILABLE", detail);
        }

        static Result rejected(String stage, String detail) {
            return new Result(true, false, stage, detail);
        }

        static Result accepted() {
            return new Result(true, true, "OEM_METHOD_ACCEPTED", "ColorOS Type-A OEM method returned true; external RF UID not verified");
        }
    }

    Result apply(String uidHex, String sakHex, String atqaHex, ClassLoader nfcLoader) {
        return setEnabled(true, uidHex, sakHex, atqaHex, nfcLoader);
    }

    Result disable(String uidHex, String sakHex, String atqaHex, ClassLoader nfcLoader) {
        return setEnabled(false, uidHex, sakHex, atqaHex, nfcLoader);
    }

    private Result setEnabled(boolean enabled, String uidHex, String sakHex, String atqaHex, ClassLoader nfcLoader) {
        final byte[] uid;
        final byte[] sak;
        final byte[] atqa;
        try {
            uid = parseHex(uidHex, 4, 7, 10);
            sak = parseHex(sakHex == null || sakHex.trim().isEmpty() ? "08" : sakHex, 1);
            // Preserve the user-supplied ATQA bytes as-is. Display-vs-wire endian mapping
            // cannot be inferred solely from the OEM ABI and needs reader-side validation.
            atqa = parseHex(atqaHex == null || atqaHex.trim().isEmpty() ? "0400" : atqaHex, 2);
        } catch (IllegalArgumentException e) {
            return Result.rejected("OEM_INVALID_PARAMETERS", e.getMessage());
        }

        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method checkService = sm.getDeclaredMethod("checkService", String.class);
            checkService.setAccessible(true);
            Object raw = checkService.invoke(null, SERVICE);
            if (!(raw instanceof IBinder)) return Result.unsupported("ColorOS OPlus NFC service is not visible");
            IBinder binder = (IBinder) raw;
            String descriptor = binder.getInterfaceDescriptor();
            if (!DESCRIPTOR.equals(descriptor)) {
                return Result.unsupported("Unexpected ColorOS NFC descriptor: " + descriptor);
            }

            Object oem = binder.queryLocalInterface(descriptor);
            if (oem == null) {
                Class<?> stub = Class.forName(STUB, false, nfcLoader);
                Method asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
                asInterface.setAccessible(true);
                oem = asInterface.invoke(null, binder);
            }
            if (oem == null) return Result.unsupported("ColorOS vendor adapter implementation unavailable");

            Method setter = oem.getClass().getMethod(
                    "setHceTypeAConfig", boolean.class, byte[].class, byte[].class, byte[].class);
            // Confirmed device APK order: enabled, ATQA, SAK, UID.
            Object returned = setter.invoke(oem, enabled, atqa, sak, uid);
            return Boolean.TRUE.equals(returned)
                    ? new Result(true, true, enabled ? "OEM_METHOD_ACCEPTED" : "OEM_DISABLE_ACCEPTED",
                            enabled ? "ColorOS Type-A method returned true; external RF UID not verified"
                                    : "ColorOS Type-A disable method returned true; stock RF reset still required")
                    : Result.rejected(enabled ? "OEM_METHOD_REJECTED" : "OEM_DISABLE_REJECTED",
                            "ColorOS rejected Type-A configuration (NFC state, feature/region or native NCI failure)");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return Result.unsupported(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Throwable e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return Result.rejected("OEM_INVOCATION_FAILED",
                    cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()));
        }
    }

    static byte[] parseHex(String raw, int... validLengths) {
        String hex = NfcHookUtils.normalizeUid(raw);
        for (int len : validLengths) {
            if (hex.length() == len * 2) return NfcHookUtils.hexToBytes(hex);
        }
        throw new IllegalArgumentException("Expected " + lengths(validLengths) + " bytes, got " +
                (hex.length() / 2) + " for Type-A parameter");
    }

    private static String lengths(int[] lengths) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lengths.length; i++) {
            if (i > 0) out.append('/');
            out.append(lengths[i]);
        }
        return out.toString();
    }
}
