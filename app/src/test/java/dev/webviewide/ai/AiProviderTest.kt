package dev.webviewide.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiProviderTest {
    @Test
    fun provider_lookup_is_stable() {
        assertEquals(AiProvider.ANTHROPIC, AiProvider.fromId("ANTHROPIC"))
        assertEquals(AiProvider.OPENAI, AiProvider.fromId("unknown"))
    }

    @Test
    fun provider_defaults_are_complete() {
        AiProvider.entries.forEach { provider ->
            assert(provider.defaultModel.isNotBlank())
            assert(provider.title.isNotBlank())
        }
        assert(AiProvider.CUSTOM.defaultBaseUrl.isBlank())
    }
}
