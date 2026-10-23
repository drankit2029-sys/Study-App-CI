package com.studyapp.decaycharge.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.studyapp.decaycharge.MainActivity
import com.studyapp.decaycharge.data.repository.StudyContent
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.di.WidgetEntryPoint
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val chapterIdKey = ActionParameters.Key<Long>(MainActivity.EXTRA_AUTO_LAUNCH_CHAPTER_ID)

class RedZoneGlanceWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val content = withTimeoutOrNull(2_500) {
            entryPoint.triageRepository().content.first { it.subjects.isNotEmpty() }
        } ?: StudyContent()
        provideContent { WidgetBody(content) }
    }
}

@Composable
@GlanceComposable
private fun WidgetBody(content: StudyContent) {
    val size = LocalSize.current
    val compact = size.width < 220.dp
    val active = content.chapters
        .filter { it.status == ChapterStatus.ACTIVE }
        .sortedBy { it.battery }
    val readiness = active.takeIf { it.isNotEmpty() }?.map { it.battery }?.average()?.toInt() ?: 0
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFFDFBF6)))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "DECAY",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF0F5132)),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Box(modifier = GlanceModifier.width(6.dp)) {}
            Text(
                text = "$readiness% held",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF1F2B23)),
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Box(modifier = GlanceModifier.height(7.dp)) {}
        if (active.isEmpty()) {
            Text(
                text = "Open DecayCharge to import a syllabus.",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF7E8378)),
                    fontSize = 11.sp,
                ),
            )
        } else {
            active.take(if (compact) 2 else 3).forEach { chapter ->
                WidgetChapterRow(chapter = chapter, compact = compact, totalWidth = size.width)
                Box(modifier = GlanceModifier.height(5.dp)) {}
            }
        }
    }
}

@Composable
@GlanceComposable
private fun WidgetChapterRow(chapter: Chapter, compact: Boolean, totalWidth: Dp) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(chapterIdKey to chapter.id),
                ),
            )
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(ColorProvider(zoneColor(chapter))),
            ) {}
            Box(modifier = GlanceModifier.width(6.dp)) {}
            Text(
                text = chapter.title,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(Color(0xFF1F2B23)),
                    fontSize = if (compact) 10.sp else 11.sp,
                ),
            )
            Text(
                text = "${chapter.battery.toInt()}%",
                style = TextStyle(
                    color = ColorProvider(zoneColor(chapter)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Box(modifier = GlanceModifier.height(3.dp)) {}
        val fillWidth = (totalWidth - 24.dp) * (chapter.battery / 100f).coerceIn(0.02f, 1f)
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(3.dp)
                .background(ColorProvider(Color(0xFFE9E5DA))),
        ) {
            Box(
                modifier = GlanceModifier
                    .width(fillWidth)
                    .height(3.dp)
                    .background(ColorProvider(zoneColor(chapter))),
            ) {}
        }
    }
}

private fun zoneColor(chapter: Chapter): Color = when {
    chapter.battery < 35f -> Color(0xFFB2574A)
    chapter.battery < 70f -> Color(0xFFBD8848)
    else -> Color(0xFF4E7A5A)
}

class RedZoneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RedZoneGlanceWidget()
}

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun refresh() {
        runCatching { RedZoneGlanceWidget().updateAll(context) }
    }
}
