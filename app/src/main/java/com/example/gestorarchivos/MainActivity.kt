package com.example.gestorarchivos

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gestorarchivos.model.FileType
import com.example.gestorarchivos.ui.screens.FileExplorerScreen
import com.example.gestorarchivos.ui.screens.ImageViewer
import com.example.gestorarchivos.ui.screens.TextFileViewer
import com.example.gestorarchivos.ui.theme.GestorArchivosTheme
import com.example.gestorarchivos.util.PermissionUtils
import com.example.gestorarchivos.viewmodel.FileExplorerViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Se necesitan permisos de almacenamiento para usar la aplicación",
                Toast.LENGTH_LONG
            ).show()

            // En Android 11+, dirigir a configuración para permisos especiales
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionUtils.openManageStorageSettings(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar y solicitar permisos
        checkAndRequestPermissions()

        setContent {
            val viewModel: FileExplorerViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            GestorArchivosTheme(
                appTheme = uiState.appTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionUtils.hasStoragePermission(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Solicitar permiso especial
                PermissionUtils.openManageStorageSettings(this)
            } else {
                // Android 10 y anteriores - Permisos normales
                requestPermissionLauncher.launch(PermissionUtils.getRequiredPermissions())
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: FileExplorerViewModel) {
    val navController = rememberNavController()
    var currentFile by remember { mutableStateOf<File?>(null) }

    NavHost(
        navController = navController,
        startDestination = "file_explorer"
    ) {
        composable("file_explorer") {
            FileExplorerScreen(
                viewModel = viewModel,
                onNavigateToViewer = { fileItem ->
                    currentFile = fileItem.file
                    when (fileItem.getFileType()) {
                        FileType.IMAGE -> {
                            navController.navigate("image_viewer")
                        }
                        FileType.TEXT,
                        FileType.JSON,
                        FileType.XML -> {
                            navController.navigate("text_viewer")
                        }
                        else -> {
                            // No debería llegar aquí
                        }
                    }
                }
            )
        }

        composable("text_viewer") {
            currentFile?.let { file ->
                TextFileViewer(
                    file = file,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("image_viewer") {
            currentFile?.let { file ->
                ImageViewer(
                    file = file,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}