package com.simple.medai.screens

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
    subtitle: String? = null
) {
    Column {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun SimpleBackButton(
    onClick: () -> Unit,
    label: String = "VOLVER"
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Text(
            "←",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontWeight = FontWeight.Bold
        )
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
    enabled: Boolean = true,
    icon: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = SimpleButtonShape
    ) {
        if (!icon.isNullOrBlank()) {
            Text(icon, fontSize = 19.sp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SimpleOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = SimpleButtonShape
    ) {
        if (!icon.isNullOrBlank()) {
            Text(icon, fontSize = 19.sp)
            Spacer(Modifier.width(8.dp))
        }
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
                SimplePrimaryButton(
                    text = actionText,
                    onClick = onAction
                )
            }
        }
    }
}

@Composable
fun FeatureTile(
    icon: String,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = SimpleCardShape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    icon,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onClick != null) {
                Text("›", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
