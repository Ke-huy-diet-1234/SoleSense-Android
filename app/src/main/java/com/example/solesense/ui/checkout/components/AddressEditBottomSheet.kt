package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.user.Address

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEditBottomSheet(
    isVisible: Boolean,
    currentAddress: Address?,
    onDismissRequest: () -> Unit,
    onSaveAddress: (receiverName: String, phone: String, detail: String) -> Unit
) {
    if (!isVisible) return

    var receiverName by remember(currentAddress) {
        mutableStateOf(currentAddress?.receiverName ?: "")
    }
    var phone by remember(currentAddress) {
        mutableStateOf(currentAddress?.phone ?: "")
    }
    var detail by remember(currentAddress) {
        mutableStateOf(currentAddress?.detail ?: "")
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Cập nhật địa chỉ nhận hàng",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = receiverName,
                onValueChange = { receiverName = it },
                label = { Text("Họ và tên người nhận") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                label = { Text("Địa chỉ chi tiết (Số nhà, Đường, Quận/Huyện, Tỉnh/TP)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (receiverName.isNotBlank() && phone.isNotBlank() && detail.isNotBlank()) {
                        onSaveAddress(receiverName.trim(), phone.trim(), detail.trim())
                    }
                },
                enabled = receiverName.isNotBlank() && phone.isNotBlank() && detail.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Lưu địa chỉ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

