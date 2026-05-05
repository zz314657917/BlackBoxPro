package com.blackboxpro.common.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionCatalogKeyboardTest {

    @Test
    fun `catalog exposes keyboard actions with stable params`() {
        assertTrue(ActionCatalog.contains("key_press"))
        assertEquals(listOf("key", "keyCode", "char", "pressTicks"), ActionCatalog.getParams("key_press"))

        assertTrue(ActionCatalog.contains("type_text"))
        assertEquals(listOf("text", "intervalTicks"), ActionCatalog.getParams("type_text"))
    }
}
