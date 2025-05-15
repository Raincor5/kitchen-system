package com.fft.kitchen.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fft.kitchen.data.Label
import com.fft.kitchen.viewmodel.PrintLabelViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.fft.kitchen.printer.AidlPrinterHelper

@Composable
fun PrintLabelScreen(
    viewModel: PrintLabelViewModel,
    onPrintLabel: (String) -> Unit,
    onPrintMultipleLabels: (List<String>) -> Unit,
    onCalibratePrinter: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var multipleTexts by remember { mutableStateOf("") }
    val printerStatus by viewModel.printerStatus.collectAsState()
    val printingStatus by viewModel.printingStatus.collectAsState()
    val printResult by viewModel.printResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Printer Status: $printerStatus",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Label Text") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onPrintLabel(text) },
            enabled = printerStatus == AidlPrinterHelper.PrinterStatus.CONNECTED && !printingStatus
        ) {
            Text("Print Label")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = multipleTexts,
            onValueChange = { multipleTexts = it },
            label = { Text("Multiple Labels (one per line)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { 
                val texts = multipleTexts.split("\n").filter { it.isNotBlank() }
                onPrintMultipleLabels(texts)
            },
            enabled = printerStatus == AidlPrinterHelper.PrinterStatus.CONNECTED && !printingStatus
        ) {
            Text("Print Multiple Labels")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCalibratePrinter,
            enabled = printerStatus == AidlPrinterHelper.PrinterStatus.CONNECTED && !printingStatus
        ) {
            Text("Calibrate Printer")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (printResult) {
            is PrintLabelViewModel.PrintResult.Success -> {
                Text(
                    text = "Print successful",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is PrintLabelViewModel.PrintResult.Error -> {
                Text(
                    text = (printResult as PrintLabelViewModel.PrintResult.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            null -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelItem(
    label: Label,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label.dishName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Tray ID: ${label.trayId}")
            Text("Prep Date: ${dateFormat.format(label.prepDate)}")
            Text("Expiry Date: ${dateFormat.format(label.expiryDate)}")
            if (label.ingredients.isNotEmpty()) {
                Text("Ingredients: ${label.ingredients.joinToString(", ")}")
            }
            if (label.allergens.isNotEmpty()) {
                Text("Allergens: ${label.allergens.joinToString(", ")}")
            }
            label.notes?.let { Text("Notes: $it") }
        }
    }
} 