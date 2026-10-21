package com.example.billingapp.util

/** How many rows a list shows on one page. */
const val PAGE_SIZE = 10

/** One page of a longer list, plus what the "11–20 of 47" and "Page 2 of 5" labels need. */
data class Page<T>(
    val items: List<T>,
    /** This page's number, starting at 1. */
    val page: Int,
    val pageCount: Int,
    /** Number of the first and last row on this page (0 and 0 when the list is empty). */
    val from: Int,
    val to: Int,
    val total: Int
) {
    val hasPrevious: Boolean get() = page > 1
    val hasNext: Boolean get() = page < pageCount
}

/**
 * Takes page [requestedPage] (counting from 1) of [all], [pageSize] rows at a time.
 * A page number that is too low or too high is moved to the nearest real page, so a list
 * that shrinks (a deleted invoice, a new search) can never leave you on a page that no
 * longer exists.
 */
fun <T> paginate(all: List<T>, requestedPage: Int, pageSize: Int = PAGE_SIZE): Page<T> {
    val pageCount = maxOf(1, (all.size + pageSize - 1) / pageSize)
    val page = requestedPage.coerceIn(1, pageCount)
    val start = (page - 1) * pageSize
    val items = all.subList(start, minOf(start + pageSize, all.size))

    return Page(
        items = items,
        page = page,
        pageCount = pageCount,
        from = if (all.isEmpty()) 0 else start + 1,
        to = start + items.size,
        total = all.size
    )
}
