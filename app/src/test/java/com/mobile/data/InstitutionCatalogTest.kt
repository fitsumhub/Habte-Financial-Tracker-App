package com.mobile.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for InstitutionCatalog — validates catalog data integrity and
 * the data-driven SMS sender lookup that replaced SmsParser's old hardcoded
 * when-chain.
 */
class InstitutionCatalogTest {

    @Test
    fun `catalog is non-empty`() {
        assertTrue(InstitutionCatalog.ALL.isNotEmpty())
    }

    @Test
    fun `all institutions have unique IDs`() {
        val ids = InstitutionCatalog.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `all institutions have unique short names`() {
        val shortNames = InstitutionCatalog.ALL.map { it.shortName }
        assertEquals(shortNames.size, shortNames.toSet().size)
    }

    @Test
    fun `all institutions have valid color hex strings`() {
        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
        InstitutionCatalog.ALL.forEach { institution ->
            assertTrue("${institution.name} colorFrom invalid: ${institution.colorFrom}", hexRegex.matches(institution.colorFrom))
            assertTrue("${institution.name} colorTo invalid: ${institution.colorTo}", hexRegex.matches(institution.colorTo))
        }
    }

    @Test
    fun `all institutions have at least one SMS matcher`() {
        InstitutionCatalog.ALL.forEach { institution ->
            assertTrue(
                "${institution.name} has no smsContains or smsExact matchers",
                institution.smsContains.isNotEmpty() || institution.smsExact.isNotEmpty()
            )
        }
    }

    @Test
    fun `all institutions declare at least one supported account type`() {
        InstitutionCatalog.ALL.forEach { institution ->
            assertTrue("${institution.name} declares no supported account types", institution.supportedAccountTypes.isNotEmpty())
        }
    }

    @Test
    fun `Telebirr is classified as a digital wallet`() {
        val wallets = InstitutionCatalog.ALL.filter { it.type == InstitutionType.DIGITAL_WALLET }
        assertTrue(wallets.any { it.shortName == "TEL" })
    }

    @Test
    fun `Data PRESET_BANKS mirrors the catalog`() {
        assertEquals(InstitutionCatalog.ALL.size, Data.PRESET_BANKS.size)
        val catalogShortNames = InstitutionCatalog.ALL.map { it.shortName }.toSet()
        val bankShortNames = Data.PRESET_BANKS.map { it.shortName }.toSet()
        assertEquals(catalogShortNames, bankShortNames)
    }

    // ── findBySmsSender ───────────────────────────────────────────────────

    @Test
    fun `findBySmsSender matches by bank name substring`() {
        assertEquals("CBE", InstitutionCatalog.findBySmsSender("CBE")?.shortName)
        assertEquals("DAS", InstitutionCatalog.findBySmsSender("dashen")?.shortName)
    }

    @Test
    fun `findBySmsSender matches CBE exact numeric shortcodes`() {
        assertEquals("CBE", InstitutionCatalog.findBySmsSender("1000")?.shortName)
        assertEquals("CBE", InstitutionCatalog.findBySmsSender("8008")?.shortName)
    }

    @Test
    fun `findBySmsSender matches Telebirr shortcodes`() {
        assertEquals("TEL", InstitutionCatalog.findBySmsSender("tele")?.shortName)
        assertEquals("TEL", InstitutionCatalog.findBySmsSender("127")?.shortName)
        assertEquals("TEL", InstitutionCatalog.findBySmsSender("*127#")?.shortName)
    }

    @Test
    fun `findBySmsSender returns null for unknown sender`() {
        assertNull(InstitutionCatalog.findBySmsSender("UnknownBank"))
    }
}
