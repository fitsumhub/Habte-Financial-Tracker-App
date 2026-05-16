package com.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.mobile.data.Data
import com.mobile.data.Bank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankModal(
    visible: Boolean,
    onClose: () -> Unit,
    onAdd: (bankName: String, accountNumber: String) -> Unit
) {

    if (!visible) return

    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var selectedBankId by remember { mutableStateOf<String?>(null) }
    
    // Validation
    val isAccountNumberValid = accountNumber.length >= 10 && accountNumber.all { it.isDigit() }
    val isFormValid = bankName.isNotBlank() && isAccountNumberValid


    val haptic = LocalHapticFeedback.current


    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0A0F20),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 48.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1A2240))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Add Bank Account",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Select Bank",
                color = Color(0xFF7B84A8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Bank Selection Carousel
            LazyRow(
                modifier = Modifier.padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(Data.PRESET_BANKS) { preset ->
                    val isSelected = selectedBankId == preset.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                            .border(
                                1.dp, 
                                if (isSelected) Color(0xFF6366F1) else Color(0xFF1A2240), 
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedBankId = preset.id
                                bankName = preset.name
                            }
                            .padding(vertical = 10.dp)

                    ) {
                        BankLogoSmall(preset.logoText, isSelected, preset.logoResId)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preset.shortName,
                            color = if (isSelected) Color.White else Color(0xFF7B84A8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Fields
            BankFormField(
                label = "Bank Name (Manual or Selected)",
                value = bankName,
                onValueChange = { 
                    bankName = it
                    selectedBankId = null // clear selection if manually edited
                },
                placeholder = "e.g. Awash Bank",
                keyboardType = KeyboardType.Text
            )
            BankFormField(
                label = "Account Number",
                value = accountNumber,
                onValueChange = { 
                    val filtered = it.filter { char -> char.isDigit() }
                    if (filtered.length <= 16) accountNumber = filtered 
                },
                placeholder = "Enter account number",
                keyboardType = KeyboardType.Number,
                isError = accountNumber.isNotEmpty() && !isAccountNumberValid,
                errorText = "Enter a valid 10-16 digit account number"
            )


            // Add button

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.98f else 1.0f,
                animationSpec = tween(100)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 12.dp)
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isFormValid) {
                            Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        }
                    )

                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (isFormValid) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAdd(bankName, accountNumber)
                                bankName = ""; accountNumber = ""; selectedBankId = null
                                onClose()
                            }
                        }

                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add Account", 
                    color = if (isFormValid) Color.White else Color(0xFF475569), 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold
                )

            }


            // Cancel button
            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Color(0xFF7B84A8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BankLogoSmall(text: String, isSelected: Boolean, logoResId: Int?) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (logoResId != null) Color.White else if (isSelected) Color(0xFF6366F1) else Color(0xFF1A2240)),
        contentAlignment = Alignment.Center
    ) {
        if (logoResId != null) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = text,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (text == "DAS") 4.dp else 0.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isError: Boolean = false,
    errorText: String = ""
) {

    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = label,
            color = Color(0xFF7B84A8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF3A4268)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0E1527),
                unfocusedContainerColor = Color(0xFF0E1527),
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color(0xFF1A2240),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF6366F1),
                errorBorderColor = Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(14.dp),
            isError = isError,
            supportingText = if (isError) {
                { Text(errorText, color = Color(0xFFEF4444), fontSize = 11.sp) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

    }
}
