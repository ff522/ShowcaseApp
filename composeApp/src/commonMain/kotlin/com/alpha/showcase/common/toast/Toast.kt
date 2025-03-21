package com.alpha.showcase.common.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Toast(toastMessage: ToastMessage, modifier: Modifier = Modifier) {
    val backgroundColor = when (toastMessage.type) {
        ToastType.SUCCESS -> Color(0xAA4CAF50)  // 绿色
        ToastType.FAILED -> Color(0xAAFF9800)   // 橙色
        ToastType.ERROR -> Color(0xAAF44336)    // 红色
        ToastType.INFO -> Color(0xAA2196F3)     // 蓝色
    }

    Card(
        modifier = modifier
            .padding(16.dp, 8.dp)
            .wrapContentSize(),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = toastMessage.message,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "来源: ${toastMessage.source}",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val toastManager = LocalToastManager.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toastManager.currentToast != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            toastManager.currentToast?.let { Toast(it) }
        }
    }
}