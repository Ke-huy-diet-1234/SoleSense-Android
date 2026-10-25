package com.example.solesense.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun OrDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
    }
}


@Preview(
    showBackground = true,

    name = "Welcome Screen"
)
@Composable
fun WelcomeScreenPreview() {
    OrDivider("")
}