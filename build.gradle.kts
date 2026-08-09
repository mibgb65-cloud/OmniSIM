plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.3.21" apply false
}

tasks.register("checkCodeFileLength") {
    group = "verification"
    description = "Fails when a Kotlin source file exceeds 600 physical lines."

    doLast {
        val limit = 600
        val violations = fileTree(rootDir) {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", ".gradle/**", ".kotlin/**")
        }.mapNotNull { file ->
            val lines = file.useLines { sequence -> sequence.count() }
            if (lines > limit) file.relativeTo(rootDir).invariantSeparatorsPath to lines else null
        }.sortedByDescending { it.second }

        if (violations.isNotEmpty()) {
            val details = violations.joinToString(separator = "\n") { (file, lines) ->
                " - $file: $lines lines"
            }
            throw GradleException("Kotlin files must not exceed $limit lines:\n$details")
        }
    }
}
