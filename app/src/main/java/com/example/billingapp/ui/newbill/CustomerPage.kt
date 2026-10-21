package com.example.billingapp.ui.newbill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.billingapp.Customer
import com.example.billingapp.isValidPhone
import com.example.billingapp.ui.components.InitialAvatar
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow

/**
 * Type a name and phone to use for this bill. While typing, saved customers that
 * match are listed underneath: tap one to use it, or press Done to use what was typed.
 */
@Composable
fun CustomerPage(
    customers: List<Customer>,
    initialName: String,
    initialPhone: String,
    onBackClick: () -> Unit,
    onDone: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }

    val matches = remember(customers, name, phone) {
        customers
            .filter {
                it.name.contains(name.trim(), ignoreCase = true) &&
                        it.phone.contains(phone)
            }
            .sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp)
    ) {

        TopRow(startText = "Back", onStartClick = onBackClick)

        Text(
            text = "Customer",
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
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                    phone = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone") },
            placeholder = { Text("9876543210") },
            supportingText = {
                when {
                    phone.isEmpty() -> Text("Optional")
                    !isValidPhone(phone) -> Text("Enter all 10 digits to add them to Customers")
                    customers.any { it.phone == phone } -> Text("Saved customer")
                    name.isBlank() -> Text("Add a name to save this customer")
                    else -> Text("Will be added to Customers")
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        if (matches.isNotEmpty()) {
            SmallCaps(
                text = "Saved customers",
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp)
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(matches, key = { it.id }) { customer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDone(customer.name, customer.phone) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InitialAvatar(name = customer.name)

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onDone(name.trim(), phone) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .height(52.dp)
        ) {
            Text("Done")
        }
    }
}
