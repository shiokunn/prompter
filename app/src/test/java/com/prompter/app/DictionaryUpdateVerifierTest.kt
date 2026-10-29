package com.prompter.app

import android.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DictionaryUpdateVerifierTest {
    @Test fun rejectsUnsignedPayload() {
        val payload = """{"format":1,"version":1,"aliases":[{"surface":"黒髪","tag":"black_hair"}]}"""
        val envelope = JSONObject().put("algorithm", "Ed25519")
            .put("payload", Base64.encodeToString(payload.toByteArray(), Base64.NO_WRAP))
            .put("signature", Base64.encodeToString(ByteArray(64), Base64.NO_WRAP))
        assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateVerifier.verify(envelope.toString().toByteArray(), 0)
        }
    }

    @Test fun rejectsOversizedEnvelope() {
        assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateVerifier.verify(ByteArray(8 * 1024 * 1024 + 1), 0)
        }
    }
}
