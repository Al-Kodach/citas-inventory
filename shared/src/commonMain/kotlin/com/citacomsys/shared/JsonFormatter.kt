package com.citacomsys.shared

object JsonFormatter {
    fun format(json: String): String {
        val indentStr = "    "
        var level = 0
        val result = StringBuilder()
        var inQuotes = false

        for (i in json.indices) {
            val char = json[i]
            when (char) {
                '"' -> {
                    // Handle escaped quotes safely
                    if (i > 0 && json[i - 1] != '\\') {
                        inQuotes = !inQuotes
                    }
                    result.append(char)
                }

                '{', '[' -> {
                    result.append(char)
                    if (!inQuotes) {
                        result.append("\n")
                        level++
                        repeat(level) { result.append(indentStr) }
                    }
                }

                '}', ']' -> {
                    if (!inQuotes) {
                        result.append("\n")
                        level--
                        repeat(level) { result.append(indentStr) }
                    }
                    result.append(char)
                }

                ',' -> {
                    result.append(char)
                    if (!inQuotes) {
                        result.append("\n")
                        repeat(level) { result.append(indentStr) }
                    }
                }

                ':' -> {
                    result.append(char)
                    if (!inQuotes) result.append(" ")
                }

                else -> {
                    // Suppress excessive raw layout line returns from unformatted strings
                    if (!inQuotes && (char == '\n' || char == '\r' || char == '\t' || char == ' ')) {
                        continue
                    }
                    result.append(char)
                }
            }
        }
        return result.toString()
    }
}
