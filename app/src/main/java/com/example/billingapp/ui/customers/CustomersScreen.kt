package com.example.billingapp.ui.customers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.billingapp.AppDatabase
import com.example.billingapp.Customer
import com.example.billingapp.Invoice
import com.example.billingapp.deleteInvoice
import com.example.billingapp.ui.components.ArrowCircle
import com.example.billingapp.ui.components.EmptyState
import com.example.billingapp.ui.components.InitialAvatar
import com.example.billingapp.ui.components.PaginationBar
import com.example.billingapp.ui.components.PillChip
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SearchField
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.util.formatRupeesCompact
import com.example.billingapp.util.Page
import com.example.billingapp.util.paginate
import kotlinx.coroutines.launch

/** What the editor page is showing: [customer] = null means "add a new one". */
private data class Editor(val customer: Customer?)

/**
 * The customer book. Three pages in one: the list, one customer's details, and the
 * add/edit form. [openCustomerId] is kept by the caller so that coming back from a bill
 * lands on the same customer again.
 */
@Composable
fun CustomersScreen(
    db: AppDatabase,
    openCustomerId: Int?,
    onOpenCustomerChange: (Int?) -> Unit,
    onInvoiceClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // null = still loading.
    var customers by remember { mutableStateOf<List<Customer>?>(null) }
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(CustomerSort.Name) }
    var pageNumber by remember { mutableStateOf(1) }
    var editor by remember { mutableStateOf<Editor?>(null) }

    // Kept here, above the page switch, so the list's scroll position survives opening a customer.
    val gridState = rememberLazyGridState()

    suspend fun reload() {
        customers = db.customerDao().getAllCustomers()
        invoices = db.invoiceDao().getAllInvoices()
    }

    LaunchedEffect(Unit) { reload() }

    val all = customers.orEmpty()
    val stats = remember(invoices) { statsByCustomer(invoices) }
    val opened = openCustomerId?.let { id -> all.firstOrNull { it.id == id } }
    val editing = editor

    BackHandler {
        when {
            editing != null -> editor = null
            opened != null -> onOpenCustomerChange(null)
            else -> onBackClick()
        }
    }

    when {
        editing != null -> CustomerForm(
            existing = editing.customer,
            customers = all,
            onBackClick = { editor = null },
            onSave = { name, phone, email ->
                scope.launch {
                    val saved = Customer(
                        id = editing.customer?.id ?: 0,
                        name = name,
                        phone = phone,
                        email = email
                    )

                    if (editing.customer == null) {
                        db.customerDao().insert(saved)
                    } else {
                        db.customerDao().update(saved)
                    }

                    reload()
                    editor = null
                }
            },
            onDelete = {
                scope.launch {
                    editing.customer?.let { db.customerDao().delete(it) }
                    reload()
                    onOpenCustomerChange(null)
                    editor = null
                }
            }
        )

        opened != null -> CustomerDetail(
            customer = opened,
            stats = stats[opened.id] ?: CustomerStats(),
            bills = billsOf(opened, invoices),
            onBackClick = { onOpenCustomerChange(null) },
            onEditClick = { editor = Editor(opened) },
            onInvoiceClick = onInvoiceClick,
            onDeleteBill = { bill ->
                scope.launch {
                    db.deleteInvoice(bill.id)
                    reload()
                }
            }
        )

        else -> {
            val shown = remember(customers, query, sort, stats) {
                sortCustomers(filterCustomers(all, query), stats, sort)
            }

            val page = paginate(shown, pageNumber)

            CustomerList(
                loaded = customers != null,
                total = all.size,
                page = page,
                stats = stats,
                query = query,
                onQueryChange = {
                    query = it
                    pageNumber = 1
                },
                sort = sort,
                onSortChange = {
                    sort = it
                    pageNumber = 1
                },
                onPageChange = {
                    pageNumber = it
                    scope.launch { gridState.scrollToItem(0) }
                },
                gridState = gridState,
                onBackClick = onBackClick,
                onAddClick = { editor = Editor(null) },
                onCustomerClick = { onOpenCustomerChange(it.id) }
            )
        }
    }
}

@Composable
private fun CustomerList(
    loaded: Boolean,
    total: Int,
    page: Page<Customer>,
    stats: Map<Int, CustomerStats>,
    query: String,
    onQueryChange: (String) -> Unit,
    sort: CustomerSort,
    onSortChange: (CustomerSort) -> Unit,
    onPageChange: (Int) -> Unit,
    gridState: LazyGridState,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onCustomerClick: (Customer) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            // Bottom padding keeps the last row clear of the floating button.
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item(span = { GridItemSpan(maxLineSpan) }) {
                TopRow(startText = "Back", onStartClick = onBackClick)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "Customers",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    SearchField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = "Search name, phone or email"
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomerSort.values().forEach { option ->
                        PillChip(
                            text = option.label,
                            selected = sort == option,
                            onClick = { onSortChange(option) }
                        )
                    }
                }
            }

            if (loaded && page.total == 0) {

                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (total == 0) {
                        EmptyState(
                            title = "No customers yet",
                            message = "Tap + Add to save your first one."
                        )
                    } else {
                        EmptyState(
                            title = "No customers found",
                            message = "Try a different name, number or email."
                        )
                    }
                }

            } else if (loaded) {

                item(span = { GridItemSpan(maxLineSpan) }) {
                    SmallCaps(
                        text = "${page.total} ${if (page.total == 1) "customer" else "customers"}",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(page.items, key = { it.id }) { customer ->
                    CustomerCard(
                        customer = customer,
                        stats = stats[customer.id],
                        onClick = { onCustomerClick(customer) }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    PaginationBar(
                        page = page,
                        onPageChange = onPageChange
                    )
                }
            }
        }

        RoundActionButton(
            text = "+ Add",
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
}

/**
 * Soft grey card: avatar, name, phone, and what the customer has spent, with a black arrow.
 * Name takes two lines so cards in a row line up.
 */
@Composable
private fun CustomerCard(
    customer: Customer,
    stats: CustomerStats?,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceVariant)
            .clickable(onClickLabel = "Open ${customer.name}", onClick = onClick)
            .padding(16.dp)
    ) {
        InitialAvatar(
            name = customer.name,
            size = 48.dp,
            shape = CircleShape,
            background = colors.surface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = customer.name,
            style = MaterialTheme.typography.titleMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = customer.phone,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        SmallCaps(
            text = "Total spent",
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = formatRupeesCompact(stats?.totalSpent ?: 0.0),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }

            ArrowCircle(
                glyph = "↗",
                container = colors.primary,
                content = colors.onPrimary
            )
        }
    }
}
