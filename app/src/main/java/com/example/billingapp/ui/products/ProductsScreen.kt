package com.example.billingapp.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import com.example.billingapp.Product
import com.example.billingapp.ui.components.EmptyState
import com.example.billingapp.ui.components.PillChip
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SearchField
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.theme.SoftGreen
import com.example.billingapp.util.formatRupees
import kotlinx.coroutines.launch

/** What the editor page is showing: [product] = null means "add a new one". */
private data class Editor(val product: Product?)

/** The catalogue: search, category chips and a grid of product cards. Tap a card to edit it. */
@Composable
fun ProductsScreen(
    db: AppDatabase,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // null = still loading.
    var products by remember { mutableStateOf<List<Product>?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<Editor?>(null) }

    // Kept here, above the list/editor switch, so the scroll position survives an edit.
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        products = db.productDao().getAllProducts()
    }

    BackHandler {
        if (editor != null) editor = null else onBackClick()
    }

    val all = products.orEmpty()
    val categories = remember(products) { categoriesOf(all) }

    val current = editor

    if (current != null) {

        ProductForm(
            existing = current.product,
            categories = categories,
            onBackClick = { editor = null },
            onSave = { name, price, categoryName ->
                scope.launch {
                    val saved = Product(
                        id = current.product?.id ?: 0,
                        name = name,
                        price = price,
                        category = categoryName
                    )

                    if (current.product == null) {
                        db.productDao().insert(saved)
                    } else {
                        db.productDao().update(saved)
                    }

                    products = db.productDao().getAllProducts()
                    editor = null
                }
            },
            onDelete = {
                val toDelete = current.product ?: return@ProductForm

                scope.launch {
                    db.productDao().delete(toDelete)
                    products = db.productDao().getAllProducts()

                    // The chip for a category that no longer has products would show nothing.
                    if (category != null && products.orEmpty().none { it.category == category }) {
                        category = null
                    }

                    editor = null
                }
            }
        )

    } else {

        val shown = remember(products, query, category) { filterProducts(all, query, category) }

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
                            text = "Products",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        SearchField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Search name or category"
                        )
                    }
                }

                if (categories.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            item {
                                PillChip(
                                    text = "All",
                                    selected = category == null,
                                    onClick = { category = null }
                                )
                            }

                            items(categories) { option ->
                                PillChip(
                                    text = option,
                                    selected = category == option,
                                    onClick = { category = option }
                                )
                            }
                        }
                    }
                }

                if (products != null && shown.isEmpty()) {

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (all.isEmpty()) {
                            EmptyState(
                                title = "No products yet",
                                message = "Tap + Add to create your first one."
                            )
                        } else {
                            EmptyState(
                                title = "No products found",
                                message = "Try another search or category."
                            )
                        }
                    }

                } else if (products != null) {

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SmallCaps(
                            text = "${shown.size} ${if (shown.size == 1) "product" else "products"}",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(shown, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onClick = { editor = Editor(product) }
                        )
                    }
                }
            }

            RoundActionButton(
                text = "+ Add",
                onClick = { editor = Editor(null) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }
}

/**
 * Outlined card: a grey block with the product's first letter (standing in for a photo),
 * then category, name and a light-green price strip. Name always takes two lines so cards in a
 * row line up.
 */
@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(onClickLabel = "Edit ${product.name}", onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = product.name.trim().firstOrNull()?.uppercase() ?: "#",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = product.category,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )

        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(SoftGreen)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = formatRupees(product.price),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
