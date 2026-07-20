package com.example.fitnessrecord.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private const val CALENDAR_ANIMATION_DURATION_MS = 200
private const val CALENDAR_CELL_SCALE_DURATION_MS = 160
private const val MIN_CALENDAR_YEAR = 1900
private const val MAX_CALENDAR_YEAR = 2100
private val minCalendarDate = LocalDate.of(MIN_CALENDAR_YEAR, 1, 1)
private val maxCalendarDate = LocalDate.of(MAX_CALENDAR_YEAR, 12, 31)
private val baseCalendarMonth = YearMonth.from(minCalendarDate)
private val baseCalendarWeek = minCalendarDate.startOfWeek()
private val monthPageCount = ChronoUnit.MONTHS.between(
    baseCalendarMonth.atDay(1),
    YearMonth.from(maxCalendarDate).atDay(1)
).toInt() + 1
private val weekPageCount = ChronoUnit.WEEKS.between(baseCalendarWeek, maxCalendarDate.startOfWeek()).toInt() + 1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeCalendar(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    visibleWeekStart: LocalDate,
    selectedDate: LocalDate,
    recordDates: Set<LocalDate>,
    onModeChange: (CalendarMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onMonthSettled: (expected: YearMonth, settled: YearMonth) -> Unit,
    onWeekSettled: (expected: LocalDate, settled: LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    var displayedMonth by remember { mutableStateOf(visibleMonth) }
    var displayedWeekStart by remember { mutableStateOf(visibleWeekStart) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalendarToolbar(
                mode = mode,
                visibleMonth = displayedMonth,
                visibleWeekStart = displayedWeekStart,
                selectedDate = selectedDate,
                today = today,
                onModeChange = onModeChange,
                onPrevious = onPrevious,
                onNext = onNext,
                onToday = onToday
            )

            WeekHeader()

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    fadeIn(tween(CALENDAR_ANIMATION_DURATION_MS))
                        .togetherWith(fadeOut(tween(CALENDAR_ANIMATION_DURATION_MS)))
                },
                modifier = Modifier.animateContentSize(tween(CALENDAR_ANIMATION_DURATION_MS)),
                label = "Calendar mode"
            ) { targetMode ->
                if (targetMode == CalendarMode.Month) {
                    MonthPager(
                        targetMonth = visibleMonth,
                        selectedDate = selectedDate,
                        today = today,
                        recordDates = recordDates,
                        onDisplayedMonthChange = { displayedMonth = it },
                        onSettled = onMonthSettled,
                        onDateClick = onDateClick
                    )
                } else {
                    WeekPager(
                        targetWeekStart = visibleWeekStart,
                        selectedDate = selectedDate,
                        today = today,
                        recordDates = recordDates,
                        onDisplayedWeekChange = { displayedWeekStart = it },
                        onSettled = onWeekSettled,
                        onDateClick = onDateClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarToolbar(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    visibleWeekStart: LocalDate,
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
    val title = remember(mode, visibleMonth, visibleWeekStart) {
        if (mode == CalendarMode.Month) visibleMonth.format(monthFormatter) else weekTitle(visibleWeekStart)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthPager(
    targetMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    recordDates: Set<LocalDate>,
    onDisplayedMonthChange: (YearMonth) -> Unit,
    onSettled: (expected: YearMonth, settled: YearMonth) -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    val targetPage = targetMonth.toMonthPage()
    val pagerState = rememberPagerState(initialPage = targetPage, pageCount = { monthPageCount })
    val currentTarget by rememberUpdatedState(targetMonth)
    val currentOnSettled by rememberUpdatedState(onSettled)
    var gestureOrigin by remember { mutableStateOf<YearMonth?>(null) }

    LaunchedEffect(targetPage) {
        if (pagerState.currentPage == targetPage && !pagerState.isScrollInProgress) return@LaunchedEffect
        if (abs(pagerState.currentPage - targetPage) <= 1) {
            pagerState.animateScrollToPage(targetPage)
        } else {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { onDisplayedMonthChange(it.toYearMonth()) }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) {
                    gestureOrigin = currentTarget
                } else {
                    val expected = gestureOrigin
                    gestureOrigin = null
                    if (expected != null) {
                        currentOnSettled(expected, pagerState.settledPage.toYearMonth())
                    }
                }
            }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - 36.dp) / 7
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(cellWidth * 6 + 40.dp)
        ) { page ->
            MonthGrid(
                visibleMonth = page.toYearMonth(),
                selectedDate = selectedDate,
                today = today,
                recordDates = recordDates,
                enabled = !pagerState.isScrollInProgress,
                onDateClick = onDateClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeekPager(
    targetWeekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    recordDates: Set<LocalDate>,
    onDisplayedWeekChange: (LocalDate) -> Unit,
    onSettled: (expected: LocalDate, settled: LocalDate) -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    val normalizedTarget = targetWeekStart.startOfWeek()
    val targetPage = normalizedTarget.toWeekPage()
    val pagerState = rememberPagerState(initialPage = targetPage, pageCount = { weekPageCount })
    val currentTarget by rememberUpdatedState(normalizedTarget)
    val currentOnSettled by rememberUpdatedState(onSettled)
    var gestureOrigin by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(targetPage) {
        if (pagerState.currentPage == targetPage && !pagerState.isScrollInProgress) return@LaunchedEffect
        if (abs(pagerState.currentPage - targetPage) <= 1) {
            pagerState.animateScrollToPage(targetPage)
        } else {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { onDisplayedWeekChange(it.toWeekStart()) }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) {
                    gestureOrigin = currentTarget
                } else {
                    val expected = gestureOrigin
                    gestureOrigin = null
                    if (expected != null) {
                        currentOnSettled(expected, pagerState.settledPage.toWeekStart())
                    }
                }
            }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - 36.dp) / 7
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(cellWidth)
        ) { page ->
            WeekRow(
                weekStart = page.toWeekStart(),
                selectedDate = selectedDate,
                today = today,
                recordDates = recordDates,
                enabled = !pagerState.isScrollInProgress,
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
    enabled: Boolean,
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
                                enabled = enabled,
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
    weekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    recordDates: Set<LocalDate>,
    enabled: Boolean,
    onDateClick: (LocalDate) -> Unit,
) {
    val weekDates = remember(weekStart) {
        List(7) { offset -> weekStart.plusDays(offset.toLong()) }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weekDates.forEach { date ->
            Box(modifier = Modifier.weight(1f)) {
                CalendarDayCell(
                    date = date,
                    selected = date == selectedDate,
                    today = today,
                    hasRecord = date in recordDates,
                    enabled = enabled,
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
    enabled: Boolean,
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
            .clickable(enabled = enabled && date in minCalendarDate..maxCalendarDate, onClick = onClick)
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

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

private fun weekTitle(anchor: LocalDate): String {
    val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = start.plusDays(6)
    return "${start.format(DateTimeFormatter.ofPattern("M/d"))} - ${end.format(DateTimeFormatter.ofPattern("M/d"))}"
}

private fun YearMonth.toMonthPage(): Int =
    ChronoUnit.MONTHS.between(baseCalendarMonth.atDay(1), atDay(1))
        .coerceIn(0L, (monthPageCount - 1).toLong())
        .toInt()

private fun Int.toYearMonth(): YearMonth = baseCalendarMonth.plusMonths(toLong())

private fun LocalDate.toWeekPage(): Int =
    ChronoUnit.WEEKS.between(baseCalendarWeek, startOfWeek())
        .coerceIn(0L, (weekPageCount - 1).toLong())
        .toInt()

private fun Int.toWeekStart(): LocalDate = baseCalendarWeek.plusWeeks(toLong())

private fun LocalDate.startOfWeek(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
