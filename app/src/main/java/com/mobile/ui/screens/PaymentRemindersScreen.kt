package com.mobile.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.Data
import com.mobile.data.PaymentReminder
import com.mobile.data.PaymentReminderRepository
import com.mobile.data.ReminderRepeat
import com.mobile.data.cycleKeyFor
import com.mobile.ui.theme.LocalEthiopianColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ReminderCategoryOption(val label: String, val icon: ImageVector, val color: Color)

val ReminderCategories = listOf(
    ReminderCategoryOption("Rent", Icons.Default.Home, Color(0xFF8B5CF6)),
    ReminderCategoryOption("Loan", Icons.Default.AccountBalance, Color(0xFFF59E0B)),
    ReminderCategoryOption("Utility", Icons.Default.Bolt, Color(0xFFF97316)),
    ReminderCategoryOption("Subscription", Icons.Default.Subscriptions, Color(0xFFEC4899)),
    ReminderCategoryOption("Insurance", Icons.Default.HealthAndSafety, Color(0xFF14B8A6)),
    ReminderCategoryOption("Internet", Icons.Default.Wifi, Color(0xFF0EA5E9)),
    ReminderCategoryOption("Other", Icons.Default.Payments, Color(0xFF6366F1))
)

fun reminderCategoryOption(label: String): ReminderCategoryOption =
    ReminderCategories.find { it.label == label } ?: ReminderCategories.last()

private val ReminderRepeatOptions = listOf(
    ReminderRepeat.ONE_TIME to "One-time",
    ReminderRepeat.WEEKLY to "Weekly",
    ReminderRepeat.MONTHLY to "Monthly",
    ReminderRepeat.YEARLY to "Yearly"
)

private val ReminderDaysBeforeOptions = listOf(0, 1, 3, 5, 7)

private fun defaultReminderDueDateMillis(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 7)
    cal.set(Calendar.HOUR_OF_DAY, 9)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun daysUntil(dueDateMillis: Long): Int =
    ((dueDateMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()

private fun sanitizeReminderAmount(raw: String): String {
    val sb = StringBuilder()
    var dotSeen = false
    for (c in raw) {
        when {
            c.isDigit() -> sb.append(c)
            c == '.' && !dotSeen -> { sb.append(c); dotSeen = true }
        }
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalEthiopianColors.current
    val reminders by PaymentReminderRepository.reminders.collectAsState()
    var editingReminder by remember { mutableStateOf<PaymentReminder?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun openEditor(reminder: PaymentReminder?) {
        editingReminder = reminder
        showEditor = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Payment Reminders",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openEditor(null)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add reminder", tint = colors.emeraldPrimary)
                }
            }
            Text(
                text = "Bills, rent, loans and subscriptions — never miss a due date again.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)
            )

            if (reminders.isEmpty()) {
                ReminderEmptyState(onAdd = { openEditor(null) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onClick = { openEditor(reminder) },
                            onToggle = { enabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                PaymentReminderRepository.setEnabled(context, reminder, enabled)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(140.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                openEditor(null)
            },
            containerColor = colors.emeraldPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 110.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add reminder")
        }
    }

    if (showEditor) {
        ReminderEditorSheet(
            reminder = editingReminder,
            onDismiss = { showEditor = false },
            onSave = { saved ->
                PaymentReminderRepository.save(context, saved)
                Toast.makeText(context, "Reminder saved", Toast.LENGTH_SHORT).show()
                showEditor = false
            },
            onDelete = { toDelete ->
                PaymentReminderRepository.delete(context, toDelete)
                Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()
                showEditor = false
            },
            onMarkPaid = { paid ->
                PaymentReminderRepository.markPaid(paid)
                Toast.makeText(context, "Marked as paid — this cycle's reminder is muted", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun ReminderEmptyState(onAdd: () -> Unit) {
    val colors = LocalEthiopianColors.current
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = colors.emeraldPrimary, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("No Payment Reminders", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Track bills, rent, subscriptions, and loans. We'll alert you before they're due.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Reminder", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: PaymentReminder,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalEthiopianColors.current
    val daysLeft = daysUntil(reminder.dueDateMillis)
    val isOverdue = daysLeft < 0
    val isDueToday = daysLeft == 0
    val option = reminderCategoryOption(reminder.category)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dueDateStr = dateFormat.format(Date(reminder.dueDateMillis))
    val isPaidThisCycle = reminder.lastPaidCycle == cycleKeyFor(reminder.dueDateMillis)

    val cardBg = colors.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(option.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(option.icon, contentDescription = null, tint = option.color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.label,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminder.amount > 0) {
                        Text(
                            text = "ETB ${Data.formatBalance(reminder.amount)}",
                            color = colors.emeraldPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(" · ", color = colors.textMuted)
                    }
                    Text(
                        text = when {
                            isPaidThisCycle -> "Paid this cycle"
                            isOverdue -> "Overdue by ${-daysLeft}d ($dueDateStr)"
                            isDueToday -> "Due today"
                            daysLeft == 1 -> "Due tomorrow"
                            else -> "Due in $daysLeft days ($dueDateStr)"
                        },
                        color = when {
                            isPaidThisCycle -> colors.income
                            isOverdue -> colors.expense
                            isDueToday || daysLeft <= 2 -> colors.warning
                            else -> colors.textSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isOverdue || isDueToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = colors.emeraldPrimary,
                    uncheckedTrackColor = colors.surfaceElevated
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditorSheet(
    reminder: PaymentReminder?,
    onDismiss: () -> Unit,
    onSave: (PaymentReminder) -> Unit,
    onDelete: (PaymentReminder) -> Unit,
    onMarkPaid: (PaymentReminder) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalEthiopianColors.current
    val isEditing = reminder != null

    var label by remember { mutableStateOf(reminder?.label ?: "") }
    var amount by remember { mutableStateOf(reminder?.amount?.takeIf { it > 0 }?.let { Data.formatBalance(it, true) } ?: "") }
    var category by remember { mutableStateOf(reminder?.category ?: "Utility") }
    var dueDateMillis by remember { mutableLongStateOf(reminder?.dueDateMillis ?: defaultReminderDueDateMillis()) }
    var repeat by remember { mutableStateOf(reminder?.repeat ?: ReminderRepeat.MONTHLY) }
    var daysBefore by remember { mutableIntStateOf(reminder?.daysBefore ?: 1) }

    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())

    fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val newCal = Calendar.getInstance().apply {
                    set(y, m, d, 9, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dueDateMillis = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (isEditing) "Edit Payment Reminder" else "New Payment Reminder",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("What is this for?", color = colors.textSecondary) },
                placeholder = { Text("e.g. Electricity, Apartment Rent", color = colors.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedBorderColor = colors.emeraldPrimary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = sanitizeReminderAmount(it) },
                label = { Text("Amount (ETB) · Optional", color = colors.textSecondary) },
                placeholder = { Text("0.00", color = colors.textMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedBorderColor = colors.emeraldPrimary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Category", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReminderCategories) { opt ->
                    val isSel = opt.label == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) colors.emeraldPrimary else colors.surfaceElevated)
                            .border(1.dp, if (isSel) colors.emeraldPrimary else colors.border, RoundedCornerShape(12.dp))
                            .clickable { category = opt.label }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(opt.icon, contentDescription = null, tint = if (isSel) MaterialTheme.colorScheme.onPrimary else opt.color, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(opt.label, color = if (isSel) MaterialTheme.colorScheme.onPrimary else colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Due Date", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable { pickDate() }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.emeraldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(dateFormat.format(Date(dueDateMillis)), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Change", color = colors.emeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (label.isBlank()) {
                        Toast.makeText(context, "Please enter a reminder label", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val item = PaymentReminder(
                        id = reminder?.id ?: 0L,
                        label = label.trim(),
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        category = category,
                        payee = "",
                        dueDateMillis = dueDateMillis,
                        repeat = repeat,
                        daysBefore = daysBefore,
                        enabled = true,
                        lastPaidCycle = reminder?.lastPaidCycle ?: ""
                    )
                    onSave(item)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEditing) "Save Changes" else "Create Reminder", fontWeight = FontWeight.Bold)
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onDelete(reminder) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Reminder", color = colors.expense, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
