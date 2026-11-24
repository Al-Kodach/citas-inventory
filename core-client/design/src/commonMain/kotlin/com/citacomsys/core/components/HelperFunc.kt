package com.citacomsys.core.components

fun formatViews(views: Long): String {
    return when {
        views >= 1_000_000 -> "${views / 1_000_000}M"
        views >= 1_000 -> "${views / 1_000}K"
        else -> views.toString()
    }
}