package com.prompter.app

import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DictionaryUpdateVerifierTest {
    private data class Keys(
        val privateKey: Ed25519PrivateKeyParameters,
        val publicKeyBase64: String,
    )

    private fun keys(): Keys {
        val privateKey = Ed25519PrivateKeyParameters(SecureRandom())
        val publicKey = privateKey.generatePublicKey()
        val spki = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00,
        ) + publicKey.encoded
        return Keys(privateKey, Base64.encodeToString(spki, Base64.NO_WRAP))
    }

    private fun sign(payload: String, key: Ed25519PrivateKeyParameters): String {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val signer = Ed25519Signer()
        signer.init(true, key)
        signer.update(bytes, 0, bytes.size)
        return JSONObject()
            .put("algorithm", "Ed25519")
            .put("payload", Base64.encodeToString(bytes, Base64.NO_WRAP))
            .put("signature", Base64.encodeToString(signer.generateSignature(), Base64.NO_WRAP))
            .toString()
    }

    @Test
    fun acceptsGenuinelySignedPayload() {
        val keys = keys()
        val payload = """{"format":1,"version":3,"aliases":[{"surface":"立ち絵","tag":"standing"}]}"""
        val result = DictionaryUpdateVerifier.verifyWithKey(
            sign(payload, keys.privateKey).toByteArray(),
            currentVersion = 2,
            publicKeyBase64 = keys.publicKeyBase64,
        )
        assertEquals(3L, result.version)
        assertEquals("立ち絵", result.aliases.single().surface)
        assertEquals("standing", result.aliases.single().tag)
    }

    @Test
    fun rejectsTamperedPayload() {
        val keys = keys()
        val signed = sign("""{"format":1,"version":3,"aliases":[{"surface":"立ち絵","tag":"standing"}]}""", keys.privateKey)
        val envelope = JSONObject(signed)
        val replacement = """{"format":1,"version":3,"aliases":[{"surface":"立ち絵","tag":"cat_ears"}]}"""
        envelope.put("payload", Base64.encodeToString(replacement.toByteArray(), Base64.NO_WRAP))
        assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateVerifier.verifyWithKey(envelope.toString().toByteArray(), 2, keys.publicKeyBase64)
        }
    }

    @Test
    fun rejectsRollback() {
        val keys = keys()
        val payload = """{"format":1,"version":2,"aliases":[{"surface":"立ち絵","tag":"standing"}]}"""
        val failure = assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateVerifier.verifyWithKey(
                sign(payload, keys.privateKey).toByteArray(),
                currentVersion = 2,
                publicKeyBase64 = keys.publicKeyBase64,
            )
        }
        assertTrue(failure.message.orEmpty().contains("バージョン"))
    }

    @Test
    fun rejectsUnsignedPayload() {
        val payload = """{"format":1,"version":1,"aliases":[{"surface":"黒髪","tag":"black_hair"}]}"""
        val envelope = JSONObject().put("algorithm", "Ed25519")
            .put("payload", Base64.encodeToString(payload.toByteArray(), Base64.NO_WRAP))
            .put("signature", Base64.encodeToString(ByteArray(64), Base64.NO_WRAP))
        assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateVerifier.verify(envelope.toString().toByteArray(), 0)
        }
    }

    @Test
    fun rejectsOversizedEnvelope() {
        assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateVerifier.verify(ByteArray(8 * 1024 * 1024 + 1), 0)
        }
    }
}
