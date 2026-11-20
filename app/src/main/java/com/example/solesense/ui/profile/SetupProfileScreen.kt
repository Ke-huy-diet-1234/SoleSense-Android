package com.example.solesense.ui.profile

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.R
import com.example.solesense.model.user.Address
import com.example.solesense.repository.AddressRepository
import com.example.solesense.utils.AssetConfig
import com.example.solesense.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

/**
 * Màn hình Hoàn thiện hồ sơ / Cập nhật hồ sơ (Sao y cấu trúc Ảnh 11 & Ảnh 12):
 * - Sử dụng icon Edit (cây bút) ở góc dưới phải Avatar
 * - Chỉ chọn Avatar từ 8 tài nguyên có sẵn trên GitHub -> lưu URL trực tiếp lên Firebase Firestore
 * - Lưu thông tin cá nhân & tự động tạo Địa chỉ nhận hàng mặc định
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    isInitialOnboardingSetup: Boolean = false,
    onCompleted: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val addressRepository = remember { AddressRepository() }
    val db = remember { FirebaseFirestore.getInstance() }

    var selectedAvatarUrl by remember(currentUser?.userId) {
        mutableStateOf(
            currentUser?.avatarUrl?.takeIf { it.isNotBlank() } ?: ""
        )
    }
    LaunchedEffect(currentUser?.avatarUrl) {
        val remoteAva = currentUser?.avatarUrl
        if (!remoteAva.isNullOrBlank()) {
            selectedAvatarUrl = remoteAva
        }
    }
    var fullName by remember(currentUser?.fullName) {
        mutableStateOf(currentUser?.fullName.orEmpty())
    }
    var nickname by remember(currentUser?.nickname) {
        mutableStateOf(currentUser?.nickname.orEmpty())
    }
    var dateOfBirth by remember(currentUser?.dateOfBirth) {
        mutableStateOf(currentUser?.dateOfBirth.orEmpty())
    }
    var email by remember(currentUser?.email) {
        mutableStateOf(currentUser?.email.orEmpty())
    }
    var phone by remember(currentUser?.phone) {
        mutableStateOf(currentUser?.phone.orEmpty())
    }
    var gender by remember(currentUser?.gender) {
        mutableStateOf(currentUser?.gender?.takeIf { it.isNotBlank() } ?: "Nam")
    }
    var province by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var addressDetail by remember(currentUser?.defaultAddress) {
        mutableStateOf(currentUser?.defaultAddress.orEmpty())
    }

    var isAvatarPickerOpen by remember { mutableStateOf(false) }
    var isGenderMenuOpen by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Tải địa chỉ mặc định hiện tại nếu người dùng đã từng lưu
    LaunchedEffect(currentUser?.userId) {
        val uid = currentUser?.userId ?: return@LaunchedEffect
        addressRepository.getDefaultAddress(uid).onSuccess { addr ->
            if (addr != null) {
                if (fullName.isBlank()) fullName = addr.receiverName
                if (phone.isBlank()) phone = addr.phone
                province = addr.province
                ward = addr.ward
                addressDetail = addr.detail
            }
        }
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateOfBirth = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
            },
            cal.get(Calendar.YEAR) - 20,
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Surface(color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color(0xFF111111)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Hoàn thiện hồ sơ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color(0xFF111111)
                        )
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            val cleanName = fullName.trim()
                            val cleanPhone = phone.trim()
                            val cleanDetail = addressDetail.trim()
                            val cleanProvince = province.trim()
                            val cleanWard = ward.trim()

                            if (cleanName.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập Họ và tên", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (cleanPhone.isBlank() || cleanPhone.length < 9) {
                                Toast.makeText(context, "Vui lòng nhập Số điện thoại hợp lệ", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (cleanDetail.isBlank() && cleanProvince.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập Địa chỉ nhận hàng mặc định", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val uid = currentUser?.userId ?: UserSessionManager.getActiveUserId()
                            if (uid.isNullOrBlank()) {
                                Toast.makeText(context, "Phiên đăng nhập không hợp lệ", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isSaving = true
                                try {
                                    val now = Timestamp.now()
                                    val combinedDefaultAddress = listOf(cleanDetail, cleanWard, cleanProvince)
                                        .filter { it.isNotBlank() }
                                        .joinToString(", ")

                                    val effectiveAvatarUrl = selectedAvatarUrl.ifBlank {
                                        currentUser?.avatarUrl.orEmpty()
                                    }

                                    // 1. Lưu cập nhật thông tin User + avatarUrl lên Firebase Firestore
                                    val userUpdates = mapOf(
                                        "userId" to uid,
                                        "fullName" to cleanName,
                                        "nickname" to nickname.trim().ifBlank { cleanName.substringAfterLast(" ") },
                                        "dateOfBirth" to dateOfBirth.trim(),
                                        "email" to email.trim(),
                                        "phone" to cleanPhone,
                                        "gender" to gender,
                                        "defaultAddress" to combinedDefaultAddress,
                                        "avatarUrl" to effectiveAvatarUrl,
                                        "photoUrl" to effectiveAvatarUrl,
                                        "avatar" to effectiveAvatarUrl,
                                        "isProfileCompleted" to true,
                                        "updatedAt" to now
                                    )
                                    db.collection("users")
                                        .document(uid)
                                        .set(userUpdates, SetOptions.merge())
                                        .await()

                                    // 2. Tự động tạo/cập nhật làm Địa chỉ nhận hàng mặc định (isDefault = true)
                                    val existingDefault = addressRepository.getDefaultAddress(uid).getOrNull()
                                    val defaultAddressObj = Address(
                                        addressId = existingDefault?.addressId ?: "",
                                        receiverName = cleanName,
                                        phone = cleanPhone,
                                        province = cleanProvince.ifBlank { "Việt Nam" },
                                        district = "",
                                        ward = cleanWard,
                                        detail = cleanDetail.ifBlank { combinedDefaultAddress },
                                        isDefault = true,
                                        createdAt = existingDefault?.createdAt ?: now,
                                        updatedAt = now
                                    )
                                    addressRepository.saveAddress(defaultAddressObj, uid)

                                    // 3. Đồng bộ ngay lập tức State trên toàn app (Trang chủ, Cá nhân) & làm mới từ Firebase
                                    UserSessionManager.updateLocalUser { old ->
                                        old.copy(
                                            fullName = cleanName,
                                            nickname = nickname.trim().ifBlank { cleanName.substringAfterLast(" ") },
                                            dateOfBirth = dateOfBirth.trim(),
                                            email = email.trim(),
                                            phone = cleanPhone,
                                            gender = gender,
                                            defaultAddress = combinedDefaultAddress,
                                            avatarUrl = effectiveAvatarUrl,
                                            isProfileCompleted = true,
                                            updatedAt = now
                                        )
                                    }
                                    UserSessionManager.refreshCurrentUser()

                                    Toast.makeText(context, "Đã lưu hồ sơ & địa chỉ mặc định!", Toast.LENGTH_SHORT).show()
                                    onCompleted()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi lưu hồ sơ: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF141414),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = if (isSaving) "Đang lưu..." else "Tiếp tục",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. AVATAR TRÒN + NÚT ICON EDIT (CÂY BÚT) Ở GÓC DƯỚI PHẢI (ĐÚNG NHƯ ẢNH 12)
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clickable { isAvatarPickerOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = selectedAvatarUrl,
                            contentDescription = "Avatar",
                            placeholder = painterResource(id = R.drawable.ic_solesense_logo),
                            error = painterResource(id = R.drawable.ic_solesense_logo),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Nút vuông bo góc màu đen chứa icon Edit (cây bút) màu trắng
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF141414))
                        .border(2.dp, Color.White, RoundedCornerShape(9.dp))
                        .clickable { isAvatarPickerOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chọn Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Chạm vào biểu tượng bút để chọn ảnh đại diện",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // 2. CÁC TRƯỜNG NHẬP THÔNG TIN (THEO ẢNH 11 & 12)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Họ và tên
                ProfileGrayInputBox(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = "Họ và tên"
                )

                // Biệt danh
                ProfileGrayInputBox(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = "Biệt danh"
                )

                // Ngày sinh (kèm icon Lịch)
                ProfileGrayInputBox(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    placeholder = "Ngày sinh (DD/MM/YYYY)",
                    trailingIcon = {
                        IconButton(
                            onClick = { openDatePicker() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = "Chọn ngày sinh",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                // Email (kèm icon Thư)
                ProfileGrayInputBox(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                )

                // Số điện thoại (kèm cờ Việt Nam 🇻🇳)
                ProfileGrayInputBox(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "Số điện thoại nhận hàng",
                    keyboardType = KeyboardType.Phone,
                    leadingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(text = "🇻🇳", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+84",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                )

                // Giới tính (Dropdown)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF6F7FB),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { isGenderMenuOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = gender.ifBlank { "Giới tính" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (gender.isBlank()) Color(0xFF9CA3AF) else Color(0xFF111827),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF4B5563)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isGenderMenuOpen,
                        onDismissRequest = { isGenderMenuOpen = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        listOf("Nam", "Nữ", "Khác").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    gender = option
                                    isGenderMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Phần Địa chỉ nhận hàng mặc định (Lưu tự động vào Sổ địa chỉ)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ĐỊA CHỈ NHẬN HÀNG MẶC ĐỊNH",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6B7280)
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileGrayInputBox(
                        value = province,
                        onValueChange = { province = it },
                        placeholder = "Tỉnh / Thành phố",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileGrayInputBox(
                        value = ward,
                        onValueChange = { ward = it },
                        placeholder = "Phường / Xã",
                        modifier = Modifier.weight(1f)
                    )
                }

                ProfileGrayInputBox(
                    value = addressDetail,
                    onValueChange = { addressDetail = it },
                    placeholder = "Số nhà, tên đường, thôn/xóm...",
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // BOTTOM SHEET CHỌN 1 TRONG 8 AVATAR TÀI NGUYÊN CÓ SẴN TRÊN GITHUB
    if (isAvatarPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { isAvatarPickerOpen = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Chọn ảnh đại diện có sẵn",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF111827)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chọn 1 trong ${AssetConfig.PRESET_AVATARS.size} mẫu ảnh đại diện SoleSense",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    itemsIndexed(AssetConfig.PRESET_AVATARS) { index, url ->
                        val isChosen = selectedAvatarUrl == url
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .border(
                                    width = if (isChosen) 3.dp else 1.dp,
                                    color = if (isChosen) Color(0xFF16A34A) else Color(0xFFE5E7EB),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedAvatarUrl = url
                                    isAvatarPickerOpen = false

                                    // Cập nhật ngay lập tức: UI local, StateFlow toàn app, Firestore và Firebase Auth
                                    scope.launch {
                                        UserSessionManager.updateAvatar(url).onSuccess {
                                            Toast.makeText(
                                                context,
                                                "Đã cập nhật ảnh đại diện!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { e ->
                                            Toast.makeText(
                                                context,
                                                "Lỗi đồng bộ ảnh đại diện: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Avatar ${index + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            if (isChosen) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileGrayInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF6F7FB))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF111827),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}
