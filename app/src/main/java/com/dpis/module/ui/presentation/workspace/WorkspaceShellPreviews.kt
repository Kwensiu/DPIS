package com.dpis.module.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
private fun WorkspaceShellPreviewContent() {
    ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
        WorkspaceShell(
            selectedDestination = WorkspaceDestination.HOME,
            onDestinationSelected = {},
            isCompactUi = false
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text("Home", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Preview(name = "Phone", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
private fun WorkspaceShellPhonePreview() {
    WorkspaceShellPreviewContent()
}

@Preview(name = "Tablet", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun WorkspaceShellTabletPreview() {
    WorkspaceShellPreviewContent()
}

@Preview(name = "Desktop", widthDp = 1280, heightDp = 900, showBackground = true)
@Composable
private fun WorkspaceShellDesktopPreview() {
    WorkspaceShellPreviewContent()
}
