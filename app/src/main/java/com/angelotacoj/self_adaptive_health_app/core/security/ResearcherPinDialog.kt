package com.angelotacoj.self_adaptive_health_app.core.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton

@Composable
fun ResearcherPinDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        containerColor = Color.White,
        onDismissRequest = onCancel,
        title = { Text("PIN del investigador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ingrese el PIN de 6 dígitos para confirmar esta acción.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(6)
                        error = false
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                if (error) {
                    Text("PIN incorrecto.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            LargePrimaryButton("Confirmar", {
                if (ResearcherSecurity.isValidResearcherPin(pin)) {
                    onConfirm()
                } else {
                    error = true
                }
            })
        },
        dismissButton = {
            LargeSecondaryButton("Cancelar", onCancel)
        }
    )
}
