package app.omnisim.android.domain.util

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

data class PhoneNumberRegion(
    val regionCode: String,
    val callingCode: String,
)

data class CallingCodeCountry(
    val regionCode: String,
    val countryName: String,
    val englishName: String,
    val callingCode: String,
    val flag: String,
)

data class PhoneInputParts(
    val regionCode: String?,
    val nationalNumber: String,
)

fun detectPhoneNumberRegion(value: String): PhoneNumberRegion? {
    val input = value.trim()
    if (!input.startsWith("+")) return null

    val phoneUtil = PhoneNumberUtil.getInstance()
    val number = runCatching { phoneUtil.parse(input, null) }.getOrNull() ?: return null
    if (!phoneUtil.isPossibleNumber(number)) return null

    val regionCode = phoneUtil.getRegionCodeForNumber(number)
        ?.takeUnless { it == PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY }
        ?: return null
    return PhoneNumberRegion(
        regionCode = regionCode,
        callingCode = "+${number.countryCode}",
    )
}

fun localizedCountryName(regionCode: String, locale: Locale = Locale.getDefault()): String =
    Locale.Builder()
        .setRegion(regionCode)
        .build()
        .getDisplayCountry(locale)

fun resolvePhoneNumberRegionCode(value: String?, fallbackRegionCode: String?): String? =
    detectPhoneNumberRegion(value.orEmpty())?.regionCode
        ?: fallbackRegionCode
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { code -> code.length == 2 && code.all { it in 'A'..'Z' } }

fun countryFlag(regionCode: String?): String? {
    val normalized = regionCode
        ?.trim()
        ?.uppercase(Locale.ROOT)
        ?.takeIf { code -> code.length == 2 && code.all { it in 'A'..'Z' } }
        ?: return null
    return normalized
        .map { char -> String(Character.toChars(0x1F1E6 + char.code - 'A'.code)) }
        .joinToString("")
}

fun callingCodeCountries(locale: Locale = Locale.getDefault()): List<CallingCodeCountry> {
    val phoneUtil = PhoneNumberUtil.getInstance()
    return phoneUtil.supportedRegions.mapNotNull { regionCode ->
        val callingCode = phoneUtil.getCountryCodeForRegion(regionCode)
        if (callingCode <= 0) return@mapNotNull null
        CallingCodeCountry(
            regionCode = regionCode,
            countryName = localizedCountryName(regionCode, locale),
            englishName = localizedCountryName(regionCode, Locale.ENGLISH),
            callingCode = "+$callingCode",
            flag = checkNotNull(countryFlag(regionCode)),
        )
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, CallingCodeCountry::countryName))
}

fun splitInternationalPhoneNumber(
    value: String?,
    fallbackRegionCode: String? = null,
    locale: Locale = Locale.getDefault(),
): PhoneInputParts {
    val input = value.orEmpty().trim()
    val countries = callingCodeCountries(locale)
    val fallback = countries.firstOrNull {
        it.regionCode.equals(fallbackRegionCode, ignoreCase = true)
    } ?: fallbackRegionCode
        ?.removePrefix("+")
        ?.toIntOrNull()
        ?.let(PhoneNumberUtil.getInstance()::getRegionCodeForCountryCode)
        ?.let { primaryRegion -> countries.firstOrNull { it.regionCode == primaryRegion } }
    if (input.isBlank()) return PhoneInputParts(fallback?.regionCode, "")

    val detectedRegion = detectPhoneNumberRegion(input)?.regionCode
    val detected = countries.firstOrNull { it.regionCode == detectedRegion }
        ?: countries
            .filter { input.startsWith(it.callingCode) }
            .maxByOrNull { it.callingCode.length }
        ?: fallback
    val nationalNumber = if (input.startsWith("+") && detected != null) {
        input.removePrefix(detected.callingCode).trimStart()
    } else {
        input
    }
    return PhoneInputParts(detected?.regionCode, nationalNumber)
}

fun formatInternationalPhoneNumber(regionCode: String, nationalNumber: String): String? {
    val localNumber = nationalNumber.trim()
    if (localNumber.isBlank()) return null

    val phoneUtil = PhoneNumberUtil.getInstance()
    val parsed = runCatching { phoneUtil.parse(localNumber, regionCode) }.getOrNull()
    if (parsed != null) {
        return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
    }
    val callingCode = phoneUtil.getCountryCodeForRegion(regionCode).takeIf { it > 0 }
        ?: return localNumber
    return "+$callingCode $localNumber"
}
