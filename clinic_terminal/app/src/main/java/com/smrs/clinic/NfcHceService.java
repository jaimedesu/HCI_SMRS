package com.smrs.clinic;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * NfcHceService — Host Card Emulation broadcaster.
 *
 * Chunks the student JSON payload and serves it to the
 * student phone over ISO-DEP APDU commands.
 *
 * Protocol:
 *   1. Student sends SELECT AID  → we reply 90 00
 *   2. Student sends GET DATA    → we reply first chunk + status
 *   3. Student sends GET NEXT    → we reply next chunk + status
 *   4. Last chunk ends with 90 00 (done), others with 61 00 (more)
 */
public class NfcHceService extends HostApduService {

    private static final String TAG = "NfcHceService";

    // Custom SMRS AID bytes: F0 53 4D 52 53 00
    private static final byte[] SMRS_AID = {
        (byte) 0xF0, (byte) 0x53, (byte) 0x4D,
        (byte) 0x52, (byte) 0x53, (byte) 0x00
    };

    // ISO 7816 status words
    private static final byte[] SW_OK       = { (byte) 0x90, (byte) 0x00 };
    private static final byte[] SW_MORE     = { (byte) 0x61, (byte) 0x00 };
    private static final byte[] SW_NOT_FOUND = { (byte) 0x6A, (byte) 0x82 };
    private static final byte[] SW_UNKNOWN  = { (byte) 0x6D, (byte) 0x00 };

    // Max bytes per NFC chunk (safe for Android HCE)
    private static final int CHUNK_SIZE = 200;

    // Payload held in static field so MainActivity can set it
    // without needing a service binding.
    private static volatile String sPayloadJson = null;

    // Per-session state (reset on deactivate)
    private byte[][] mChunks   = null;
    private int      mChunkIdx = 0;

    /** Called by MainActivity when a student is loaded. */
    public static void setPayload(String json) {
        sPayloadJson = json;
        if (json != null) {
            Log.i(TAG, "New HCE Payload set. Length: " + json.length() + " chars.");
        } else {
            Log.i(TAG, "HCE Payload cleared.");
        }
    }

    /** Called by MainActivity when the Clear button is tapped. */
    public static void clearPayload() {
        sPayloadJson = null;
        Log.d(TAG, "Payload cleared");
    }

    public static boolean hasPayload() {
        return sPayloadJson != null;
    }

    // ── APDU handler ─────────────────────────────────────────────────

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        if (apdu == null || apdu.length < 2) {
            return SW_UNKNOWN;
        }

        Log.d(TAG, "APDU in: " + hex(apdu));

        // SELECT AID command: CLA=00 INS=A4 P1=04 P2=00 Lc=06 AID[6]
        if (isSelectAid(apdu)) {
            return handleSelect();
        }

        // GET DATA (first): CLA=00 INS=CA P1=00 P2=00
        if (apdu[0] == 0x00 && apdu[1] == (byte) 0xCA &&
            apdu[2] == 0x00 && apdu[3] == 0x00) {
            return handleGetData();
        }

        // GET NEXT chunk: CLA=00 INS=CA P1=00 P2=01
        if (apdu[0] == 0x00 && apdu[1] == (byte) 0xCA &&
            apdu[2] == 0x00 && apdu[3] == 0x01) {
            return handleGetNext();
        }

        Log.w(TAG, "Unknown APDU: " + hex(apdu));
        return SW_UNKNOWN;
    }

    private byte[] handleSelect() {
        if (sPayloadJson == null) {
            Log.w(TAG, "SELECT AID received but sPayloadJson is NULL. Returning 6A82.");
            return SW_NOT_FOUND;
        }
        try {
            byte[] data = sPayloadJson.getBytes(StandardCharsets.UTF_8);
            mChunks   = chunkArray(data, CHUNK_SIZE);
            mChunkIdx = 0;
            Log.i(TAG, "SELECT AID Success. Payload size: " + data.length + " bytes. Prepared " + mChunks.length + " chunks.");
            return SW_OK;
        } catch (Exception e) {
            Log.e(TAG, "Error preparing chunks", e);
            return SW_UNKNOWN;
        }
    }

    private byte[] handleGetData() {
        if (mChunks == null || mChunkIdx >= mChunks.length) {
            return SW_NOT_FOUND;
        }
        return sendChunk();
    }

    private byte[] handleGetNext() {
        if (mChunks == null || mChunkIdx >= mChunks.length) {
            return SW_NOT_FOUND;
        }
        return sendChunk();
    }

    private byte[] sendChunk() {
        byte[] chunk  = mChunks[mChunkIdx];
        boolean isLast = (mChunkIdx == mChunks.length - 1);
        mChunkIdx++;

        byte[] sw = isLast ? SW_OK : SW_MORE;
        byte[] response = new byte[chunk.length + sw.length];
        System.arraycopy(chunk, 0, response, 0, chunk.length);
        System.arraycopy(sw, 0, response, chunk.length, sw.length);

        Log.d(TAG, "Sending chunk " + mChunkIdx + "/" + mChunks.length
            + " (" + chunk.length + "b) last=" + isLast);
        return response;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCE deactivated reason=" + reason);
        mChunks   = null;
        mChunkIdx = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private boolean isSelectAid(byte[] apdu) {
        // Minimum: 00 A4 04 00 Lc [AID bytes]
        // Some systems might use P2=0C or other values
        if (apdu.length < 5) return false;
        if (apdu[0] != 0x00 || apdu[1] != (byte) 0xA4) return false;
        if (apdu[2] != 0x04) return false; // P1 must be 04
        
        int lc = apdu[4] & 0xFF;
        if (apdu.length < 5 + lc) return false;
        if (lc != SMRS_AID.length) return false;

        for (int i = 0; i < lc; i++) {
            if (apdu[5 + i] != SMRS_AID[i]) return false;
        }
        return true;
    }

    private static byte[][] chunkArray(byte[] data, int size) {
        int count = (int) Math.ceil((double) data.length / size);
        byte[][] result = new byte[count][];
        for (int i = 0; i < count; i++) {
            int start = i * size;
            int end   = Math.min(start + size, data.length);
            result[i] = Arrays.copyOfRange(data, start, end);
        }
        return result;
    }

    private static String hex(byte[] b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X ", x));
        return sb.toString().trim();
    }
}
