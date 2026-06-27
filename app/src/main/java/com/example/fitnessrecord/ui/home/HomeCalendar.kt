package com.example.fitnessrecord.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun HomeCalendar(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    recordDates: Set<LocalDate>,
    onModeChange: (CalendarMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        text = if (mode == CalendarMode.Month) visibleMonth.format(monthFormatter) else weekTitle(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onNext) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "下一页")
                    }
                }
            }

            WeekHeader()

            if (mode == CalendarMode.Month) {
                MonthGrid(visibleMonth, selectedDate, recordDates, onDateClick)
            } else {
                WeekRow(selectedDate, recordDates, onDateClick)
            }
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
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
private fun MonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    recordDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
) {
    val firstDay = visibleMonth.atDay(1)
    val leadingBlankCount = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val dates = List(leadingBlankCount) { null } + (1..visibleMonth.lengthOfMonth()).map { visibleMonth.atDay(it) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        dates.chunked(7).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (row + List(7 - row.size) { null }).forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date == null) {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        } else {
                            CalendarDayCell(date, date == selectedDate, date in recordDates) { onDateClick(date) }
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
    recordDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
) {
    val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..6).forEach { offset ->
            val date = start.plusDays(offset.toLong())
            Box(modifier = Modifier.weight(1f)) {
                CalendarDayCell(date, date == selectedDate, date in recordDates) { onDateClick(date) }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    selected: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        date == LocalDate.now() -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
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
                .background(if (hasRecord) contentColor else Color.Transparent)
        )
    }
}

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.CHINA)

private fun weekTitle(anchor: LocalDate): String {
    val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = start.plusDays(6)
    return "${start.format(DateTimeFormatter.ofPattern("M/d"))} - ${end.format(DateTimeFormatter.ofPattern("M/d"))}"
}
