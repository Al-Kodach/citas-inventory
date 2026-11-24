package com.citacomsys.shared

fun <E : Enum<E>> Enum<E>.displayName() = name.lowercase().split("_")
    .joinToString(" ") {
        it.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }

