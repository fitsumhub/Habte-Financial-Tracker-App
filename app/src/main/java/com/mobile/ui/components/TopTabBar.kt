package com.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.ui.theme.LocalEthiopianColors

data class Tab(val key: String, val label: String)

@Composable
fun TopTabBar(
    tabs: List<Tab>,
    activeKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEthiopianColors.current
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tabs) { tab ->
            val isActive = tab.key == activeKey

            val textColor by animateColorAsState(
                targetValue = if (isActive) onPrimaryColor else colors.textSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "tabTextColor"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isActive) colors.emeraldPrimary else colors.surface,
                animationSpec = tween(durationMillis = 200),
                label = "tabBgColor"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        color = if (isActive) colors.emeraldPrimary else colors.border,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(tab.key) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
