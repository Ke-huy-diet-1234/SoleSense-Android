package com.example.solesense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.model.catalog.Brand

@Composable
fun BrandItem(
    brand: Brand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(64.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            val resolvedLogoModel = when (brand.brandId.lowercase()) {
                "nike" -> "file:///android_asset/brands/Logo_NIKE.svg"
                "adidas" -> "file:///android_asset/brands/Adidas_Logo.svg"
                "asics" -> "file:///android_asset/brands/Asics_Logo.svg"
                "newbalance", "new_balance" -> "file:///android_asset/brands/New_Balance_Logo.svg"
                "converse" -> "file:///android_asset/brands/Converse_Logo.svg"
                "puma" -> "file:///android_asset/brands/Puma_Logo.svg"
                "reebok" -> "file:///android_asset/brands/Reebok_Logo.svg"
                else -> brand.logoUrl
            }

            if (!resolvedLogoModel.isNullOrEmpty()) {
                AsyncImage(
                    model = resolvedLogoModel,
                    contentDescription = brand.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_more),
                    contentDescription = brand.name,
                    modifier = Modifier.size(24.dp),
                    tint = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))


        Text(
            text = brand.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
