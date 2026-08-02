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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val RemindersAccent = Color(0xFF6366F1)

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

// Keeps only digits and, when allowed, a single decimal point — kept as its own private
// copy rather than reusing ToolsScreen's version, since private top-level declarations
// aren't visible across files even within the same package.
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
            .background(MaterialTheme.colorScheme.background)
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Payment Reminders",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openEditor(null)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add reminder", tint = RemindersAccent)
                }
            }
            Text(
                text = "Bills, rent, loans and subscriptions — never miss a due date again.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            containerColor = RemindersAccent,
            contentColor = Color.White,
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
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(RemindersAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = RemindersAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "No reminders yet",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Add a reminder for rent, a loan, a subscription or any recurring bill and Habte will notify you before it's due.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = RemindersAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Reminder", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: PaymentReminder, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val option = reminderCategoryOption(reminder.category)
    val dueDateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val isPaid = reminder.lastPaidCycle == cycleKeyFor(reminder.dueDateMillis)
    val daysLeft = daysUntil(reminder.dueDateMillis)

    val (statusText, statusColor) = when {
        !reminder.enabled -> "Off" to MaterialTheme.colorScheme.onSurfaceVariant
        isPaid -> "Paid" to Color(0xFF059669)
        daysLeft < 0 -> "Overdue" to Color(0xFFDC2626)
        daysLeft == 0 -> "Due today" to Color(0xFFDC2626)
        daysLeft <= reminder.daysBefore -> "$daysLeft day${if (daysLeft == 1) "" else "s"} left" to Color(0xFFF59E0B)
        else -> "$daysLeft days left" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(option.color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(option.icon, contentDescription = null, tint = option.color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = if (reminder.payee.isNotBlank()) "${reminder.category} • ${reminder.payee}" else reminder.category,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    dueDateFormat.format(Date(reminder.dueDateMillis)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                if (reminder.amount > 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "ETB ${Data.formatBalance(reminder.amount)}",
                        color = option.color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = reminder.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = RemindersAccent, checkedTrackColor = RemindersAccent.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun ReminderOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = RemindersAccent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun CategoryChip(option: ReminderCategoryOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) option.color else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            option.icon,
            contentDescription = null,
            tint = if (selected) Color.White else option.color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            option.label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReminderSelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) RemindersAccent else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
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
    val haptic = LocalHapticFeedback.current
    val isNew = reminder == null

    var label by remember { mutableStateOf(reminder?.label ?: "") }
    var category by remember { mutableStateOf(reminder?.category ?: ReminderCategories.first().label) }
    var amountInput by remember {
        mutableStateOf(if ((reminder?.amount ?: 0.0) > 0) reminder!!.amount.toString() else "")
    }
    var payeeInput by remember { mutableStateOf(reminder?.payee ?: "") }
    var repeat by remember { mutableStateOf(reminder?.repeat ?: ReminderRepeat.MONTHLY) }
    var daysBefore by remember { mutableStateOf(reminder?.daysBefore ?: 3) }
    var enabled by remember { mutableStateOf(reminder?.enabled ?: true) }
    var dueDateMillis by remember { mutableStateOf(reminder?.dueDateMillis ?: defaultReminderDueDateMillis()) }

    val dueDateFormat = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }
    val isPaidThisCycle = reminder != null && reminder.lastPaidCycle == cycleKeyFor(dueDateMillis)

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 9, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dueDateMillis = picked.timeInMillis
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = System.currentTimeMillis() - 1_000L
        dialog.show()
    }

    fun buildReminder(): PaymentReminder? {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) return null
        return PaymentReminder(
            id = reminder?.id ?: 0,
            label = trimmedLabel,
            category = category,
            amount = amountInput.toDoubleOrNull() ?: 0.0,
            payee = payeeInput.trim(),
            dueDateMillis = dueDateMillis,
            repeat = repeat,
            daysBefore = daysBefore,
            enabled = enabled,
            lastPaidCycle = reminder?.lastPaidCycle ?: ""
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isNew) "New Reminder" else "Edit Reminder",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!isNew) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDelete(reminder!!)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete reminder", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(
                "Get notified ahead of time, every cycle.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ReminderOutlinedField(
                value = label,
                onValueChange = { label = it },
                placeholder = "What's this for? (e.g. Rent, Netflix)",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Text(
                "Category",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(ReminderCategories) { option ->
                    CategoryChip(option, selected = category == option.label) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        category = option.label
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                ReminderOutlinedField(
                    value = amountInput,
                    onValueChange = { amountInput = sanitizeReminderAmount(it) },
                    placeholder = "Amount (ETB)",
                    modifier = Modifier.weight(1f)
                )
                ReminderOutlinedField(
                    value = payeeInput,
                    onValueChange = { payeeInput = it },
                    placeholder = "Payee (optional)",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "Due date",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { openDatePicker() }
                    .padding(14.dp)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RemindersAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(dueDateFormat.format(Date(dueDateMillis)), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Repeat",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                ReminderRepeatOptions.forEach { (value, text) ->
                    ReminderSelectableChip(text, selected = repeat == value) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        repeat = value
                    }
                }
            }

            Text(
                "Remind me",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                ReminderDaysBeforeOptions.forEach { d ->
                    val text = if (d == 0) "Same day" else "$d day${if (d == 1) "" else "s"} before"
                    ReminderSelectableChip(text, selected = daysBefore == d) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        daysBefore = d
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enabled", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Turn off to pause without losing these details", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        enabled = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = RemindersAccent, checkedTrackColor = RemindersAccent.copy(alpha = 0.5f))
                )
            }

            if (!isNew) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMarkPaid(reminder!!)
                    },
                    enabled = !isPaidThisCycle,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(if (isPaidThisCycle) "Already marked as paid" else "Mark this cycle as paid")
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val built = buildReminder()
                    if (built == null) {
                        Toast.makeText(context, "Give this reminder a name first", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(built)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RemindersAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNew) "Add Reminder" else "Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}
