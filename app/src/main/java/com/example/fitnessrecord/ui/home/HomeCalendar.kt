package com.example.fitnessrecord.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

private const val CALENDAR_ANIMATION_DURATION_MS = 200
private const val CALENDAR_CELL_SCALE_DURATION_MS = 160

@Composable
fun HomeCalendar(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    recordDates: Set<LocalDate>,
    onModeChange: (CalendarMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    var pageDirection by remember { mutableStateOf(CalendarPageDirection.Neutral) }

    val goPrevious = {
        pageDirection = CalendarPageDirection.Previous
        onPrevious()
    }
    val goNext = {
        pageDirection = CalendarPageDirection.Next
        onNext()
    }
    val goToday = {
        pageDirection = CalendarPageDirection.Neutral
        onToday()
    }
    val changeMode: (CalendarMode) -> Unit = { calendarMode ->
        pageDirection = CalendarPageDirection.Neutral
        onModeChange(calendarMode)
    }
    val selectDate: (LocalDate) -> Unit = { date ->
        pageDirection = CalendarPageDirection.Neutral
        onDateClick(date)
    }

    Card(
        modifier = Modifier.calendarSwipe(
            thresholdPx = swipeThresholdPx,
            onPrevious = goPrevious,
            onNext = goNext
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalendarToolbar(
                mode = mode,
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                onModeChange = changeMode,
                onPrevious = goPrevious,
                onNext = goNext,
                onToday = goToday
            )

            WeekHeader()

            AnimatedCalendarContent(
                mode = mode,
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                recordDates = recordDates,
                pageDirection = pageDirection,
                onDateClick = selectDate
            )
        }
    }
}

@Composable
private fun CalendarToolbar(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onModeChange: (CalendarMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val isTodaySelected = selectedDate == today
    val todayButtonColor by animateColorAsState(
        targetValue = if (isTodaySelected) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(CALENDAR_ANIMATION_DURATION_MS),
        label = "Today button color"
    )
    val title = remember(mode, visibleMonth, selectedDate) {
        if (mode == CalendarMode.Month) visibleMonth.format(monthFormatter) else weekTitle(selectedDate)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalendarMode.entries.forEach { calendarMode ->
                FilterChip(
                    selected = mode == calendarMode,
                    onClick = { onModeChange(calendarMode) },
                    label = { Text(calendarMode.label) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一页")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "下一页")
            }
            TextButton(
                onClick = onToday,
                enabled = !isTodaySelected,
                modifier = Modifier.semantics { contentDescription = "回到今天" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = todayButtonColor,
                    disabledContentColor = todayButtonColor
                )
            ) {
                Text("今天")
            }
        }
    }
}

@Composable
private fun WeekHeader() {
    val labels = remember { listOf("一", "二", "三", "四", "五", "六", "日") }
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnimatedCalendarContent(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    recordDates: Set<LocalDate>,
    pageDirection: CalendarPageDirection,
    onDateClick: (LocalDate) -> Unit,
) {
    AnimatedContent(
        targetState = CalendarContentKey(
            mode = mode,
            selectedDate = selectedDate,
            visibleMonth = visibleMonth
        ),
        transitionSpec = {
            val animationSpec = tween<IntOffset>(CALENDAR_ANIMATION_DURATION_MS)
            val fadeSpec = tween<Float>(CALENDAR_ANIMATION_DURATION_MS)
            when (pageDirection) {
                CalendarPageDirection.Next -> {
                    (slideInHorizontally(animationSpec) { 48 } + fadeIn(fadeSpec))
                        .togetherWith(slideOutHorizontally(animationSpec) { -48 } + fadeOut(fadeSpec))
                }
                CalendarPageDirection.Previous -> {
                    (slideInHorizontally(animationSpec) { -48 } + fadeIn(fadeSpec))
                        .togetherWith(slideOutHorizontally(animationSpec) { 48 } + fadeOut(fadeSpec))
                }
                CalendarPageDirection.Neutral -> {
                    fadeIn(fadeSpec).togetherWith(fadeOut(fadeSpec))
                }
            }
        },
        label = "Calendar content"
    ) { contentKey ->
        if (contentKey.mode == CalendarMode.Month) {
            MonthGrid(
                visibleMonth = contentKey.visibleMonth,
                selectedDate = contentKey.selectedDate,
                today = today,
                recordDates = recordDates,
                onDateClick = onDateClick
            )
        } else {
            WeekRow(
                selectedDate = contentKey.selectedDate,
                today = today,
                recordDates = recordDates,
                onDateClick = onDateClick
            )
        }
    }
}

@Composable
private fun MonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    recordDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
) {
    val rows = remember(visibleMonth) { visibleMonth.calendarRows() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date == null) {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        } else {
                            CalendarDayCell(
                                date = date,
                                selected = date == selectedDate,
                                today = today,
                                hasRecord = date in recordDates,
                                onClick = { onDateClick(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekRow(
    selectedDate: LocalDate,
    today: LocalDate,
    recordDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
) {
    val weekDates = remember(selectedDate) {
        val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        List(7) { offset -> start.plusDays(offset.toLong()) }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weekDates.forEach { date ->
            Box(modifier = Modifier.weight(1f)) {
                CalendarDayCell(
                    date = date,
                    selected = date == selectedDate,
                    today = today,
                    hasRecord = date in recordDates,
                    onClick = { onDateClick(date) }
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    selected: Boolean,
    today: LocalDate,
    hasRecord: Boolean,
    onClick: () -> Unit,
) {
    val targetBackground = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val targetContentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        date == today -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(CALENDAR_ANIMATION_DURATION_MS),
        label = "Calendar day background"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(CALENDAR_ANIMATION_DURATION_MS),
        label = "Calendar day content color"
    )
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = tween(CALENDAR_CELL_SCALE_DURATION_MS),
        label = "Calendar selected day scale"
    )
    val recordDotAlpha by animateFloatAsState(
        targetValue = if (hasRecord) 1f else 0f,
        animationSpec = tween(CALENDAR_ANIMATION_DURATION_MS),
        label = "Calendar record dot alpha"
    )

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
            }
            .background(background)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(5.dp)
                .clip(CircleShape)
                .alpha(recordDotAlpha)
                .background(contentColor)
        )
    }
}

private fun YearMonth.calendarRows(): List<List<LocalDate?>> {
    val firstDay = atDay(1)
    val leadingBlankCount = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val dates = List(leadingBlankCount) { null } + (1..lengthOfMonth()).map { atDay(it) }
    return dates.chunked(7).map { row -> row + List(7 - row.size) { null } }
}

private fun Modifier.calendarSwipe(
    thresholdPx: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Modifier = pointerInput(thresholdPx, onPrevious, onNext) {
    var totalX = 0f
    var totalY = 0f
    detectDragGestures(
        onDragStart = {
            totalX = 0f
            totalY = 0f
        },
        onDragEnd = {
            val isHorizontalSwipe = abs(totalX) > thresholdPx && abs(totalX) > abs(totalY) * 1.5f
            if (isHorizontalSwipe) {
                if (totalX < 0f) onNext() else onPrevious()
            }
            totalX = 0f
            totalY = 0f
        },
        onDragCancel = {
            totalX = 0f
            totalY = 0f
        },
        onDrag = { _, dragAmount ->
            totalX += dragAmount.x
            totalY += dragAmount.y
        }
    )
}

private enum class CalendarPageDirection {
    Previous,
    Next,
    Neutral,
}

private data class CalendarContentKey(
    val mode: CalendarMode,
    val selectedDate: LocalDate,
    val visibleMonth: YearMonth,
)

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

private fun weekTitle(anchor: LocalDate): String {
    val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = start.plusDays(6)
    return "${start.format(DateTimeFormatter.ofPattern("M/d"))} - ${end.format(DateTimeFormatter.ofPattern("M/d"))}"
}
