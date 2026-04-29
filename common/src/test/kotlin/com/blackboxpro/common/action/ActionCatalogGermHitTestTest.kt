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
}
