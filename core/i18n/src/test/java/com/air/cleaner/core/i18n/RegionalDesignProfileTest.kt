package com.air.cleaner.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalDesignProfileTest {
    @Test
    fun supportsTwentyLaunchLocales() {
        assertEquals(20, supportedLocales.size)
        assertTrue(supportedLocales.any { it.languageTag == "en-US" })
        assertTrue(supportedLocales.any { it.languageTag == "ar" })
        assertTrue(supportedLocales.any { it.languageTag == "hi" })
    }

    @Test
    fun arabicUsesRtlComfortableLayoutAndSafeDeletionTrust() {
        val profile = RegionalDesignProfile.forLanguageTag("ar")

        assertEquals(LayoutDirectionPreference.Rtl, profile.layoutDirection)
        assertEquals(VisualDensity.Comfortable, profile.visualDensity)
        assertEquals(CopyTone.Reassuring, profile.copyTone)
        assertTrue(profile.trustCues.contains(TrustCue.UserControl))
        assertTrue(profile.trustCues.contains(TrustCue.SafeDeletion))
    }

    @Test
    fun germanUsesPrivacyFirstToneAndSubscriptionClarity() {
        val profile = RegionalDesignProfile.forLanguageTag("de")

        assertEquals(LayoutDirectionPreference.Ltr, profile.layoutDirection)
        assertEquals(CopyTone.PrivacyFirst, profile.copyTone)
        assertTrue(profile.trustCues.contains(TrustCue.Privacy))
        assertTrue(profile.trustCues.contains(TrustCue.SubscriptionClarity))
    }

    @Test
    fun japaneseUsesCompactPreciseHighTrustPresentation() {
        val profile = RegionalDesignProfile.forLanguageTag("ja")

        assertEquals(VisualDensity.Compact, profile.visualDensity)
        assertEquals(CopyTone.Precise, profile.copyTone)
        assertTrue(profile.trustCues.contains(TrustCue.Accuracy))
    }

    @Test
    fun hindiUsesResultFirstFreePreviewBeforePaywall() {
        val profile = RegionalDesignProfile.forLanguageTag("hi")

        assertEquals(VisualDensity.Comfortable, profile.visualDensity)
        assertEquals(CopyTone.ResultFirst, profile.copyTone)
        assertTrue(profile.trustCues.contains(TrustCue.VisibleResult))
        assertTrue(profile.trustCues.contains(TrustCue.FreePreview))
    }

    @Test
    fun brazilianPortugueseUsesBenefitForwardValueFraming() {
        val profile = RegionalDesignProfile.forLanguageTag("pt-BR")

        assertEquals(CopyTone.BenefitForward, profile.copyTone)
        assertTrue(profile.trustCues.contains(TrustCue.ValueForMoney))
        assertTrue(profile.trustCues.contains(TrustCue.VisibleResult))
    }
}
