package com.example.obitrust.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.obitrust.data.model.BusinessProfileEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.obitrust.ui.components.BusinessLogo
import com.example.obitrust.ui.components.LogoPresets
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.businessProfile.collectAsState()
    val context = LocalContext.current

    var businessName by remember(profile) { mutableStateOf(profile?.businessName ?: "OBITRUST VENTURES GHANA") }
    var tagline by remember(profile) { mutableStateOf(profile?.tagline ?: "Wholesale & Retail Enterprise") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "+233 24 456 7890") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "sales@obitrust.com.gh") }
    var address by remember(profile) { mutableStateOf(profile?.address ?: "Plot 18, Commercial Street, Accra Central") }
    var tinNumber by remember(profile) { mutableStateOf(profile?.tinNumber ?: "C003498124X") }
    var currencySymbol by remember(profile) { mutableStateOf(profile?.currencySymbol ?: "GH₵") }
    var taxRateText by remember(profile) { mutableStateOf(profile?.customTaxRate?.toString() ?: "4.0") }
    var selectedTaxScheme by remember(profile) { mutableStateOf(profile?.taxScheme ?: "VAT_FLAT_RATE") }
    var selectedRole by remember(profile) { mutableStateOf(profile?.activeUserRole ?: "Admin") }
    var pinRequired by remember(profile) { mutableStateOf(profile?.isPinRequired ?: false) }
    var pinCode by remember(profile) { mutableStateOf(profile?.pinCode ?: "1234") }

    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val roles = listOf("Admin", "Manager", "Cashier", "Accountant", "Inventory Officer")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateBusinessLogoFromUri(context, uri) {
                    Toast.makeText(context, "Logo updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings & Business Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Manage your logo branding, Ghana tax rates, security PIN, and store profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Business Brand Logo Customization Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth().testTag("brand_logo_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BusinessLogo(
                            logoUri = profile?.logoUri,
                            selectedPreset = profile?.selectedLogoPreset ?: "SHIELD",
                            size = 64.dp,
                            modifier = Modifier.testTag("current_business_logo_preview")
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Business Logo & Branding",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            val hasCustom = !profile?.logoUri.isNullOrBlank() && java.io.File(profile?.logoUri ?: "").exists()
                            if (hasCustom) {
                                Badge(
                                    containerColor = EmeraldProfit.copy(alpha = 0.2f),
                                    contentColor = EmeraldProfit,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Custom Photo Active", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    "Emblem: ${profile?.selectedLogoPreset ?: "SHIELD"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f).testTag("upload_logo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NavyDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Your Logo", color = NavyDark, fontWeight = FontWeight.Bold)
                        }

                        if (!profile?.logoUri.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.removeCustomLogo(context) {
                                        Toast.makeText(context, "Custom logo removed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("remove_logo_button")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CrimsonExpense, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove", color = CrimsonExpense)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Or Choose an Executive Brand Emblem Preset:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(LogoPresets.presets) { (presetKey, pair) ->
                            val isSelected = (profile?.selectedLogoPreset ?: "SHIELD").equals(presetKey, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateLogoPreset(presetKey) {
                                        Toast.makeText(context, "Brand emblem set to ${pair.first}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = pair.second,
                                        contentDescription = null,
                                        tint = if (isSelected) NavyDark else GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(pair.first, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = NavyDark
                                ),
                                modifier = Modifier.testTag("preset_logo_$presetKey")
                            )
                        }
                    }
                }
            }
        }

        // Active Role Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth().testTag("active_role_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Operating Role", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedRole, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                        Icon(Icons.Default.Security, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Switch Role for Demonstration / Testing:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roles.forEach { r ->
                            FilterChip(
                                selected = selectedRole == r,
                                onClick = {
                                    selectedRole = r
                                    profile?.let { viewModel.saveProfile(it.copy(activeUserRole = r)) {} }
                                },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Business Profile Fields
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Business Information & Receipt Header", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Trading Name") },
                        modifier = Modifier.fillMaxWidth().testTag("business_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("Business Tagline / Subtitle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Official Phone / MoMo (+233)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Business Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Physical Store / Warehouse Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tinNumber,
                        onValueChange = { tinNumber = it },
                        label = { Text("GRA Tax Identification Number (TIN)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Ghana Tax Configuration
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ghana Tax Settings (GRA Compliance)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Select your business tax category to auto-compute VAT/levies on sales", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    listOf(
                        "VAT_FLAT_RATE" to "Ghana VAT Flat Rate Scheme (4.0% - 3% VAT + 1% COVID)",
                        "VAT_STANDARD" to "Standard VAT Scheme (~21.9% - 15% VAT + NHIL + GETFund + COVID)",
                        "NONE" to "Non-Taxable / Exempt (0%)",
                        "CUSTOM" to "Custom Tax Rate"
                    ).forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedTaxScheme = code
                                taxRateText = when (code) {
                                    "VAT_FLAT_RATE" -> "4.0"
                                    "VAT_STANDARD" -> "21.9"
                                    "NONE" -> "0.0"
                                    else -> taxRateText
                                }
                            }
                        ) {
                            RadioButton(
                                selected = selectedTaxScheme == code,
                                onClick = {
                                    selectedTaxScheme = code
                                    taxRateText = when (code) {
                                        "VAT_FLAT_RATE" -> "4.0"
                                        "VAT_STANDARD" -> "21.9"
                                        "NONE" -> "0.0"
                                        else -> taxRateText
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedTextField(
                        value = taxRateText,
                        onValueChange = { taxRateText = it },
                        label = { Text("Active Applied Tax Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Security & App Lock
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Security & PIN Protection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require 4-digit PIN on App Open", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = pinRequired,
                            onCheckedChange = { pinRequired = it }
                        )
                    }

                    if (pinRequired) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current PIN: ••••", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { showPinChangeDialog = true }) {
                                Text("Change PIN", color = GoldPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Save Profile Button
        item {
            Button(
                onClick = {
                    val rate = taxRateText.toDoubleOrNull() ?: 4.0
                    val updated = BusinessProfileEntity(
                        id = 1,
                        businessName = businessName.trim(),
                        tagline = tagline.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        tinNumber = tinNumber.trim(),
                        currencySymbol = currencySymbol.trim(),
                        taxScheme = selectedTaxScheme,
                        customTaxRate = rate,
                        receiptFooter = profile?.receiptFooter ?: "Thank you for shopping with OBITRUST.",
                        pinCode = pinCode,
                        isPinRequired = pinRequired,
                        activeUserRole = selectedRole,
                        logoUri = profile?.logoUri ?: "",
                        selectedLogoPreset = profile?.selectedLogoPreset ?: "SHIELD"
                    )
                    viewModel.saveProfile(updated) {
                        Toast.makeText(context, "Business settings updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
            ) {
                Text("Save Settings", color = NavyDark, fontWeight = FontWeight.Bold)
            }
        }

        // Reset / Reseed Demo Data
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Demo Data & System Re-initialization", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Reload realistic Ghanaian business demo records (products, sales, purchases, expenses and accounts)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedButton(
                        onClick = {
                            viewModel.reseedDemoData {
                                Toast.makeText(context, "Demo business data re-initialized!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("reseed_demo_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reload Ghana Business Demo Data")
                    }
                }
            }
        }
    }

    // Change PIN Dialog
    if (showPinChangeDialog) {
        var newPin by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showPinChangeDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Change 4-Digit Security PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4) newPin = it },
                        label = { Text("Enter 4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showPinChangeDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (newPin.length == 4) {
                                    pinCode = newPin
                                    showPinChangeDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set PIN", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
