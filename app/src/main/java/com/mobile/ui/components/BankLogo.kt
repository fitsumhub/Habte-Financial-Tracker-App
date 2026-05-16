package com.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun BankLogo(
    shortName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    fontSize: TextUnit = 12.sp,
    resId: Int? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (resId != null) Color.White else Color(0x2EFFFFFF))
            .border(1.dp, Color(0x40FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (resId != null) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = shortName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (shortName == "DAS") 6.dp else 0.dp), // add some padding to the dashen star logo so it isn't completely flush with edges
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = shortName,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}
