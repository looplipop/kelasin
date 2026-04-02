package com.kelasin.app.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelasin.app.data.entity.*
import com.kelasin.app.data.repository.TugasRepository
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalenderScreen(
    userId: String,
    tugasRepo: TugasRepository,
    mkRepo: MataKuliahRepository
) {
    val allTugas by tugasRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    val mkList by mkRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    val darkTheme = LocalThemeMode.current
    
    val baseCal = remember { Calendar.getInstance().apply { set(2025, Calendar.SEPTEMBER, 1, 0, 0, 0) } }
    
    val todayCal = Calendar.getInstance()
    val diffMonth = (todayCal.get(Calendar.YEAR) - 2025) * 12 + (todayCal.get(Calendar.MONTH) - Calendar.SEPTEMBER)
    val initialPage = diffMonth.coerceIn(0, 11)
    
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 12 })
    
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    
    val currentPagedMonth = remember(pagerState.currentPage) {
        (baseCal.clone() as Calendar).apply { add(Calendar.MONTH, pagerState.currentPage) }
    }
    
    val tugasMap = remember(allTugas) { 
        allTugas.filter { it.status != StatusTugas.SELESAI }.groupBy { 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.deadline))
        }
    }
    
    val selectedTugas = remember(selectedDate, allTugas) {
        if (selectedDate != null) {
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
            tugasMap[dateKey] ?: emptyList()
        } else emptyList()
    }
    
    val monthEvents = remember(currentPagedMonth) {
        getEventsForMonth(currentPagedMonth.get(Calendar.YEAR), currentPagedMonth.get(Calendar.MONTH))
    }
    
    Scaffold(
        topBar = {
            val topBarBg = if (darkTheme) KelasinDarkBannerBlue else KelasinPrimary
            Box(
                modifier = Modifier.fillMaxWidth().background(topBarBg)
            ) {
                Column {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    DynamicStatusBar(
                        statusBarColor = topBarBg,
                        useDarkIcons = !darkTheme
                    )
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarMonth, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    SimpleDateFormat("MMMM yyyy", Locale("id")).format(currentPagedMonth.time),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                val pageCal = remember(page) { (baseCal.clone() as Calendar).apply { add(Calendar.MONTH, page) } }
                val pageDays = remember(pageCal) { getDaysInMonth(pageCal) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = KelasinSubtext
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.heightIn(max = 350.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(pageDays) { dayData ->
                                CalendarDayCell(
                                    dayData = dayData,
                                    tugasCount = tugasMap[dayData.dateKey]?.size ?: 0,
                                    isSelected = selectedDate != null && 
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate) == dayData.dateKey,
                                    onClick = {
                                        selectedDate = if (dayData.date != null) dayData.date else null
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Legend & Academic Events OR Selected tasks
            if (selectedDate == null) {
                // Show Academic Events for this month
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Kalender Akademik UTB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KelasinPrimary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(monthEvents) { event ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(event.color))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (event.startDateStr == event.endDateStr) 
                                        SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(event.startMillis))
                                    else 
                                        "${SimpleDateFormat("dd MMM", Locale("id")).format(Date(event.startMillis))} - ${SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(event.endMillis))}",
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = KelasinSubtext
                                )
                            }
                        }
                    }
                    item {
                        if (monthEvents.isEmpty()) {
                            Text("Tidak ada jadwal spesifik akademi bulan ini.", style = MaterialTheme.typography.bodyMedium, color = KelasinSubtext)
                        }
                    }
                }
            } else {
                // Show selected tasks
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedTugas.size} Tugas pada ${SimpleDateFormat("dd MMMM", Locale("id")).format(selectedDate!!)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { selectedDate = null }) {
                                Icon(Icons.Filled.Close, "Tutup", tint = KelasinSubtext)
                            }
                        }
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedTugas) { tugas ->
                                val mk = mkList.find { it.id == tugas.mataKuliahId }
                                TugasCalendarCard(tugas, mk)
                            }
                            if (selectedTugas.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("Tidak ada tugas pada tanggal ini", color = KelasinSubtext)
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

data class DayData(
    val day: Int?,
    val date: Date?,
    val dateKey: String,
    val isToday: Boolean,
    val isCurrentMonth: Boolean,
    val bgAcadColors: List<Color>
)

fun getDaysInMonth(calendar: Calendar): List<DayData> {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    val today = Calendar.getInstance()
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)
    
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val result = mutableListOf<DayData>()
    
    val emptyCellsStart = if (firstDayOfWeek == Calendar.SUNDAY) 0 else firstDayOfWeek - 1
    repeat(emptyCellsStart) {
        result.add(DayData(null, null, "", false, false, emptyList()))
    }
    
    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val acadColors = getColorsForDate(cal.timeInMillis)
        
        result.add(
            DayData(
                day = day,
                date = cal.time,
                dateKey = dateKey,
                isToday = dateKey == todayKey,
                isCurrentMonth = true,
                bgAcadColors = acadColors
            )
        )
    }
    
    while (result.size % 7 != 0) {
        result.add(DayData(null, null, "", false, false, emptyList()))
    }
    
    return result
}

@Composable
fun CalendarDayCell(
    dayData: DayData,
    tugasCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val darkTheme = com.kelasin.app.ui.theme.LocalThemeMode.current
    val hasAcademicEvent = dayData.bgAcadColors.isNotEmpty()
    val hasDualAcademicEvent = dayData.bgAcadColors.size >= 2
    val isAcademicEvent = hasAcademicEvent && !isSelected
    val eventBg = if (hasDualAcademicEvent) {
        null
    } else if (darkTheme && hasAcademicEvent) {
        dayData.bgAcadColors.first().copy(alpha = 0.5f)
    } else {
        dayData.bgAcadColors.firstOrNull()
    }
    
    val bgColor = when {
        isSelected -> KelasinPrimary
        dayData.isToday && !isAcademicEvent -> KelasinPrimaryVar
        eventBg != null -> eventBg
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected || (dayData.isToday && !isAcademicEvent) -> Color.White
        dayData.day == null -> Color.Transparent
        hasAcademicEvent -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = dayData.day != null) { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isSelected && dayData.day != null && hasDualAcademicEvent) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val first = if (darkTheme) dayData.bgAcadColors[0].copy(alpha = 0.5f) else dayData.bgAcadColors[0]
                val second = if (darkTheme) dayData.bgAcadColors[1].copy(alpha = 0.5f) else dayData.bgAcadColors[1]

                // Diagonal split from top-right to bottom-left
                val path1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(0f, size.height)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(path = path1, color = first)
                drawPath(path = path2, color = second)
            }
        }

        if (dayData.day != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayData.day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (dayData.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
                if (tugasCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(minOf(tugasCount, 3)) {
                                    Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected || (dayData.isToday && !isAcademicEvent) || hasAcademicEvent) Color.White else KelasinError)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TugasCalendarCard(tugas: TugasEntity, mk: com.kelasin.app.data.entity.MataKuliahEntity?) {
    val mkColor = if (mk != null) {
        runCatching { Color(android.graphics.Color.parseColor(mk.warna)) }.getOrElse { KelasinPrimary }
    } else {
        KelasinSubtext.copy(alpha = 0.6f)
    }
    
    val priorityColor = when (tugas.prioritas) {
        Prioritas.TINGGI -> KelasinError
        Prioritas.SEDANG -> KelasinWarning
        else -> KelasinSuccess
    }
    
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(mkColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        mk?.nama ?: "Umum",
                        style = MaterialTheme.typography.labelSmall,
                        color = mkColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            tugas.prioritas.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    tugas.judul,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (tugas.deskripsi.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tugas.deskripsi.take(60) + if (tugas.deskripsi.length > 60) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = KelasinSubtext
                    )
                }
            }
            Icon(
                Icons.Filled.Schedule,
                null,
                tint = KelasinSubtext,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                fmt.format(Date(tugas.deadline)),
                style = MaterialTheme.typography.labelSmall,
                color = KelasinSubtext
            )
        }
    }
}
