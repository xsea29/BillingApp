package com.example.billingapp.ui.customers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.billingapp.Customer
import com.example.billingapp.isValidEmail
import com.example.billingapp.isValidPhone
import com.example.billingapp.otherCustomerWithSameDetails
import com.example.billingapp.ui.components.ConfirmDialog
import com.example.billingapp.ui.components.TopRow

/**
 * Add a customer ([existing] = null) or change one. Name and a 10-digit phone number are
 * required; email is optional. The same name with the same number can't be saved twice, but
 * different people may share a number.
 */
@Composable
fun CustomerForm(
    existing: Customer?,
    customers: List<Customer>,
    onBackClick: () -> Unit,
    onSave: (name: String, phone: String, email: String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Two different people can share a number (a family, the shop's phone); only the same name
    // with the same number is a duplicate.
    val duplicate = if (name.isNotBlank() && isValidPhone(phone)) {
        otherCustomerWithSameDetails(name, phone, customers, ignoreId = existing?.id)
    } else {
        null
    }
    val emailOk = isValidEmail(email)
    val canSave = name.isNotBlank() && isValidPhone(phone) && duplicate == null && emailOk
    val fieldShape = MaterialTheme.shapes.medium

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp)
        ) {

            TopRow(startText = "Back", onStartClick = onBackClick)

            Text(
                text = if (existing == null) "New Customer" else "Edit Customer",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                placeholder = { Text("Sirsendu Roy") },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) phone = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone") },
                placeholder = { Text("9876543210") },
                singleLine = true,
                shape = fieldShape,
                isError = duplicate != null,
                supportingText = {
                    when {
                        duplicate != null -> Text("${duplicate.name} is already saved with this number")
                        else -> Text("10 digits")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email (optional)") },
                placeholder = { Text("sirsendu@example.com") },
                singleLine = true,
                shape = fieldShape,
                isError = !emailOk,
                supportingText = if (emailOk) null else {
                    { Text("Enter a valid email, or leave it empty") }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            if (existing != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = "Delete customer",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Button(
            onClick = { onSave(name.trim(), phone, email.trim()) },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(52.dp)
        ) {
            Text("Save")
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete customer?",
            message = "${existing.name} will be removed from your customer list. " +
                    "Their past invoices stay as they are.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
