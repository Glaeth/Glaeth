package com.clipsmile.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DenNameTest {
    @Test
    fun poisonedShopNameFallsBackToTheSpecies() {
        assertEquals("Kıpır", Den.keptName("kipir", "Çirkin", "Kıpır"))
        assertEquals("Tekir", Den.keptName("tekir", "  Çirkin  ", "Tekir"))
        assertEquals("Çirkin", Den.keptName("pug", "Çirkin", "Çirkin"))
    }

    @Test
    fun aChosenNameStays() {
        assertEquals("Boncuk", Den.keptName("kipir", "Boncuk", "Kıpır"))
        assertEquals("Minnos", Den.keptName("cat", null, "Minnos"))
        assertEquals("Gece", Den.keptName("gece", "   ", "Gece"))
    }
}
