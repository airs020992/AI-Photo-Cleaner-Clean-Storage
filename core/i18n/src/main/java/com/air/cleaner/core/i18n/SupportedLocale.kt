package com.air.cleaner.core.i18n

data class SupportedLocale(
    val languageTag: String,
    val displayName: String,
    val launchPriority: LaunchPriority,
)

enum class LaunchPriority {
    Primary,
    Expansion,
}

val supportedLocales = listOf(
    SupportedLocale("en-US", "English", LaunchPriority.Primary),
    SupportedLocale("zh-CN", "简体中文", LaunchPriority.Primary),
    SupportedLocale("es", "Español", LaunchPriority.Primary),
    SupportedLocale("pt-BR", "Português (Brasil)", LaunchPriority.Primary),
    SupportedLocale("ru", "Русский", LaunchPriority.Primary),
    SupportedLocale("ja", "日本語", LaunchPriority.Primary),
    SupportedLocale("ko", "한국어", LaunchPriority.Primary),
    SupportedLocale("de", "Deutsch", LaunchPriority.Primary),
    SupportedLocale("fr", "Français", LaunchPriority.Primary),
    SupportedLocale("ar", "العربية", LaunchPriority.Primary),
    SupportedLocale("id", "Bahasa Indonesia", LaunchPriority.Expansion),
    SupportedLocale("th", "ไทย", LaunchPriority.Expansion),
    SupportedLocale("vi", "Tiếng Việt", LaunchPriority.Expansion),
    SupportedLocale("tr", "Türkçe", LaunchPriority.Expansion),
    SupportedLocale("it", "Italiano", LaunchPriority.Expansion),
    SupportedLocale("el", "Ελληνικά", LaunchPriority.Expansion),
    SupportedLocale("pl", "Polski", LaunchPriority.Expansion),
    SupportedLocale("nl", "Nederlands", LaunchPriority.Expansion),
    SupportedLocale("sv", "Svenska", LaunchPriority.Expansion),
    SupportedLocale("hi", "हिन्दी", LaunchPriority.Expansion),
)
