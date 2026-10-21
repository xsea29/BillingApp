package com.example.billingapp.ui.stock

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.billingapp.AppDatabase
import com.example.billingapp.Product
import com.example.billingapp.SoldLine
import com.example.billingapp.StockEntry
import com.example.billingapp.ui.components.BorderedCard
import com.example.billingapp.ui.components.ConfirmDialog
import com.example.billingapp.ui.components.DeleteButton
import com.example.billingapp.ui.components.EmptyState
import com.example.billingapp.ui.components.FilterIconButton
import com.example.billingapp.ui.components.PaginationBar
import com.example.billingapp.ui.components.PillChip
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.newbill.InvoiceDatePickerDialog
import com.example.billingapp.ui.products.ProductForm
import com.example.billingapp.ui.products.categoriesOf
import com.example.billingapp.ui.theme.MoneyGreen
import com.example.billingapp.util.formatDateTime
import com.example.billingapp.util.Page
import com.example.billingapp.util.moveToPickedDay
import com.example.billingapp.util.paginate
import com.example.billingapp.util.startOfDay
import kotlinx.coroutines.launch

/** Which page of the Stocks section is showing. */
private enum class StocksPage { List, Add, NewProduct }

/**
 * Stock, day by day. The list shows, for Today, Yesterday, this month, this year or one picked
 * date, what each product started with, what came in, what was sold and what is left. + Add
 * enters new stock (see [AddStockPage]). Yesterday's leftover is always today's starting point.
 */
@Composable
fun StocksScreen(
    db: AppDatabase,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // null = still loading.
    var entries by remember { mutableStateOf<List<StockEntry>?>(null) }
    var sold by remember { mutableStateOf<List<SoldLine>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // One of the two is set: a ready-made period, or a day picked on the calendar.
    var period by remember { mutableStateOf<StockPeriod?>(StockPeriod.Today) }
    var pickedDay by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var page by remember { mutableStateOf(StocksPage.List) }
    var pageNumber by remember { mutableStateOf(1) }
    var newProductName by remember { mutableStateOf("") }

    // What is being added on the Add page, by product name. Lives here so it survives a trip to
    // the new-product form and back.
    var draft by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    var detailName by remember { mutableStateOf<String?>(null) }
    var pendingRemove by remember { mutableStateOf<StockEntry?>(null) }

    suspend fun reload() {
        now = System.currentTimeMillis()

        val all = db.stockDao().getAll()
        entries = all

        // Bills from before the first stock was entered never count, so don't even load them.
        sold = if (all.isEmpty()) {
            emptyList()
        } else {
            db.invoiceItemDao().getSoldLines(startOfDay(all.minOf { it.addedAt }))
        }

        products = db.productDao().getAllProducts()
    }

    LaunchedEffect(Unit) { reload() }

    BackHandler {
        when (page) {
            StocksPage.NewProduct -> page = StocksPage.Add
            StocksPage.Add -> {
                draft = emptyMap()
                page = StocksPage.List
            }
            StocksPage.List -> onBackClick()
        }
    }

    val allEntries = entries.orEmpty()

    val (start, end) = pickedDay?.let(::dayRange) ?: (period ?: StockPeriod.Today).range(now)

    val rows = remember(entries, sold, start, end) { stockRows(allEntries, sold, start, end) }

    // Ten products at a time. A page number that no longer exists (after removing stock, say)
    // is moved to the nearest real one.
    val rowsPage = paginate(rows, pageNumber)

    // What each product has left right now, for the Add page.
    val leftByName = remember(entries, sold, now) {
        val (todayStart, todayEnd) = StockPeriod.Today.range(now)
        stockRows(allEntries, sold, todayStart, todayEnd).associate { it.name to it.remaining }
    }

    val addedToday = remember(entries, now) {
        val (todayStart, todayEnd) = StockPeriod.Today.range(now)
        allEntries.any { it.addedAt >= todayStart && it.addedAt < todayEnd }
    }

    when (page) {

        StocksPage.NewProduct -> ProductForm(
            existing = null,
            categories = categoriesOf(products),
            initialName = newProductName,
            onBackClick = { page = StocksPage.Add },
            onSave = { name, price, category ->
                scope.launch {
                    db.productDao().insert(Product(name = name, price = price, category = category))
                    products = db.productDao().getAllProducts()

                    // Straight onto the list being added, ready for its quantity.
                    draft = draft + (name to (draft[name] ?: 1))
                    page = StocksPage.Add
                }
            },
            onDelete = {}
        )

        StocksPage.Add -> AddStockPage(
            products = products,
            leftByName = leftByName,
            draft = draft,
            onQuantityChange = { product, quantity -> draft = draft + (product.name to quantity) },
            onNewProduct = {
                newProductName = it
                page = StocksPage.NewProduct
            },
            onBackClick = {
                draft = emptyMap()
                page = StocksPage.List
            },
            onSave = {
                scope.launch {
                    val at = System.currentTimeMillis()

                    val fresh = draft
                        .filterValues { it > 0 }
                        .map { (name, quantity) -> StockEntry(productName = name, quantity = quantity, addedAt = at) }

                    if (fresh.isNotEmpty()) db.stockDao().insertAll(fresh)

                    draft = emptyMap()
                    reload()

                    // Show what was just added.
                    period = StockPeriod.Today
                    pickedDay = null
                    pageNumber = 1
                    page = StocksPage.List
                }
            }
        )

        StocksPage.List -> StockList(
            loaded = entries != null,
            hasAnyStock = allEntries.isNotEmpty(),
            rows = rowsPage,
            title = stockRangeTitle(period, pickedDay, now),
            period = period,
            datePicked = pickedDay != null,
            askToAdd = pickedDay == null && period == StockPeriod.Today && allEntries.isNotEmpty() && !addedToday,
            onPeriodChange = {
                period = it
                pickedDay = null
                pageNumber = 1
            },
            onPageChange = { pageNumber = it },
            onFilterClick = { showDatePicker = true },
            onRowClick = { detailName = it.name },
            onAddClick = { page = StocksPage.Add },
            onBackClick = onBackClick
        )
    }

    if (showDatePicker) {
        InvoiceDatePickerDialog(
            current = pickedDay ?: now,
            onDismiss = { showDatePicker = false },
            onPick = {
                pickedDay = startOfDay(moveToPickedDay(now, it))
                period = null
                pageNumber = 1
            }
        )
    }

    val detail = detailName?.let { name -> rows.firstOrNull { it.name == name } }

    if (detail != null) {
        StockDetailDialog(
            row = detail,
            added = allEntries.filter { it.productName == detail.name && it.addedAt >= start && it.addedAt < end },
            onRemove = { pendingRemove = it },
            onDismiss = { detailName = null }
        )
    }

    pendingRemove?.let { entry ->
        ConfirmDialog(
            title = "Remove this stock?",
            message = "+${entry.quantity} of ${entry.productName}, added ${formatDateTime(entry.addedAt)}. " +
                    "It will be taken off the stock count.",
            confirmLabel = "Remove",
            onConfirm = {
                pendingRemove = null
                scope.launch {
                    db.stockDao().deleteById(entry.id)
                    reload()
                }
            },
            onDismiss = { pendingRemove = null }
        )
    }
}

@Composable
private fun StockList(
    loaded: Boolean,
    hasAnyStock: Boolean,
    rows: Page<StockRow>,
    title: String,
    period: StockPeriod?,
    datePicked: Boolean,
    askToAdd: Boolean,
    onPeriodChange: (StockPeriod) -> Unit,
    onPageChange: (Int) -> Unit,
    onFilterClick: () -> Unit,
    onRowClick: (StockRow) -> Unit,
    onAddClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Bottom padding keeps the last row clear of the floating button.
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp)
        ) {

            item {
                TopRow(startText = "Back", onStartClick = onBackClick)
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = "Stocks",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    // The periods slide sideways; the date filter stays put at the end.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StockPeriod.values().forEach { option ->
                                PillChip(
                                    text = option.label,
                                    selected = period == option,
                                    onClick = { onPeriodChange(option) }
                                )
                            }
                        }

                        FilterIconButton(
                            active = datePicked,
                            contentDescription = "Pick a date",
                            onClick = onFilterClick
                        )
                    }
                }
            }

            if (loaded) {

                if (askToAdd) {
                    item {
                        AddTodayCard(
                            onAddClick = onAddClick,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }

                if (!hasAnyStock) {

                    item {
                        EmptyState(
                            title = "No stock yet",
                            message = "Tap + Add to enter what came in today."
                        )
                    }

                } else {

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SmallCaps(text = title)
                            SmallCaps(text = "${rows.total} ${if (rows.total == 1) "product" else "products"}")
                        }
                    }

                    if (rows.total == 0) {
                        item {
                            EmptyState(
                                title = "Nothing in stock",
                                message = "No stock was counted for this time."
                            )
                        }
                    }

                    items(rows.items, key = { it.name }) { row ->
                        StockRowItem(row = row, onClick = { onRowClick(row) })
                    }

                    item {
                        PaginationBar(
                            page = rows,
                            onPageChange = {
                                onPageChange(it)
                                scope.launch { listState.scrollToItem(0) }
                            }
                        )
                    }
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

/** The daily nudge: yesterday's leftover is already counted, so only what came in today is missing. */
@Composable
private fun AddTodayCard(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    BorderedCard(modifier = modifier) {
        Text(
            text = "Add today's stock",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "What was left yesterday is already counted. Enter what came in today.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Button(
            onClick = onAddClick,
            modifier = Modifier
                .padding(top = 16.dp)
                .height(44.dp)
        ) {
            Text("Add stock")
        }
    }
}

/**
 * Name and "Opening 5 · Added 12" on the left; on the right, what was sold (green) and what is left
 * (red) as two numbers with their labels under them: "4 | 18". Tap for details.
 */
@Composable
private fun StockRowItem(row: StockRow, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Details of ${row.name}", onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Opening ${row.opening} · Added ${row.added}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.padding(start = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Figure(value = row.sold, label = "Sold", color = MoneyGreen)

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(colors.outlineVariant)
            )

            Figure(value = row.remaining, label = "Left", color = colors.error)
        }
    }
}

/** A number with its small-caps label under it, right-aligned: "4" over "SOLD". */
@Composable
private fun Figure(value: Int, label: String, color: Color) {
    Column(
        modifier = Modifier.widthIn(min = 44.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        SmallCaps(text = label)
    }
}

/**
 * One product's figures for the time being looked at, and each delivery that was entered in it
 * with a bin to take a wrong one back out (a typo of 400 for 40).
 */
@Composable
private fun StockDetailDialog(
    row: StockRow,
    added: List<StockEntry>,
    onRemove: (StockEntry) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.name) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Opening ${row.opening} · Added ${row.added} · Sold ${row.sold} · Left ${row.remaining}",
                    style = MaterialTheme.typography.bodyMedium
                )

                SmallCaps(
                    text = "Stock added",
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
                )

                if (added.isEmpty()) {
                    Text(
                        text = "Nothing was added in this time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                added.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDateTime(entry.addedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        DeleteButton(
                            onClick = { onRemove(entry) },
                            contentDescription = "Remove +${entry.quantity}"
                        )

                        // Last on the line, with room for "+100", so the numbers line up down the list.
                        Text(
                            text = "+${entry.quantity}",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(min = 44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
