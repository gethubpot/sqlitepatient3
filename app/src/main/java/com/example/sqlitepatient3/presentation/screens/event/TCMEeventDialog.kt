package com.example.sqlitepatient3.presentation.screens.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sqlitepatient3.presentation.components.DatePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Dialog for TCM events that require a hospital discharge date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TCMEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    initialDate: LocalDate = LocalDate.now().minusDays(7)
) {
    var dischargeDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TCM Event Information") },
        text = {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Transitional Care Management (TCM) events require hospital discharge date information.",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Hospital Discharge Date Field
                OutlinedTextField(
                    value = dischargeDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                    onValueChange = { },
                    label = { Text("Hospital Discharge Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Date"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The event bill date will be set to 32 days after the discharge date.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dischargeDate) }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        val epochMillis = dischargeDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        DatePickerDialog(
            onDateSelected = { millis ->
                val selectedDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                dischargeDate = selectedDate
            },
            onDismiss = { showDatePicker = false },
            initialSelectedDateMillis = epochMillis
        )
    }
}