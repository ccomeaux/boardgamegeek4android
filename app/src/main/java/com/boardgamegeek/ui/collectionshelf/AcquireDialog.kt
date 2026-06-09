@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui.collectionshelf

import android.annotation.SuppressLint
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import kotlinx.coroutines.android.awaitFrame
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

@Composable
fun AcquireDialog(
    item: CollectionItem,
    modifier: Modifier = Modifier,
    acquiredFromList: List<String> = emptyList(),
    onDismiss: () -> Unit = {},
    onConfirmation: (info: CollectionDetailsViewModel.AcquisitionInfo) -> Unit = {},
) {
    val pricePaidCurrency = remember { mutableStateOf(item.pricePaidCurrency) }
    val pricePaid = remember { mutableStateOf(item.pricePaid) }
    val quantity: MutableState<Int?> = remember { mutableStateOf(item.quantity) }
    val acquisitionDate = remember { mutableStateOf(item.acquisitionDate) }
    val acquiredFrom = remember { mutableStateOf(item.acquiredFrom) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                onConfirmation(
                    CollectionDetailsViewModel.AcquisitionInfo(
                        pricePaidCurrency.value,
                        pricePaid.value,
                        quantity.value,
                        acquisitionDate.value,
                        acquiredFrom.value,
                    )
                )
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(item.robustName) },
        text = {
            AcquireDialogText(pricePaidCurrency, pricePaid, quantity, acquisitionDate, acquiredFrom, acquiredFromList)
        }
    )
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun AcquireDialogText(
    pricePaidCurrency: MutableState<String>,
    pricePaid: MutableState<Double?>,
    quantity: MutableState<Int?>,
    acquisitionDate: MutableState<Long>,
    acquiredFrom: MutableState<String>,
    acquiredFromList: List<String> = emptyList(),
) {
    val currencyFormat = DecimalFormat("0.00")
    var pricePaidText by remember { mutableStateOf(if (pricePaid.value == null) "" else currencyFormat.format(pricePaid.value)) }
    var quantityText by remember { mutableStateOf(if (quantity.value == null) "" else quantity.value.toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDate =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (acquisitionDate.value == 0L) {
                    LocalDate.now()
                } else {
                    Instant.ofEpochMilli(acquisitionDate.value)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
            } else null
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.material_margin_vertical))
    ) {
        val annotatedString = AnnotatedString.fromHtml(stringResource(R.string.message_acquiring))
        Text(text = annotatedString)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.material_margin_vertical))
        ) {
            val currencyOptions = stringArrayResource(R.array.currency)
            var isCurrencyExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = isCurrencyExpanded,
                onExpandedChange = { isCurrencyExpanded = it },
                modifier = Modifier
                    .widthIn(100.dp)
                    .fillMaxWidth(0.25F)
            ) {
                OutlinedTextField(
                    value = pricePaidCurrency.value.ifEmpty { "USD" },
                    onValueChange = { pricePaidCurrency.value = it },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .wrapContentSize(),
                    label = { Text(stringResource(R.string.currency)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = MaterialTheme.shapes.small
                )
                ExposedDropdownMenu(
                    expanded = isCurrencyExpanded,
                    onDismissRequest = { isCurrencyExpanded = false }
                ) {
                    currencyOptions.forEach { label ->
                        DropdownMenuItem(
                            text = { Text(text = label) },
                            onClick = {
                                pricePaidCurrency.value = label
                                isCurrencyExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = pricePaidText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.toDoubleOrNull() != null) {
                        pricePaidText = input
                        pricePaid.value = input.toDoubleOrNull() ?: 0.0
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                label = { Text(stringResource(R.string.price)) },
                trailingIcon = {
                    IconButton(onClick = {
                        pricePaidText = ""
                        pricePaid.value = null
                    }) {
                        Icon(painterResource(R.drawable.clear_24px), contentDescription = null)
                    }
                }
            )
        }
        OutlinedTextField(
            value = quantityText,
            onValueChange = { input ->
                quantityText = input.filter { it.isDigit() }
                quantity.value = quantityText.toIntOrNull()
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.small,
            label = { Text(stringResource(R.string.quantity)) },
            trailingIcon = {
                IconButton(onClick = {
                    quantityText = ""
                    quantity.value = null
                }) {
                    Icon(painterResource(R.drawable.clear_24px), contentDescription = null)
                }
            }
        )
        OutlinedButton(
            onClick = {
                showDatePicker = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            ),
            contentPadding = PaddingValues(horizontal = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (acquisitionDate.value == 0L)
                        stringResource(R.string.acquisition_date)
                    else
                        acquisitionDate.value.formatDateTime(
                            LocalContext.current,
                            ResourcesCompat.ID_NULL,
                            DateUtils.FORMAT_SHOW_DATE
                        ).toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (acquisitionDate.value == 0L) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_calendar_today_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        var isAcquiredFromExpanded by remember { mutableStateOf(false) }
        val acquiredFromFilteredList by remember { derivedStateOf { acquiredFromList.filter { it.contains(acquiredFrom.value, true) } } }
        ExposedDropdownMenuBox(
            expanded = isAcquiredFromExpanded,
            onExpandedChange = { isAcquiredFromExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = acquiredFrom.value,
                onValueChange = { acquiredFrom.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = MaterialTheme.shapes.small,
                label = { Text(stringResource(R.string.acquired_from)) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                trailingIcon = {
                    IconButton(onClick = { acquiredFrom.value = "" }) {
                        Icon(painterResource(R.drawable.clear_24px), contentDescription = null)
                    }
                }
            )
            ExposedDropdownMenu(
                expanded = isAcquiredFromExpanded,
                onDismissRequest = { isAcquiredFromExpanded = false },
                modifier = Modifier
                    .exposedDropdownSize()
                    .requiredSizeIn(maxHeight = 200.dp)
            ) {
                acquiredFromFilteredList.forEach { label ->
                    DropdownMenuItem(
                        text = { Text(text = label) },
                        onClick = {
                            acquiredFrom.value = label
                            isAcquiredFromExpanded = false
                        }
                    )
                }
            }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp ->
                        val timeZone = TimeZone.getDefault()
                        acquisitionDate.value = timestamp - timeZone.getOffset(timestamp)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis = acquisitionDate.value // reset back to the initial value
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
private fun DialogTextPreview() {
    AcquireDialogText(
        mutableStateOf("USD"),
        mutableStateOf(24.99),
        mutableStateOf(1),
        mutableStateOf(0L), // 1234567890L
        mutableStateOf("Store"),
    )
}
