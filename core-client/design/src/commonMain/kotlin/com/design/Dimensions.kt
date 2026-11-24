package com.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimensions {
    @Composable
    fun iconSize(scale: Int = 1): Dp {
        return (18 + (scale - 1) * 2).dp
    }

    @Composable
    fun fontSize(baseSize: Int, scale: Int = 1): TextUnit {
        val factor = when (scale) {
            2 -> 1.2f
            3 -> 1.4f
            else -> 1.0f
        }
        return (baseSize * factor).sp
    }
}
