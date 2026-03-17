package live.vio.VioCore

import live.vio.VioCore.models.Component
import live.vio.VioCore.models.SponsorSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SponsorSlotParsingTest {

    @Test
    fun testParseSponsorSlotFromComponent() {
        val config = mapOf(
            "type" to "poll_cta",
            "config" to mapOf(
                "title" to "Help us decide!",
                "text" to "Cast your vote now."
            )
        )
        
        val component = Component(
            id = "comp-1",
            type = "sponsor_slots",
            name = "Sponsor Moment",
            config = config,
            status = "active"
        )
        
        val sponsorSlot = component.toSponsorSlot()
        
        assertNotNull(sponsorSlot)
        assertEquals("poll_cta", sponsorSlot?.type)
        @Suppress("UNCHECKED_CAST")
        val innerConfig = sponsorSlot?.config as Map<String, Any?>
        assertEquals("Help us decide!", innerConfig["title"])
        assertEquals("Cast your vote now.", innerConfig["text"])
    }

    @Test
    fun testParseProductSponsorSlot() {
        val config = mapOf(
            "type" to "product",
            "config" to mapOf(
                "title" to "Awesome Product",
                "description" to "You need this."
            )
        )
        
        val component = Component(
            id = "comp-2",
            type = "sponsor_slots",
            name = "Product Moment",
            config = config,
            status = "active"
        )
        
        val sponsorSlot = component.toSponsorSlot()
        
        assertNotNull(sponsorSlot)
        assertEquals("product", sponsorSlot?.type)
        assertEquals("Awesome Product", sponsorSlot?.config?.get("title"))
    }
}
