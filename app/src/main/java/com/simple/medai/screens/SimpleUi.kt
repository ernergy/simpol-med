package com.simple.medai.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SimpleCardShape = RoundedCornerShape(22.dp)
val SimpleButtonShape = RoundedCornerShape(16.dp)

@Composable
fun SimpleHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SimpleSectionTitle(title: String, subtitle: String? = null) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    if (!subtitle.isNullOrBlank()) {
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SimplePrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = SimpleButtonShape
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SimpleOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = SimpleButtonShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SimpleInfoCard(
    title: String,
    body: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SimpleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                SimplePrimaryButton(actionText, onAction)
            }
        }
    }
}
