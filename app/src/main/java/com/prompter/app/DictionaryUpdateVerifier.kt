package com.prompter.app

import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONObject

data class DictionaryAliasUpdate(val surface: String, val tag: String)
data class VerifiedDictionaryUpdate(val version: Long, val aliases: List<DictionaryAliasUpdate>)

/** Validates the exact signed bytes before parsing or changing any local data. */
object DictionaryUpdateVerifier {
    private const val MAX_ENVELOPE_BYTES = 8 * 1024 * 1024
    private const val MAX_ALIASES = 100_000
    // RFC 8410 SubjectPublicKeyInfo, from the user's public key; the private key is not shipped.
    private const val PUBLIC_KEY = "MCowBQYDK2VwAyEAkkEShoQv2KoK+xXF54ppoWPc1I8MVDWqhJgFGBuT5m4="
    private val spkiPrefix = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)

    fun verify(envelopeBytes: ByteArray, currentVersion: Long): VerifiedDictionaryUpdate {
        require(envelopeBytes.isNotEmpty() && envelopeBytes.size <= MAX_ENVELOPE_BYTES) { "辞書ファイルのサイズが不正です。" }
        val envelope = JSONObject(envelopeBytes.toString(Charsets.UTF_8))
        require(envelope.optString("algorithm") == "Ed25519") { "署名方式が不正です。" }
        val payload = Base64.decode(envelope.getString("payload"), Base64.DEFAULT)
        val signature = Base64.decode(envelope.getString("signature"), Base64.DEFAULT)
        require(signature.size == 64) { "署名の形式が不正です。" }
        val key = Base64.decode(PUBLIC_KEY, Base64.DEFAULT)
        require(key.size == spkiPrefix.size + 32 && key.copyOfRange(0, spkiPrefix.size).contentEquals(spkiPrefix))
        val verifier = Ed25519Signer()
        verifier.init(false, Ed25519PublicKeyParameters(key, spkiPrefix.size))
        verifier.update(payload, 0, payload.size)
        require(verifier.verifySignature(signature)) { "辞書の署名を確認できませんでした。" }

        val json = JSONObject(payload.toString(Charsets.UTF_8))
        require(json.getInt("format") == 1) { "対応していない辞書形式です。" }
        val versionValue = json.get("version")
        require(versionValue is Int || versionValue is Long) { "バージョンが不正です。" }
        val version = (versionValue as Number).toLong()
        require(version > currentVersion && version > 0) { "辞書のバージョンが新しくありません。" }
        val entries = json.getJSONArray("aliases")
        require(entries.length() in 1..MAX_ALIASES) { "辞書の件数が不正です。" }
        val seen = HashSet<String>()
        val aliases = (0 until entries.length()).map { index ->
            val entry = entries.getJSONObject(index)
            val surface = entry.getString("surface")
            val tag = entry.getString("tag")
            val norm = M1Matcher.normalize(surface)
            require(surface.isNotBlank() && surface.length <= 128 && norm.isNotEmpty() && tag.matches(Regex("[a-z0-9_]{1,100}"))) {
                "辞書の項目が不正です。"
            }
            require(seen.add(norm)) { "同じ読みの項目が重複しています。" }
            DictionaryAliasUpdate(surface, tag)
        }
        return VerifiedDictionaryUpdate(version, aliases)
    }
}
