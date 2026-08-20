package app.omnisim.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal sealed interface ReleaseNoteBlock {
    data class Heading(val text: String, val level: Int) : ReleaseNoteBlock
    data class Bullet(val text: String) : ReleaseNoteBlock
    data class Numbered(val number: Int, val text: String) : ReleaseNoteBlock
    data class Paragraph(val text: String) : ReleaseNoteBlock
}

private val numberedReleaseNote = Regex("""^(\d+)\.\s+(.+)$""")

internal fun parseReleaseNotes(markdown: String): List<ReleaseNoteBlock> {
    val blocks = mutableListOf<ReleaseNoteBlock>()
    val paragraph = mutableListOf<String>()
    val listItem = mutableListOf<String>()
    var listNumber: Int? = null
    var isBullet = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += ReleaseNoteBlock.Paragraph(cleanInlineMarkdown(paragraph.joinToString(" ")))
            paragraph.clear()
        }
    }

    fun flushListItem() {
        if (listItem.isEmpty()) return
        val text = cleanInlineMarkdown(listItem.joinToString(" "))
        blocks += listNumber?.let { ReleaseNoteBlock.Numbered(it, text) }
            ?: ReleaseNoteBlock.Bullet(text)
        listItem.clear()
        listNumber = null
        isBullet = false
    }

    markdown.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        val isIndented = rawLine.firstOrNull()?.isWhitespace() == true
        val numberedMatch = numberedReleaseNote.matchEntire(line)
        when {
            line.isEmpty() -> {
                flushListItem()
                flushParagraph()
            }
            line == "---" -> {
                flushListItem()
                flushParagraph()
            }
            line.startsWith("### ") -> {
                flushListItem()
                flushParagraph()
                blocks += ReleaseNoteBlock.Heading(cleanInlineMarkdown(line.drop(4)), 3)
            }
            line.startsWith("## ") -> {
                flushListItem()
                flushParagraph()
                blocks += ReleaseNoteBlock.Heading(cleanInlineMarkdown(line.drop(3)), 2)
            }
            line.startsWith("# ") -> {
                flushListItem()
                flushParagraph()
                blocks += ReleaseNoteBlock.Heading(cleanInlineMarkdown(line.drop(2)), 1)
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                flushListItem()
                flushParagraph()
                isBullet = true
                listItem += line.drop(2).trim()
            }
            numberedMatch != null -> {
                flushListItem()
                flushParagraph()
                listNumber = numberedMatch.groupValues[1].toInt()
                listItem += numberedMatch.groupValues[2]
            }
            (isBullet || listNumber != null) && isIndented -> listItem += line
            isBullet || listNumber != null -> {
                flushListItem()
                paragraph += line
            }
            else -> paragraph += line
        }
    }
    flushListItem()
    flushParagraph()
    return blocks
}

private fun cleanInlineMarkdown(value: String): String = value
    .replace("**", "")
    .replace("__", "")
    .replace("`", "")

@Composable
internal fun ReleaseNotesContent(markdown: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parseReleaseNotes(markdown).forEach { block ->
            when (block) {
                is ReleaseNoteBlock.Heading -> Text(
                    text = block.text,
                    style = if (block.level == 1) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                )
                is ReleaseNoteBlock.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("•")
                    Text(block.text, modifier = Modifier.weight(1f))
                }
                is ReleaseNoteBlock.Numbered -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${block.number}.")
                    Text(block.text, modifier = Modifier.weight(1f))
                }
                is ReleaseNoteBlock.Paragraph -> Text(block.text)
            }
        }
    }
}
