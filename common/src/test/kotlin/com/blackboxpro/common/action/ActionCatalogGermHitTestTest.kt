package com.blackboxpro.common.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionCatalogGermHitTestTest {

    @Test
    fun `catalog exposes Germ hit-test query with stable params`() {
        assertTrue(ActionCatalog.contains("query_germ_hit_test"))
        assertEquals(
            listOf("x", "y", "maxDepth", "maxComponents", "includeFields"),
            ActionCatalog.getParams("query_germ_hit_test")
        )
    }

    @Test
    fun `catalog exposes explicit Germ component click with stable params`() {
        assertTrue(ActionCatalog.contains("click_germ_component"))
        assertEquals(
            listOf(
                "x",
                "y",
                "componentId",
                "button",
                "clickCount",
                "maxDepth",
                "maxComponents",
                "includeFields",
                "fallbackScreenClick"
            ),
            ActionCatalog.getParams("click_germ_component")
        )
    }

    @Test
    fun `catalog exposes explicit Germ gui part dos with stable params`() {
        assertTrue(ActionCatalog.contains("germ_gui_part_dos"))
        assertEquals(
            listOf("guiName", "partId", "dosType", "execute", "resolvePlaceholders", "mode"),
            ActionCatalog.getParams("germ_gui_part_dos")
        )
    }
}
