package com.example.solesense

import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.ProductBadge
import com.example.solesense.model.order.OrderItem
import com.example.solesense.model.order.Voucher
import com.example.solesense.model.support.Notification
import com.example.solesense.model.user.Address
import com.example.solesense.push.FcmTokenManager
import com.example.solesense.push.PushNotificationHelper
import com.example.solesense.ui.checkout.CheckoutSessionHolder
import com.example.solesense.utils.removeVietnameseAccents
import com.example.solesense.utils.toProductModel
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Bộ kiểm thử đơn vị (Unit Test) kiểm chứng tính đúng đắn của các quy tắc nghiệp vụ cốt lõi:
 * 1. Vòng đời trạng thái và công thức tính chiết khấu Voucher (FIXED, PERCENT, FREE_SHIPPING, giới hạn trần, giữ chỗ QR).
 * 2. Ánh xạ sản phẩm (ProductMapper), tính phần trăm giảm giá, phân loại nhãn (Badge) và chuẩn hóa tiếng Việt không dấu.
 * 3. Chuẩn hóa hiển thị địa chỉ nhận hàng đa dòng / đơn dòng chống trùng lặp.
 * 4. Quản lý phiên thanh toán tạm (CheckoutSessionHolder) khi xoay màn hình và khi đăng xuất / hoàn tất đơn.
 */
class SoleSenseBusinessLogicTest {

    // =========================================================================
    // 1. KIỂM THỬ NGHIỆP VỤ VOUCHER & CHIẾT KHẤU
    // =========================================================================

    @Test
    fun voucher_calculateStatus_returnsDisabledWhenNotEnabled() {
        val now = System.currentTimeMillis()
        val voucher = Voucher(
            voucherId = "v1",
            code = "SOLE200",
            isEnabled = false,
            startAt = Timestamp(Date(now - 60_000)),
            endAt = Timestamp(Date(now + 60_000)),
            usageLimit = 100,
            usedCount = 10
        )
        assertEquals("DISABLED", voucher.calculateStatus())
        assertFalse(voucher.isCurrentlyActive())
    }

    @Test
    fun voucher_calculateStatus_returnsScheduledWhenStartAtInFuture() {
        val now = System.currentTimeMillis()
        val voucher = Voucher(
            voucherId = "v2",
            code = "FUTURE50",
            isEnabled = true,
            startAt = Timestamp(Date(now + 3_600_000)),
            endAt = Timestamp(Date(now + 7_200_000))
        )
        assertEquals("SCHEDULED", voucher.calculateStatus())
    }

    @Test
    fun voucher_calculateStatus_returnsExpiredWhenEndAtInPast() {
        val now = System.currentTimeMillis()
        val voucher = Voucher(
            voucherId = "v3",
            code = "PAST50",
            isEnabled = true,
            startAt = Timestamp(Date(now - 7_200_000)),
            endAt = Timestamp(Date(now - 3_600_000))
        )
        assertEquals("EXPIRED", voucher.calculateStatus())
    }

    @Test
    fun voucher_calculateStatus_returnsExhaustedWhenUsedPlusReservedReachesLimit() {
        val now = System.currentTimeMillis()
        // Tổng giới hạn 10 lượt: đã dùng 8 lượt + đang giữ chỗ QR 2 lượt = 10 -> EXHAUSTED
        val voucher = Voucher(
            voucherId = "v4",
            code = "QRHOLD",
            isEnabled = true,
            startAt = Timestamp(Date(now - 60_000)),
            endAt = Timestamp(Date(now + 3_600_000)),
            usageLimit = 10,
            usedCount = 8,
            reservedCount = 2
        )
        assertEquals("EXHAUSTED", voucher.calculateStatus())
    }

    @Test
    fun voucher_calculateDiscountAmount_fixedAndPercentAndFreeShipping() {
        // 1. FIXED: Giảm cố định 200.000đ nhưng không vượt quá giá trị đơn hàng
        val fixedVoucher = Voucher(discountType = "FIXED", discountValue = 200_000.0)
        assertEquals(200_000.0, fixedVoucher.calculateDiscountAmount(1_500_000.0), 0.01)
        assertEquals(120_000.0, fixedVoucher.calculateDiscountAmount(120_000.0), 0.01)
        assertEquals(0.0, fixedVoucher.calculateDiscountAmount(0.0), 0.01)

        // 2. PERCENT có trần maxDiscountAmount = 300.000đ
        val percentVoucher = Voucher(
            discountType = "PERCENT",
            discountValue = 10.0,
            maxDiscountAmount = 300_000.0
        )
        // 10% của 2.000.000đ = 200.000đ (dưới trần 300.000đ)
        assertEquals(200_000.0, percentVoucher.calculateDiscountAmount(2_000_000.0), 0.01)
        // 10% của 4.500.000đ = 450.000đ -> bị chặn ở trần 300.000đ
        assertEquals(300_000.0, percentVoucher.calculateDiscountAmount(4_500_000.0), 0.01)

        // 3. FREE_SHIPPING
        val freeShipVoucher = Voucher(discountType = "FREE_SHIPPING", discountValue = 30_000.0)
        assertEquals(30_000.0, freeShipVoucher.calculateDiscountAmount(500_000.0), 0.01)
    }

    // =========================================================================
    // 2. KIỂM THỬ PRODUCT MAPPER & TÌM KIẾM TIẾNG VIỆT KHÔNG DẤU
    // =========================================================================

    @Test
    fun productMapper_mapsPricesBadgesAndOutOfStockAccurately() {
        val lowStockProduct = Product(
            productId = "p1",
            name = "Nike Pegasus 41",
            brandId = "nike",
            categoryId = "running",
            basePrice = 3_800_000.0,
            salePrice = 3_200_000.0,
            totalStockQuantity = 5,
            totalSoldCount = 900,
            status = "ACTIVE"
        )
        val model1 = lowStockProduct.toProductModel()
        assertEquals(3_200_000L, model1.currentPrice)
        assertEquals(3_800_000L, model1.originalPrice)
        assertEquals(15, model1.discountPercent)
        // Tồn kho 5 (1..18) ưu tiên cảnh báo LOW_STOCK
        assertEquals(ProductBadge.LOW_STOCK, model1.badge)
        assertFalse(model1.isOutOfStock)

        val outOfStockProduct = lowStockProduct.copy(
            totalStockQuantity = 0,
            status = "OUT_OF_STOCK"
        )
        val model2 = outOfStockProduct.toProductModel()
        assertTrue(model2.isOutOfStock)
    }

    @Test
    fun removeVietnameseAccents_stripsDiacriticsCorrectly() {
        val raw = "Giày chạy bộ Đen Trắng Đặc Biệt"
        assertEquals("Giay chay bo Den Trang Dac Biet", raw.removeVietnameseAccents())
    }

    // =========================================================================
    // 3. KIỂM THỬ ĐỊNH DẠNG ĐỊA CHỈ GIAO HÀNG
    // =========================================================================

    @Test
    fun address_toMultiLineAddress_preventsDuplicateSuffix() {
        val addr = Address(
            addressId = "a1",
            receiverName = "Tu Minh",
            phone = "0987654321",
            ward = "Xã Đông Anh",
            province = "Thành phố Hà Nội",
            detail = "Số nhà 83, Thôn Lại Đà"
        )
        assertEquals("Xã Đông Anh, Thành phố Hà Nội", addr.provinceAndWardText())
        assertEquals("Số nhà 83, Thôn Lại Đà\nXã Đông Anh, Thành phố Hà Nội", addr.toMultiLineAddress())
        assertEquals("Số nhà 83, Thôn Lại Đà, Xã Đông Anh, Thành phố Hà Nội", addr.toFullSingleLineAddress())
    }

    // =========================================================================
    // 4. KIỂM THỬ QUẢN LÝ PHIÊN CHECKOUT & THÔNG BÁO
    // =========================================================================

    @Test
    fun checkoutSessionHolder_preservesItemsUntilExplicitClear() {
        CheckoutSessionHolder.clearSession()
        assertTrue(CheckoutSessionHolder.consumeItems().isEmpty())

        val item = OrderItem(
            itemId = "item_1",
            productId = "prod_1",
            variantId = "var_1",
            productName = "Adidas Ultraboost 5",
            unitPrice = 2_450_000.0,
            quantity = 2,
            subtotal = 4_900_000.0
        )
        CheckoutSessionHolder.setDirectBuyItem(item)

        // Lần đọc 1 và lần đọc 2 (khi xoay màn hình) đều giữ nguyên danh sách
        assertEquals(1, CheckoutSessionHolder.consumeItems().size)
        assertEquals(1, CheckoutSessionHolder.consumeItems().size)

        // Sau khi clearSession (đặt đơn xong hoặc đăng xuất) thì danh sách trống
        CheckoutSessionHolder.clearSession()
        assertTrue(CheckoutSessionHolder.consumeItems().isEmpty())
    }

    @Test
    fun notification_displayContent_fallsBackToLegacyBody() {
        val notifWithBodyOnly = Notification(
            notificationId = "n1",
            title = "Thông báo hệ thống",
            content = "",
            body = "Nội dung từ trường body cũ"
        )
        assertEquals("Nội dung từ trường body cũ", notifWithBodyOnly.displayContent())
    }

    // =========================================================================
    // 5. KIỂM THỬ THÔNG BÁO MÀN HÌNH FCM CHO 3 TÁC NHÂN (USER / SUPPORT / ADMIN)
    // =========================================================================

    @Test
    fun fcmTokenManager_topicForRole_mapsAllThreeActorsCorrectly() {
        assertEquals(FcmTokenManager.TOPIC_ROLE_ADMIN, FcmTokenManager.topicForRole("ADMIN"))
        assertEquals(FcmTokenManager.TOPIC_ROLE_ADMIN, FcmTokenManager.topicForRole("admin"))
        assertEquals(FcmTokenManager.TOPIC_ROLE_SUPPORT, FcmTokenManager.topicForRole("SUPPORT"))
        assertEquals(FcmTokenManager.TOPIC_ALL_CUSTOMERS, FcmTokenManager.topicForRole("CUSTOMER"))
        assertEquals(FcmTokenManager.TOPIC_ALL_CUSTOMERS, FcmTokenManager.topicForRole(""))
    }

    @Test
    fun pushNotificationHelper_resolveChannelId_mapsAllThreeActorsCorrectly() {
        // USER channels
        assertEquals(PushNotificationHelper.CHANNEL_ORDERS, PushNotificationHelper.resolveChannelId("ORDER", "CUSTOMER"))
        assertEquals(PushNotificationHelper.CHANNEL_ORDERS, PushNotificationHelper.resolveChannelId("PAYMENT", "CUSTOMER"))
        assertEquals(PushNotificationHelper.CHANNEL_CHAT, PushNotificationHelper.resolveChannelId("CHAT", "CUSTOMER"))
        assertEquals(PushNotificationHelper.CHANNEL_PROMOTIONS, PushNotificationHelper.resolveChannelId("PROMOTION", "CUSTOMER"))

        // SUPPORT channels
        assertEquals(PushNotificationHelper.CHANNEL_SUPPORT_QUEUE, PushNotificationHelper.resolveChannelId("SUPPORT_WAITING", "SUPPORT"))
        assertEquals(PushNotificationHelper.CHANNEL_SUPPORT_CHAT, PushNotificationHelper.resolveChannelId("SUPPORT_CHAT", "SUPPORT"))

        // ADMIN channels
        assertEquals(PushNotificationHelper.CHANNEL_ADMIN_ORDERS, PushNotificationHelper.resolveChannelId("ADMIN_ORDER", "ADMIN"))
        assertEquals(PushNotificationHelper.CHANNEL_ADMIN_ALERTS, PushNotificationHelper.resolveChannelId("ADMIN_PRODUCT", "ADMIN"))
    }

    @Test
    fun pushNotificationHelper_buildDeepLinkRoute_mapsAllThreeActorsCorrectly() {
        // USER deep-links
        assertEquals("my_orders?orderId=ord_123", PushNotificationHelper.buildDeepLinkRoute("CUSTOMER", "ORDER", "ord_123"))
        assertEquals("product_detail/p_001", PushNotificationHelper.buildDeepLinkRoute("CUSTOMER", "PRODUCT_REVIEW", "p_001"))
        assertEquals("chat", PushNotificationHelper.buildDeepLinkRoute("CUSTOMER", "CHAT", "chat_u1"))
        assertEquals("explore", PushNotificationHelper.buildDeepLinkRoute("CUSTOMER", "PROMOTION", "camp_1"))

        // SUPPORT deep-links
        assertEquals("support_waiting", PushNotificationHelper.buildDeepLinkRoute("SUPPORT", "SUPPORT_WAITING", "req_1"))
        assertEquals("support_assigned", PushNotificationHelper.buildDeepLinkRoute("SUPPORT", "SUPPORT_CHAT", "req_1"))
        assertEquals("support_ops_returns", PushNotificationHelper.buildDeepLinkRoute("SUPPORT", "RETURN_REQUEST", "ord_1"))

        // ADMIN deep-links
        assertEquals("admin_order", PushNotificationHelper.buildDeepLinkRoute("ADMIN", "ADMIN_ORDER", "ord_1"))
        assertEquals("admin_product", PushNotificationHelper.buildDeepLinkRoute("ADMIN", "ADMIN_PRODUCT", "p_1"))
        assertEquals("admin_voucher", PushNotificationHelper.buildDeepLinkRoute("ADMIN", "ADMIN_VOUCHER", "v_1"))
        assertEquals("admin_ops_returns", PushNotificationHelper.buildDeepLinkRoute("ADMIN", "RETURN_REQUEST", "ord_1"))
    }
}
