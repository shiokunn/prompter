package com.prompter.app

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OpenRouterClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendsJsonSchemaAndParsesTheResult() {
        val content = JSONObject()
            .put("pixai_prompt", "An anime-style illustration of a girl with long black hair.")
            .put("tags", JSONArray().put("1girl").put("long_hair"))
            .toString()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatResponse(content)),
        )

        val result = OpenRouterClient(server.url("/").toString()).refine(
            apiKey = "test-key",
            model = "test-model",
            input = "長い黒髪の少女",
            offlineTags = listOf("1girl", "long_hair"),
            unmatched = "",
        )

        assertEquals("An anime-style illustration of a girl with long black hair.", result.pixaiPrompt)
        assertEquals(listOf("1girl", "long_hair"), result.tags)

        val request = server.takeRequest()
        assertEquals("/chat/completions", request.path)
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("test-model", body.getString("model"))
        assertEquals(
            "json_schema",
            body.getJSONObject("response_format").getString("type"),
        )
        assertTrue(body.getJSONArray("messages").length() == 2)
    }

    @Test
    fun retriesWithJsonObjectWhenSchemaIsUnsupported() {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":{"message":"response_format json_schema is not supported"}}"""),
        )
        val content = JSONObject()
            .put("pixai_prompt", "An anime-style illustration.")
            .put("tags", JSONArray().put("1girl"))
            .toString()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatResponse(content)),
        )

        val result = OpenRouterClient(server.url("/").toString()).refine(
            apiKey = "test-key",
            model = "free-model",
            input = "少女",
            offlineTags = listOf("1girl"),
            unmatched = "",
        )
        assertEquals(listOf("1girl"), result.tags)

        val first = JSONObject(server.takeRequest().body.readUtf8())
        val second = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("json_schema", first.getJSONObject("response_format").getString("type"))
        assertEquals("json_object", second.getJSONObject("response_format").getString("type"))
    }

    @Test
    fun retriesWithJsonObjectWhenContentIsNull() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"content":null}}]}"""),
        )
        val content = JSONObject()
            .put("pixai_prompt", "An anime-style illustration.")
            .put("tags", JSONArray().put("1girl"))
            .toString()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatResponse(content)),
        )

        val result = OpenRouterClient(server.url("/").toString()).refine(
            apiKey = "test-key",
            model = "openrouter/free",
            input = "少女",
            offlineTags = listOf("1girl"),
            unmatched = "",
        )
        assertEquals(listOf("1girl"), result.tags)

        val first = JSONObject(server.takeRequest().body.readUtf8())
        val second = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("json_schema", first.getJSONObject("response_format").getString("type"))
        assertEquals("json_object", second.getJSONObject("response_format").getString("type"))
    }

    @Test
    fun mapsUnauthorizedErrorsWithoutLeakingTheKey() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"No auth credentials found"}}"""),
        )

        try {
            OpenRouterClient(server.url("/").toString()).refine(
                apiKey = "secret-key",
                model = "test-model",
                input = "少女",
                offlineTags = listOf("1girl"),
                unmatched = "",
            )
            fail("Expected OpenRouterException")
        } catch (error: OpenRouterException) {
            assertEquals(401, error.statusCode)
            assertTrue(error.message!!.contains("APIキー"))
            assertTrue(!error.message!!.contains("secret-key"))
        }
    }

    @Test
    fun extractsJsonFromMarkdownFences() {
        val client = OpenRouterClient()
        val parsed = client.parseResult(
            """
            ```json
            {"pixai_prompt":"A prompt.","tags":["1girl"]}
            ```
            """.trimIndent(),
        )
        assertEquals("A prompt.", parsed.pixaiPrompt)
        assertEquals(listOf("1girl"), parsed.tags)
    }

    private fun chatResponse(content: String): String =
        JSONObject()
            .put(
                "choices",
                JSONArray().put(
                    JSONObject().put(
                        "message",
                        JSONObject().put("content", content),
                    ),
                ),
            )
            .toString()
}
