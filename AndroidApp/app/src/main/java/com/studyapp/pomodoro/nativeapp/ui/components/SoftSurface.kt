package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studyapp.pomodoro.nativeapp.ui.theme.Paper
import com.studyapp.pomodoro.nativeapp.ui.theme.ShadowWarm
import com.studyapp.pomodoro.nativeapp.ui.theme.White

fun Modifier.softRaised(
    color: Color = Paper,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp,
    highlight: Boolean = true,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    var result = shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ShadowWarm.copy(alpha = 0.45f),
        spotColor = ShadowWarm,
    ).background(color, shape)
    if (highlight) {
        result = result.border(1.dp, White.copy(alpha = 0.72f), shape)
    }
    return result
}
