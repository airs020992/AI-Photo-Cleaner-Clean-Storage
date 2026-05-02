package com.air.cleaner.core.i18n

data class RegionalDesignProfile(
    val layoutDirection: LayoutDirectionPreference,
    val visualDensity: VisualDensity,
    val copyTone: CopyTone,
    val trustCues: Set<TrustCue>,
) {
    companion object {
        fun forLanguageTag(languageTag: String): RegionalDesignProfile {
            val language = languageTag.lowercase().substringBefore("-")
            return when (language) {
                "ar" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Rtl,
                    visualDensity = VisualDensity.Comfortable,
                    copyTone = CopyTone.Reassuring,
                    trustCues = setOf(
                        TrustCue.UserControl,
                        TrustCue.CulturalNeutrality,
                        TrustCue.SafeDeletion,
                    ),
                )
                "de", "fr", "nl", "sv" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Standard,
                    copyTone = CopyTone.PrivacyFirst,
                    trustCues = setOf(
                        TrustCue.Privacy,
                        TrustCue.SubscriptionClarity,
                        TrustCue.SafeDeletion,
                    ),
                )
                "ja", "ko" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Compact,
                    copyTone = CopyTone.Precise,
                    trustCues = setOf(
                        TrustCue.Accuracy,
                        TrustCue.UserControl,
                        TrustCue.SafeDeletion,
                    ),
                )
                "es", "pt" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Standard,
                    copyTone = CopyTone.BenefitForward,
                    trustCues = setOf(
                        TrustCue.VisibleResult,
                        TrustCue.ValueForMoney,
                        TrustCue.FreePreview,
                    ),
                )
                "hi", "id", "vi", "th", "tr", "pl", "ru", "el", "it" -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Comfortable,
                    copyTone = CopyTone.ResultFirst,
                    trustCues = setOf(
                        TrustCue.VisibleResult,
                        TrustCue.FreePreview,
                        TrustCue.SafeDeletion,
                    ),
                )
                else -> RegionalDesignProfile(
                    layoutDirection = LayoutDirectionPreference.Ltr,
                    visualDensity = VisualDensity.Standard,
                    copyTone = CopyTone.Direct,
                    trustCues = setOf(
                        TrustCue.Privacy,
                        TrustCue.UserControl,
                        TrustCue.SafeDeletion,
                    ),
                )
            }
        }
    }
}

enum class LayoutDirectionPreference {
    Ltr,
    Rtl,
}

enum class VisualDensity {
    Compact,
    Standard,
    Comfortable,
}

enum class CopyTone {
    Direct,
    PrivacyFirst,
    Precise,
    BenefitForward,
    ResultFirst,
    Reassuring,
}

enum class TrustCue {
    Privacy,
    UserControl,
    SubscriptionClarity,
    CulturalNeutrality,
    Accuracy,
    ValueForMoney,
    VisibleResult,
    FreePreview,
    SafeDeletion,
}
