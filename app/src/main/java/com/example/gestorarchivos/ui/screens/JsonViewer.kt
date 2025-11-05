package com.example.gestorarchivos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonViewer(
    file: File,
    onBack: () -> Unit
) {
    var formattedJson by remember { mutableStateOf("Cargando...") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        try {
            val content = file.readText()
            val jsonElement = JsonParser.parseString(content)
            val gson = GsonBuilder().setPrettyPrinting().create()
            formattedJson = gson.toJson(jsonElement)
            isLoading = false
        } catch (e: Exception) {
            error = "Error al leer el archivo JSON: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize()
                    )
                }
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                formattedJson.lines().forEach { line ->
                                    when {
                                        line.contains("\"") && line.contains(":") -> {
                                            // Clave: valor
                                            val parts = line.split(":")
                                            withStyle(SpanStyle(color = Color(0xFF2196F3))) {
                                                append(parts[0])
                                            }
                                            append(":")
                                            if (parts.size > 1) {
                                                withStyle(SpanStyle(color = Color(0xFF4CAF50))) {
                                                    append(parts.subList(1, parts.size).joinToString(":"))
                                                }
                                            }
                                        }
                                        else -> append(line)
                                    }
                                    append("\n")
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}