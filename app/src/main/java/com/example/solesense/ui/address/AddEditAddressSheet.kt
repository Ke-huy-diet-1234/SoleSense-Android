package com.example.solesense.ui.address

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.user.Address

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressSheet(
    isVisible: Boolean,
    editingAddress: Address?,
    isSaving: Boolean = false,
    onDismissRequest: () -> Unit,
    onSaveAddress: (
        addressId: String,
        receiverName: String,
        phone: String,
        provinceAndWard: String,
        streetDetail: String,
        isDefault: Boolean
    ) -> Unit,
    onDeleteAddress: ((addressId: String) -> Unit)? = null
) {
    if (!isVisible) return

    val isEditMode = editingAddress != null && editingAddress.addressId.isNotBlank()

    // Khởi tạo dữ liệu cũ nếu đang ở chế độ Sửa địa chỉ
    var provinceAndWard by remember(editingAddress) {
        val initialProvinceWard = editingAddress?.provinceAndWardText().orEmpty()
        // Nếu dữ liệu cũ chưa tách ward/province mà gộp trong detail bởi dấu xuống dòng hoặc dấu phẩy
        val resolved = if (initialProvinceWard.isNotBlank()) {
            initialProvinceWard
        } else {
            val detailText = editingAddress?.detail.orEmpty()
            if (detailText.contains("\n")) {
                detailText.substringAfter("\n").trim()
            } else {
                ""
            }
        }
        mutableStateOf(resolved)
    }

    var streetDetail by remember(editingAddress) {
        val initialProvinceWard = editingAddress?.provinceAndWardText().orEmpty()
        val detailText = editingAddress?.detail.orEmpty()
        val resolved = if (initialProvinceWard.isBlank() && detailText.contains("\n")) {
            detailText.substringBefore("\n").trim()
        } else {
            detailText
        }
        mutableStateOf(resolved)
    }

    var receiverName by remember(editingAddress) {
        mutableStateOf(editingAddress?.receiverName ?: "")
    }

    var phone by remember(editingAddress) {
        mutableStateOf(editingAddress?.phone ?: "")
    }

    var isDefault by remember(editingAddress) {
        mutableStateOf(editingAddress?.isDefault ?: false)
    }

    var validationError by remember(editingAddress) {
        mutableStateOf<String?>(null)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Sửa địa chỉ giao hàng" else "Thêm địa chỉ mới",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = Color(0xFF1A1A1A)
                    )
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF616161)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(14.dp))

            // MỤC 1: ĐỊA CHỈ
            Text(
                text = "Địa chỉ",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A1A)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = provinceAndWard,
                onValueChange = {
                    provinceAndWard = it
                    validationError = null
                },
                label = { Text("Tỉnh/Thành phố và Phường xã") },
                placeholder = { Text("VD: Xã Đông Anh, Thành phố Hà Nội") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = streetDetail,
                onValueChange = {
                    streetDetail = it
                    validationError = null
                },
                label = { Text("Tên đường, Tòa nhà, Số nhà") },
                placeholder = { Text("VD: Số nhà 83, Thôn Lại Đà") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // MỤC 2: THÔNG TIN LIÊN HỆ
            Text(
                text = "Thông tin liên hệ",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A1A)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = receiverName,
                onValueChange = {
                    receiverName = it
                    validationError = null
                },
                label = { Text("Họ và tên") },
                placeholder = { Text("Nhập họ và tên người nhận") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    validationError = null
                },
                label = { Text("Số điện thoại") },
                placeholder = { Text("Nhập số điện thoại liên hệ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // MỤC 3: THANH GẠT ĐẶT LÀM MẶC ĐỊNH
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8F8F8),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Đặt làm địa chỉ mặc định",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                        )
                        Text(
                            text = "Tự động áp dụng địa chỉ này cho các đơn hàng tiếp theo",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF757575),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE53935),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFBDBDBD)
                        )
                    )
                }
            }

            // Hiển thị thông báo lỗi nếu thiếu dữ liệu
            if (!validationError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nút Lưu địa chỉ
            Button(
                onClick = {
                    val cleanName = receiverName.trim()
                    val cleanPhone = phone.trim()
                    val cleanProvinceWard = provinceAndWard.trim()
                    val cleanStreet = streetDetail.trim()

                    when {
                        cleanProvinceWard.isEmpty() -> {
                            validationError = "Vui lòng nhập Tỉnh/Thành phố và Phường xã"
                        }
                        cleanStreet.isEmpty() -> {
                            validationError = "Vui lòng nhập Tên đường, Tòa nhà, Số nhà"
                        }
                        cleanName.isEmpty() -> {
                            validationError = "Vui lòng nhập Họ và tên người nhận"
                        }
                        cleanPhone.isEmpty() || !cleanPhone.all { it.isDigit() || it == '+' || it == ' ' } || cleanPhone.filter { it.isDigit() }.length !in 9..11 -> {
                            validationError = "Số điện thoại không hợp lệ (cần từ 9 đến 11 chữ số)"
                        }
                        else -> {
                            onSaveAddress(
                                editingAddress?.addressId ?: "",
                                cleanName,
                                cleanPhone,
                                cleanProvinceWard,
                                cleanStreet,
                                isDefault
                            )
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111111),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (isEditMode) "Hoàn tất chỉnh sửa" else "Hoàn tất thêm địa chỉ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            // Nút Xóa địa chỉ (chỉ hiện khi đang Sửa địa chỉ)
            if (isEditMode && onDeleteAddress != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onDeleteAddress(editingAddress!!.addressId) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53935)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa địa chỉ",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Xóa địa chỉ",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

