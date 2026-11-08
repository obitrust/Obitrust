package com.example.obitrust.ui.ebooks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.obitrust.data.model.EBookEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EBooksScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val ebooks by viewModel.ebooks.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBookForReading by remember { mutableStateOf<EBookEntity?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    val categories = remember(ebooks) {
        listOf("All") + ebooks.map { it.category }.distinct()
    }

    val filteredBooks = remember(ebooks, selectedCategory, searchQuery) {
        ebooks.filter { b ->
            val matchCat = selectedCategory == "All" || b.category == selectedCategory
            val matchQuery = searchQuery.isEmpty() ||
                    b.title.contains(searchQuery, ignoreCase = true) ||
                    b.summary.contains(searchQuery, ignoreCase = true) ||
                    b.contentMarkdown.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomDialog = true },
                containerColor = GoldPrimary,
                contentColor = NavyDark,
                modifier = Modifier.testTag("add_ebook_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add E-Book / Note")
            }
        },
        modifier = modifier.fillMaxSize().testTag("ebooks_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Academy Banner
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth().testTag("academy_banner_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Obitrust Business Academy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Master bookkeeping, double-entry accounting, Ghana taxation and business growth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search lessons, tax rules, accounting guides...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("ebook_search_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBooks) { book ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBookForReading = book }
                            .testTag("ebook_card_${book.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (book.isBookmarked) Icons.Default.Bookmark else Icons.Default.Book,
                                        contentDescription = null,
                                        tint = if (book.isBookmarked) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        book.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Badge(
                                    containerColor = when (book.level) {
                                        "Beginner" -> EmeraldProfit.copy(alpha = 0.2f)
                                        "Intermediate" -> GoldPrimary.copy(alpha = 0.2f)
                                        else -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                    },
                                    contentColor = when (book.level) {
                                        "Beginner" -> EmeraldProfit
                                        "Intermediate" -> GoldPrimary
                                        else -> Color(0xFF38BDF8)
                                    }
                                ) {
                                    Text(book.level, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                book.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${book.readTimeMinutes} min read • ${book.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { selectedBookForReading = book },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Read Lesson", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // In-App E-Book Reader Modal
    selectedBookForReading?.let { book ->
        EBookReaderDialog(
            book = book,
            onDismiss = { selectedBookForReading = null },
            onUpdateProgress = { prog ->
                viewModel.updateEBook(book.copy(progressPercent = prog))
            },
            onToggleBookmark = {
                val updated = book.copy(isBookmarked = !book.isBookmarked)
                viewModel.updateEBook(updated)
                selectedBookForReading = updated
            },
            onSaveNotes = { notes ->
                val updated = book.copy(userNotes = notes)
                viewModel.updateEBook(updated)
                selectedBookForReading = updated
            }
        )
    }

    // Add Custom E-Book / Note Dialog
    if (showAddCustomDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Business Growth") }
        var newSummary by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCustomDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("add_ebook_dialog")
            ) {
                LazyColumn(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Add Custom Business Guide / E-Book", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Save custom SOPs, training manuals, or accounting procedures", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    item {
                        OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    item {
                        OutlinedTextField(value = newSummary, onValueChange = { newSummary = it }, label = { Text("Summary") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    item {
                        OutlinedTextField(
                            value = newContent,
                            onValueChange = { newContent = it },
                            label = { Text("Content (Markdown / Text) *") },
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showAddCustomDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            Button(
                                onClick = {
                                    if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                                        viewModel.addCustomEBook(newTitle, newCategory, newContent, newSummary) {
                                            showAddCustomDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Publish to Library", color = NavyDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EBookReaderDialog(
    book: EBookEntity,
    onDismiss: () -> Unit,
    onUpdateProgress: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    var userNotes by remember { mutableStateOf(book.userNotes) }
    var showNotesInput by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(4.dp).testTag("reader_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                // Reader Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${book.author} • ${book.level}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        IconButton(onClick = onToggleBookmark) {
                            Icon(
                                imageVector = if (book.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (book.isBookmarked) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showNotesInput = !showNotesInput }) {
                            Icon(Icons.Default.EditNote, contentDescription = "Notes", tint = GoldPrimary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (showNotesInput) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Personal Study Notes & Highlights:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = userNotes,
                                onValueChange = { userNotes = it },
                                placeholder = { Text("Jot down practical takeaways or business actions...") },
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = {
                                    onSaveNotes(userNotes)
                                    showNotesInput = false
                                }) {
                                    Text("Save Note", fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                            }
                        }
                    }
                }

                // Lesson Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = book.contentMarkdown,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reading Progress: 100%", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                    Button(
                        onClick = {
                            onUpdateProgress(100)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Complete Lesson", color = NavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
