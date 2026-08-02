package com.mobile.ui.components

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import com.mobile.data.Bank


import com.mobile.data.Data


@Composable
fun BankCard(
    bank: Bank,
    onPress: (Bank) -> Unit,
    modifier: Modifier = Modifier
) {
    val autoHide by com.mobile.data.SettingsRepository.autoHideBalances.collectAsState()
    var hidden by remember(autoHide) { mutableStateOf(autoHide) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val total = remember(bank) { Data.getBankTotal(bank) }
    
    // Pressed state animation
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(

        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .scale(pressedScale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x1A000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(parseColor(bank.colorFrom)), Color(parseColor(bank.colorTo)))
                )
            )
            .clickable(interactionSource = interactionSource, indication = null) { onPress(bank) }
            .padding(16.dp)


    ) {
        Column {
            BankLogo(shortName = bank.logoText, size = 42.dp, fontSize = 11.sp, resId = bank.logoResId, domain = bank.domain)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = bank.shortName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )

            Text(
                text = "${bank.accounts.size} accounts",
                color = Color(0x8CFFFFFF),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 1.dp, bottom = 10.dp)
            )

            // Divider with animated opacity
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1AFFFFFF))
                    .alpha(if (isPressed) 0.8f else 1.0f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Balance row with animated reveal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(200)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = if (hidden) "•••••" else "${Data.formatBalance(total, true)} ETB",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33000000))
                        .clickable { hidden = !hidden }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hidden) "Show" else "Hide",
                        tint = Color(0xA6FFFFFF),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
