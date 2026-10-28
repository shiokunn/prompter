package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterIdentityTest {
    @Test fun confirmedNameIsKeptAndEmptyNameChangesNothing() {
        assertEquals("The character is Cinderella. A girl smiles.",
            CharacterIdentity.prefix("A girl smiles.", "Cinderella"))
        assertEquals("A girl smiles.", CharacterIdentity.prefix("A girl smiles.", ""))
    }
}
