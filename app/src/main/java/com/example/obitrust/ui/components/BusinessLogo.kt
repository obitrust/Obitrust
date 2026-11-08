package com.example.obitrust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import java.io.File

object LogoPresets {
    val presets = listOf(
        "SHIELD" to ("Prestige Shield" to Icons.Default.Shield),
        "FINANCE" to ("Finance Ledger" to Icons.Default.AccountBalance),
        "STORE" to ("Retail Store" to Icons.Default.Storefront),
        "CART" to ("Commerce Cart" to Icons.Default.ShoppingCart),
        "DIAMOND" to ("Luxury Diamond" to Icons.Default.Diamond),
        "CROWN" to ("Executive Crown" to Icons.Default.WorkspacePremium),
        "AGRO" to ("Agro & Farm" to Icons.Default.Eco),
        "PHARMACY" to ("Health & Pharmacy" to Icons.Default.LocalHospital),
        "TECH" to ("Tech & Electronics" to Icons.Default.Devices)
    )

    fun getIconForPreset(preset: String): ImageVector {
        return presets.firstOrNull { it.first.equals(preset, ignoreCase = true) }?.second?.second
            ?: Icons.Default.Shield
    }
}

@Composable
fun BusinessLogo(
    logoUri: String?,
    selectedPreset: String? = "SHIELD",
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    containerColor: Color = GoldPrimary.copy(alpha = 0.2f),
    iconTint: Color = GoldPrimary,
    borderColor: Color = GoldPrimary.copy(alpha = 0.4f),
    showBorder: Boolean = true,
    borderWidth: Dp = 1.dp,
    modifier: Modifier = Modifier
) {
    val hasValidCustomLogo = !logoUri.isNullOrBlank() && File(logoUri).exists()

    val borderModifier = if (showBorder) {
        Modifier.border(borderWidth, borderColor, shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .then(borderModifier),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidCustomLogo) {
            AsyncImage(
                model = File(logoUri!!),
                contentDescription = "Business Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
            )
        } else {
            val icon = LogoPresets.getIconForPreset(selectedPreset ?: "SHIELD")
            Icon(
                imageVector = icon,
                contentDescription = "Business Emblem",
                tint = iconTint,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}
