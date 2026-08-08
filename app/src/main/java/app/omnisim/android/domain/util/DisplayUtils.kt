package app.omnisim.android.domain.util

fun maskPhoneNumber(phoneNumber: String?): String? {
    if (phoneNumber.isNullOrBlank()) return null
    val digits = phoneNumber.filter(Char::isDigit)
    if (digits.length <= 4) return "••••"
    val prefix = phoneNumber.trim().takeWhile { it == '+' || it.isDigit() }
        .takeIf { it.startsWith('+') }
        ?.take(3)
        ?.trimEnd()
        .orEmpty()
    return listOf(prefix, "•••", "•••", digits.takeLast(4))
        .filter(String::isNotBlank)
        .joinToString(" ")
}

