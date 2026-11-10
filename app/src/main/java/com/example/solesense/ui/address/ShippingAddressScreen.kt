package com.example.solesense.ui.address

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.model.user.Address
import com.example.solesense.repository.AddressRepository
import kotlinx.coroutines.launch

@Composable
fun ShippingAddressScreen(
    initialSelectedAddressId: String? = null,
    onBackClick: () -> Unit,
    onAddressApplied: (Address) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val addressRepository = remember { AddressRepository() }

    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var selectedAddressId by remember { mutableStateOf(initialSelectedAddressId ?: "") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Trạng thái mở BottomSheet Thêm / Sửa địa chỉ
    var isFormVisible by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Address?>(null) }

    fun loadAddressesFromFirebase(preferAddressId: String? = null) {
        scope.launch {
            isLoading = true
            val result = addressRepository.getAddresses()
            result.fold(
                onSuccess = { list ->
                    addresses = list
                    val targetId = preferAddressId
                        ?: selectedAddressId.takeIf { id -> list.any { it.addressId == id } }
                        ?: list.firstOrNull { it.isDefault }?.addressId
                        ?: list.firstOrNull()?.addressId
                        ?: ""
                    selectedAddressId = targetId
                    isLoading = false
                },
                onFailure = { e ->
                    isLoading = false
                    Toast.makeText(
                        context,
                        "Lỗi tải danh sách địa chỉ: ${e.localizedMessage ?: "Không xác định"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        loadAddressesFromFirebase(initialSelectedAddressId)
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Surface(
                color = Color(0xFFFAFAFA),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Địa chỉ giao hàng",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            color = Color(0xFF111111)
                        )
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFFFAFAFA),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            val chosen = addresses.firstOrNull { it.addressId == selectedAddressId }
                                ?: addresses.firstOrNull()
                            if (chosen != null) {
                                onAddressApplied(chosen)
                            } else {
                                Toast.makeText(context, "Vui lòng thêm địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = addresses.isNotEmpty(),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF111111),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Áp dụng",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF111111))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(addresses, key = { it.addressId }) { address ->
                    ShippingAddressCard(
                        address = address,
                        isSelected = address.addressId == selectedAddressId,
                        onSelectClick = {
                            selectedAddressId = address.addressId
                        },
                        onEditClick = {
                            editingAddress = address
                            isFormVisible = true
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Nút "+ Thêm địa chỉ mới" chuẩn thiết kế
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable {
                                editingAddress = null
                                isFormVisible = true
                            },
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFFEEEEEE)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Thêm địa chỉ mới",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Thêm địa chỉ mới",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = Color(0xFF111111)
                                )
                            )
                        }
                    }
                }
            }
        }

        // BottomSheet Thêm mới / Sửa / Xóa địa chỉ
        AddEditAddressSheet(
            isVisible = isFormVisible,
            editingAddress = editingAddress,
            isSaving = isSaving,
            onDismissRequest = {
                if (!isSaving) {
                    isFormVisible = false
                    editingAddress = null
                }
            },
            onSaveAddress = { addressId, receiverName, phone, provinceAndWard, streetDetail, isDefault ->
                scope.launch {
                    isSaving = true
                    // Tách Phường/Xã và Tỉnh/Thành phố từ chuỗi "Tỉnh/Thành phố và Phường xã"
                    val parts = provinceAndWard.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val wardPart = if (parts.size >= 2) parts.dropLast(1).joinToString(", ") else ""
                    val provincePart = if (parts.isNotEmpty()) parts.last() else provinceAndWard

                    val addressToSave = Address(
                        addressId = addressId,
                        receiverName = receiverName,
                        phone = phone,
                        province = provincePart,
                        district = "",
                        ward = wardPart,
                        detail = streetDetail,
                        isDefault = isDefault
                    )

                    val result = addressRepository.saveAddress(addressToSave)
                    isSaving = false
                    result.fold(
                        onSuccess = { saved ->
                            isFormVisible = false
                            editingAddress = null
                            Toast.makeText(
                                context,
                                if (addressId.isBlank()) "Đã thêm địa chỉ mới thành công!" else "Đã cập nhật địa chỉ thành công!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadAddressesFromFirebase(preferAddressId = saved.addressId)
                        },
                        onFailure = { e ->
                            Toast.makeText(
                                context,
                                "Lỗi lưu địa chỉ: ${e.localizedMessage ?: "Không thể kết nối Firebase"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            },
            onDeleteAddress = { addressIdToDelete ->
                scope.launch {
                    isSaving = true
                    val result = addressRepository.deleteAddress(addressIdToDelete)
                    isSaving = false
                    result.fold(
                        onSuccess = {
                            isFormVisible = false
                            editingAddress = null
                            Toast.makeText(context, "Đã xóa địa chỉ thành công!", Toast.LENGTH_SHORT).show()
                            loadAddressesFromFirebase()
                        },
                        onFailure = { e ->
                            Toast.makeText(
                                context,
                                "Lỗi xóa địa chỉ: ${e.localizedMessage ?: "Không xác định"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun ShippingAddressCard(
    address: Address,
    isSelected: Boolean,
    onSelectClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelectClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vòng tròn xám nhạt bên ngoài + vòng tròn đen bên trong chứa icon ic_map_marker
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map_marker),
                        contentDescription = "Địa chỉ",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Cột thông tin: Họ và tên + ic_person_edit + badge Mặc định | SĐT | Địa chỉ nhiều hàng
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = address.receiverName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF111111)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Icon sửa địa chỉ (ic_person_edit)
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person_edit),
                        contentDescription = "Sửa địa chỉ",
                        tint = Color(0xFF757575),
                        modifier = Modifier
                            .size(21.dp)
                            .clip(CircleShape)
                            .clickable { onEditClick() }
                            .padding(1.dp)
                    )

                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFDE4E8))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Mặc định",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFD83B56),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = address.phone,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF616161),
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Hiển thị địa chỉ tách thành các hàng nếu dài
                Text(
                    text = address.toMultiLineAddress(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF616161),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radio button tròn đen chuẩn thiết kế
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF111111), CircleShape)
                    .clickable { onSelectClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111111))
                    )
                }
            }
        }
    }
}

