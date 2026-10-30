package com.prompter.app

/** An explicitly confirmed English name; search titles are never treated as identity. */
object CharacterIdentity {
    fun prefix(prompt: String, englishName: String, workName: String = ""): String {
        val name = englishName.trim()
        if (name.isEmpty()) return prompt
        val work = workName.trim()
        val identity = if (work.isEmpty()) name else "$name from $work"
        return "The character is $identity. " + prompt.trim()
    }
}
