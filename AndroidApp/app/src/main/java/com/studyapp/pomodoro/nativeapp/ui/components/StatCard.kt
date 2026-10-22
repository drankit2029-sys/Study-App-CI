package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyapp.pomodoro.nativeapp.ui.theme.Charcoal
import com.studyapp.pomodoro.nativeapp.ui.theme.DeepTeal
import com.studyapp.pomodoro.nativeapp.ui.theme.MutedGrey
import com.studyapp.pomodoro.nativeapp.ui.theme.RaisedCream
import com.studyapp.pomodoro.nativeapp.ui.theme.TealTint

@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .softRaised(RaisedCream, 16.dp, 5.dp)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(TealTint, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepTeal,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            color = Charcoal,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontFeatureSettings = "tnum",
            ),
        )
        Text(
            text = label,
            color = MutedGrey,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                lineHeight = 12.sp,
            ),
        )
    }
}
