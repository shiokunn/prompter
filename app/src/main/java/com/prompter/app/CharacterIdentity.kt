package com.prompter.app

/** An explicitly confirmed English name; search titles are never treated as identity. */
object CharacterIdentity {
    fun prefix(prompt: String, englishName: String): String {
        val name = englishName.trim()
        if (name.isEmpty()) return prompt
        return "The character is $name. " + prompt.trim()
    }
}
