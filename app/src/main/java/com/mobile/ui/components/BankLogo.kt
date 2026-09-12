package com.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mobile.data.InstitutionCatalog

@Composable
fun BankLogo(
    shortName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    fontSize: TextUnit = 12.sp,
    resId: Int? = null,
    domain: String? = null
) {
    val catalogEntry = remember(shortName) {
        InstitutionCatalog.ALL.find {
            it.shortName.equals(shortName, ignoreCase = true) ||
            it.logoText.equals(shortName, ignoreCase = true) ||
            it.id.equals(shortName, ignoreCase = true) ||
            it.name.equals(shortName, ignoreCase = true) ||
            it.smsContains.any { contains -> contains.equals(shortName, ignoreCase = true) }
        }
    }

    val finalResId = resId ?: catalogEntry?.logoResId
    val finalDomain = domain ?: catalogEntry?.domain

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (finalResId != null || finalDomain != null) Color.White else Color(0x2EFFFFFF))
            .border(1.dp, Color(0x40FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (finalResId != null) {
            Image(
                painter = painterResource(id = finalResId),
                contentDescription = shortName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (shortName.equals("DAS", ignoreCase = true) || shortName.equals("DSB", ignoreCase = true)) 6.dp else 2.dp),
                contentScale = ContentScale.Fit
            )
        } else if (finalDomain != null) {
            AsyncImage(
                model = "https://www.google.com/s2/favicons?domain=${finalDomain}&sz=128",
                contentDescription = shortName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = shortName.take(4),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}
