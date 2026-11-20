package com.example.solesense.ui.admin.voucher

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.order.Voucher
import com.example.solesense.repository.VoucherRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVoucherManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voucherRepository = remember { VoucherRepository() }

    var vouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf("Tất cả") }

    var isCreateVoucherDialogOpen by remember { mutableStateOf(false) }
    var selectedVoucherForEdit by remember { mutableStateOf<Voucher?>(null) }
    var selectedVoucherForDetail by remember { mutableStateOf<Voucher?>(null) }

    fun refreshVouchers() {
        scope.launch {
            isLoading = true
            voucherRepository.getAllVouchers()
                .onSuccess { list ->
                    vouchers = list
                }
                .onFailure {
                    Toast.makeText(context, "Không thể tải danh sách voucher", Toast.LENGTH_SHORT).show()
                }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshVouchers()
    }

    val filterTabs = listOf("Tất cả", "Đang hoạt động", "Chưa bắt đầu", "Đã hết hạn", "Đã tắt")

    // Lọc danh sách theo Tab trạng thái và từ khóa tìm kiếm
    val filteredVouchers = remember(vouchers, searchQuery, selectedFilterTab) {
        vouchers.filter { voucher ->
            val status = voucher.calculateStatus()
            val matchesTab = when (selectedFilterTab) {
                "Đang hoạt động" -> status == "ACTIVE"
                "Chưa bắt đầu" -> status == "SCHEDULED"
                "Đã hết hạn" -> status == "EXPIRED" || status == "EXHAUSTED"
                "Đã tắt" -> status == "DISABLED"
                else -> true // "Tất cả"
            }

            val query = searchQuery.trim().lowercase()
            val matchesQuery = query.isEmpty() ||
                voucher.code.lowercase().contains(query) ||
                voucher.name.lowercase().contains(query) ||
                (voucher.description?.lowercase()?.contains(query) == true)

            matchesTab && matchesQuery
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FD),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quản lý Voucher",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = Color(0xFF111111)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Nút "+ Tạo voucher" (Nút tròn dài màu đen, không bao giờ rớt dòng)
                    Button(
                        onClick = { isCreateVoucherDialogOpen = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1A),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tạo voucher",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            softWrap = false
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
        ) {
            // 1. THANH TÌM KIẾM THEO MÃ HOẶC TÊN (Ảnh mẫu 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(23.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF111111),
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Tìm theo mã hoặc tên voucher",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF9E9E9E),
                                            fontSize = 13.5.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            // 2. DÃY TAB LỌC TRẠNG THÁI: Tất cả | Đang hoạt động | Chưa bắt đầu | Đã hết hạn | Đã tắt
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterTabs) { tab ->
                    val isSelected = selectedFilterTab == tab
                    val bg = if (isSelected) Color(0xFFEDE9FE) else Color.White
                    val textColor = if (isSelected) Color(0xFF6D28D9) else Color(0xFF6B7280)
                    val border = if (isSelected) BorderStroke(1.dp, Color(0xFFC4B5FD)) else BorderStroke(1.dp, Color(0xFFE5E7EB))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = bg,
                        border = border,
                        modifier = Modifier.clickable { selectedFilterTab = tab }
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // 3. DANH SÁCH THẺ VOUCHER (ĐỐI CHIẾU CHÍNH XÁC VỚI ẢNH MẪU 1)
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1A1A1A))
                }
            } else if (filteredVouchers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Không tìm thấy voucher phù hợp",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredVouchers, key = { it.voucherId }) { voucher ->
                        AdminVoucherCard(
                            voucher = voucher,
                            onDetailClick = { selectedVoucherForDetail = voucher },
                            onEditClick = { selectedVoucherForEdit = voucher },
                            onToggleStatus = { isChecked ->
                                // Cập nhật Optimistic UI ngay lập tức để Switch gạt mượt và đổi trạng thái tức thì
                                vouchers = vouchers.map {
                                    if (it.voucherId == voucher.voucherId) it.copy(isEnabled = isChecked) else it
                                }
                                scope.launch {
                                    voucherRepository.toggleVoucherStatus(voucher.voucherId, isChecked)
                                        .onSuccess {
                                            Toast.makeText(
                                                context,
                                                if (isChecked) "Đã kích hoạt voucher ${voucher.code}" else "Đã tắt voucher ${voucher.code}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .onFailure {
                                            refreshVouchers()
                                            Toast.makeText(context, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal tạo Voucher mới (Chuẩn 5 nhóm quy trình Admin)
    if (isCreateVoucherDialogOpen) {
        CreateVoucherDialog(
            initialVoucher = null,
            onDismiss = { isCreateVoucherDialogOpen = false },
            onVoucherCreated = {
                isCreateVoucherDialogOpen = false
                refreshVouchers()
                Toast.makeText(context, "Tạo voucher mới thành công!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal chỉnh sửa Voucher đã có
    if (selectedVoucherForEdit != null) {
        CreateVoucherDialog(
            initialVoucher = selectedVoucherForEdit,
            onDismiss = { selectedVoucherForEdit = null },
            onVoucherCreated = {
                selectedVoucherForEdit = null
                refreshVouchers()
                Toast.makeText(context, "Cập nhật voucher thành công!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal xem chi tiết Voucher
    if (selectedVoucherForDetail != null) {
        VoucherDetailDialog(
            voucher = selectedVoucherForDetail!!,
            onDismiss = { selectedVoucherForDetail = null }
        )
    }
}

/**
 * Thẻ hiển thị Voucher trong Admin (Sao y bản chính Ảnh mẫu 1):
 * - Vạch xanh lá bên trái
 * - Header: Mã SOLE200 + Tag trạng thái "• Đang hoạt động"
 * - Dòng to: 200.000đ (Màu xanh lá đậm)
 * - Mô tả: Giảm trực tiếp - Đơn tối thiểu 2.000.000đ
 * - Icon Lịch: Đã sử dụng 45 / 100
 * - Icon Đồng hồ: Thời hạn 01/11 – 30/11/2026
 * - Nút Chi tiết & Nút Sửa / Bật tắt
 */
@Composable
private fun AdminVoucherCard(
    voucher: Voucher,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val status = voucher.calculateStatus()

    val (statusLabel, statusColor, statusBg, stripColor) = when (status) {
        "ACTIVE" -> Quadruple("• Đang hoạt động", Color(0xFF16A34A), Color(0xFFDCFCE7), Color(0xFF22C55E))
        "SCHEDULED" -> Quadruple("• Chưa bắt đầu", Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFFF59E0B))
        "EXPIRED" -> Quadruple("• Đã hết hạn", Color(0xFFDC2626), Color(0xFFFEE2E2), Color(0xFFEF4444))
        "EXHAUSTED" -> Quadruple("• Đã hết lượt", Color(0xFF9333EA), Color(0xFFF3E8FF), Color(0xFFA855F7))
        else -> Quadruple("• Đã tắt", Color(0xFF6B7280), Color(0xFFF3F4F6), Color(0xFF9CA3AF))
    }

    val displayAmountText = when (voucher.discountType) {
        "PERCENT" -> "${voucher.discountValue.toInt()}%"
        "FREE_SHIPPING" -> "FREESHIP ${numberFormat.format(voucher.discountValue.toLong())}đ"
        else -> "${numberFormat.format(voucher.discountValue.toLong())}đ"
    }

    val subtext = if (voucher.minOrderValue > 0) {
        "Giảm trực tiếp - Đơn tối thiểu ${numberFormat.format(voucher.minOrderValue.toLong())}đ"
    } else {
        "Giảm trực tiếp - Áp dụng mọi đơn hàng"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Vạch màu nhấn bên trái thẻ (Xanh lá / Vàng / Đỏ / Xám)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // HÀNG 1: MÃ VOUCHER + TAG TRẠNG THÁI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = voucher.code,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFF111111)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusBg)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // HÀNG 2: MỨC GIẢM CHÍNH (TO, ĐẬM MÀU XANH)
                Text(
                    text = displayAmountText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color(0xFF15803D)
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // HÀNG 3: MÔ TẢ ĐIỀU KIỆN
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 12.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // HÀNG 4: ICON ĐÃ SỬ DỤNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đã sử dụng",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Text(
                        text = "${voucher.usedCount} / ${if (voucher.usageLimit > 0) voucher.usageLimit else "∞"}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            fontSize = 13.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HÀNG 5: ICON THỜI HẠN
                val startStr = dateFormat.format(voucher.startAt.toDate())
                val endStr = dateFormat.format(voucher.endAt.toDate())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Thời hạn",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Text(
                        text = "$startStr – $endStr",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151),
                            fontSize = 12.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // HÀNG 6: CÁC NÚT THAO TÁC (Chi tiết / Sửa / Bật tắt)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cụm nút bấm "Chi tiết" và "Sửa"
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDetailClick,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Chi tiết",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151),
                                    fontSize = 11.5.sp
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sửa",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151),
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    // Công tắc Bật/Tắt Voucher
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (voucher.isEnabled) "Kích hoạt" else "Tắt",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (voucher.isEnabled) Color(0xFF16A34A) else Color(0xFF9CA3AF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = voucher.isEnabled,
                            onCheckedChange = onToggleStatus,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hộp thoại Tạo mới hoặc Chỉnh sửa Voucher (5 NHÓM ĐẶC TẢ CHI TIẾT):
 * 1. Nhóm cơ bản: Mã, Tên, Mô tả
 * 2. Nhóm hình thức: Giảm tiền cố định, Phần trăm, Miễn phí vận chuyển, Giảm tối đa
 * 3. Nhóm điều kiện: Đơn tối thiểu, Phạm vi áp dụng (Thương hiệu, Danh mục, Toàn sàn), Đối tượng, Số lượt mỗi người, Tổng lượt
 * 4. Nhóm thời gian: Thời hạn hiệu lực
 * 5. Xem trước & Xác nhận lưu Firestore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateVoucherDialog(
    initialVoucher: Voucher? = null,
    onDismiss: () -> Unit,
    onVoucherCreated: () -> Unit
) {
    val isEditMode = initialVoucher != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voucherRepository = remember { VoucherRepository() }

    var code by remember { mutableStateOf(initialVoucher?.code ?: "") }
    var name by remember { mutableStateOf(initialVoucher?.name ?: "") }
    var description by remember { mutableStateOf(initialVoucher?.description ?: "") }

    var discountType by remember { mutableStateOf(initialVoucher?.discountType ?: "FIXED") }
    var discountValueText by remember { mutableStateOf(initialVoucher?.discountValue?.toLong()?.toString() ?: "200000") }
    var maxDiscountText by remember { mutableStateOf(initialVoucher?.maxDiscountAmount?.toLong()?.toString() ?: "300000") }

    var minOrderValueText by remember { mutableStateOf(initialVoucher?.minOrderValue?.toLong()?.toString() ?: "2000000") }
    var scopeType by remember { mutableStateOf(initialVoucher?.scope ?: "ALL") }
    var selectedBrand by remember { mutableStateOf(initialVoucher?.applicableBrandIds?.firstOrNull() ?: "nike") }
    var selectedCategory by remember { mutableStateOf(initialVoucher?.applicableCategoryIds?.firstOrNull() ?: "running") }

    var targetAudience by remember { mutableStateOf(initialVoucher?.targetAudience ?: "ALL") }
    var perUserLimitText by remember { mutableStateOf(initialVoucher?.perUserLimit?.toString() ?: "1") }
    var usageLimitText by remember { mutableStateOf(initialVoucher?.usageLimit?.toString() ?: "100") }

    val initialDaysRemaining = remember(initialVoucher) {
        if (initialVoucher != null) {
            val diffSec = initialVoucher.endAt.seconds - Timestamp.now().seconds
            ((diffSec / 86400L).toInt()).coerceAtLeast(1).toString()
        } else "30"
    }
    var durationDaysText by remember { mutableStateOf(initialDaysRemaining) }
    var isEnabledState by remember { mutableStateOf(initialVoucher?.isEnabled ?: true) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Chỉnh sửa Voucher" else "Tạo voucher mới",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // NHÓM 1: THÔNG TIN CƠ BẢN
                Text(text = "1. Thông tin cơ bản", fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9), fontSize = 13.sp)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Mã voucher (VD: SOLE200)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên voucher (VD: Khuyến mãi mùa hè)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // NHÓM 2: HÌNH THỨC GIẢM GIÁ (Đã fix dàn đều không rớt dòng)
                Text(text = "2. Hình thức giảm giá", fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9), fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = discountType == "FIXED",
                        onClick = { discountType = "FIXED" },
                        label = { Text("Tiền cố định", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = discountType == "PERCENT",
                        onClick = { discountType = "PERCENT" },
                        label = { Text("Giảm %", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterChip(
                    selected = discountType == "FREE_SHIPPING",
                    onClick = { discountType = "FREE_SHIPPING" },
                    label = { Text("Miễn phí vận chuyển (Freeship)", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = discountValueText,
                    onValueChange = { discountValueText = it },
                    label = { Text(if (discountType == "PERCENT") "Phần trăm giảm (VD: 10%)" else "Số tiền giảm (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (discountType == "PERCENT") {
                    OutlinedTextField(
                        value = maxDiscountText,
                        onValueChange = { maxDiscountText = it },
                        label = { Text("Mức giảm tối đa (VNĐ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // NHÓM 3: ĐIỀU KIỆN ÁP DỤNG (Đã fix lỗi Ảnh 22 chữ "Theo danh mục" bị ép dọc)
                Text(text = "3. Điều kiện áp dụng", fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9), fontSize = 13.sp)
                OutlinedTextField(
                    value = minOrderValueText,
                    onValueChange = { minOrderValueText = it },
                    label = { Text("Đơn hàng tối thiểu (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Phạm vi áp dụng:", fontSize = 12.sp, color = Color.Gray)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scopeType == "ALL",
                            onClick = { scopeType = "ALL" },
                            label = { Text("Toàn sàn", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = scopeType == "BRAND",
                            onClick = { scopeType = "BRAND" },
                            label = { Text("Theo hãng", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = scopeType == "CATEGORY",
                        onClick = { scopeType = "CATEGORY" },
                        label = { Text("Theo danh mục sản phẩm", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (scopeType == "BRAND") {
                    OutlinedTextField(
                        value = selectedBrand,
                        onValueChange = { selectedBrand = it.lowercase() },
                        label = { Text("Mã hãng (nike, adidas, asics, puma...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (scopeType == "CATEGORY") {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { selectedCategory = it.lowercase() },
                        label = { Text("Mã danh mục (running, lifestyle, basketball...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = perUserLimitText,
                        onValueChange = { perUserLimitText = it },
                        label = { Text("Lượt/người") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = usageLimitText,
                        onValueChange = { usageLimitText = it },
                        label = { Text("Tổng lượt") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // NHÓM 4: THỜI GIAN & TRẠNG THÁI
                Text(text = "4. Thời gian áp dụng", fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9), fontSize = 13.sp)
                OutlinedTextField(
                    value = durationDaysText,
                    onValueChange = { durationDaysText = it },
                    label = { Text("Thời hạn hiệu lực (Số ngày kể từ hôm nay)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Trạng thái kích hoạt:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isEnabledState,
                            onCheckedChange = { isEnabledState = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanCode = code.trim().uppercase()
                    if (cleanCode.isBlank()) {
                        Toast.makeText(context, "Mã voucher không được để trống", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val discountVal = discountValueText.toDoubleOrNull() ?: 0.0
                    val minOrderVal = minOrderValueText.toDoubleOrNull() ?: 0.0
                    val days = durationDaysText.toIntOrNull() ?: 30
                    val maxUsage = usageLimitText.toIntOrNull() ?: 100
                    val perUser = perUserLimitText.toIntOrNull() ?: 1

                    val now = Timestamp.now()
                    val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                    val endTimestamp = Timestamp(calendar.time)

                    val voucherPayload = Voucher(
                        voucherId = initialVoucher?.voucherId ?: "",
                        code = cleanCode,
                        name = name.ifBlank { "Voucher $cleanCode" },
                        description = description.ifBlank {
                            "Giảm ${if (discountType == "PERCENT") "${discountVal.toInt()}%" else "${discountVal.toLong()}đ"} cho đơn từ ${minOrderVal.toLong()}đ"
                        },
                        discountType = discountType,
                        discountValue = discountVal,
                        maxDiscountAmount = if (discountType == "PERCENT") maxDiscountText.toDoubleOrNull() else null,
                        minOrderValue = minOrderVal,
                        scope = scopeType,
                        applicableBrandIds = if (scopeType == "BRAND") listOf(selectedBrand) else emptyList(),
                        applicableCategoryIds = if (scopeType == "CATEGORY") listOf(selectedCategory) else emptyList(),
                        targetAudience = targetAudience,
                        usageLimit = maxUsage,
                        usedCount = initialVoucher?.usedCount ?: 0,
                        reservedCount = initialVoucher?.reservedCount ?: 0,
                        perUserLimit = perUser,
                        startAt = now,
                        endAt = endTimestamp,
                        isEnabled = isEnabledState,
                        createdAt = initialVoucher?.createdAt ?: now,
                        updatedAt = now
                    )

                    scope.launch {
                        isSubmitting = true
                        val result = if (isEditMode) {
                            voucherRepository.saveEditedVoucher(voucherPayload).map { voucherPayload.voucherId }
                        } else {
                            voucherRepository.createVoucher(voucherPayload)
                        }
                        result
                            .onSuccess {
                                onVoucherCreated()
                            }
                            .onFailure { err ->
                                Toast.makeText(context, err.message ?: "Lưu voucher thất bại", Toast.LENGTH_LONG).show()
                            }
                        isSubmitting = false
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text(
                    text = if (isSubmitting) "Đang lưu..." else if (isEditMode) "Lưu thay đổi" else "Xác nhận tạo voucher",
                    maxLines = 1,
                    softWrap = false
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}

/**
 * Hộp thoại xem chi tiết thông số Voucher
 */
@Composable
private fun VoucherDetailDialog(
    voucher: Voucher,
    onDismiss: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chi tiết Voucher: ${voucher.code}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Tên: ${voucher.name}", fontWeight = FontWeight.SemiBold)
                Text(text = "Mô tả: ${voucher.description ?: "Không có"}")
                Text(text = "Hình thức: ${voucher.discountType} (Giá trị: ${voucher.discountValue})")
                Text(text = "Đơn tối thiểu: ${numberFormat.format(voucher.minOrderValue.toLong())}đ")
                Text(text = "Phạm vi: ${voucher.scope}")
                Text(text = "Đã sử dụng thành công: ${voucher.usedCount} lượt")
                Text(text = "Đang giữ chỗ (QR 5p): ${voucher.reservedCount} lượt")
                Text(text = "Tổng hạn mức: ${if (voucher.usageLimit > 0) "${voucher.usageLimit} lượt" else "Không giới hạn"}")
                Text(text = "Giới hạn mỗi khách: ${voucher.perUserLimit} lần")
                Text(text = "Bắt đầu: ${dateFormat.format(voucher.startAt.toDate())}")
                Text(text = "Kết thúc: ${dateFormat.format(voucher.endAt.toDate())}")
                Text(text = "Trạng thái: ${voucher.calculateStatus()}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
