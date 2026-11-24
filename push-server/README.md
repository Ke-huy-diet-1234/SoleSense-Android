# 🚀 SoleSense Push Relay Server (Miễn phí 100% trên Firebase Spark Plan)

Server trung gian siêu nhẹ (~150 dòng Node.js) giúp ứng dụng Android **SoleSense** gửi **Thông báo màn hình khóa (FCM HTTP v1 API)** cho cả 3 tác nhân (`USER`, `SUPPORT`, `ADMIN`) kể cả khi đã vuốt tắt hẳn ứng dụng — mà **không cần nâng cấp gói Firebase Blaze**.

## 🔐 Kiến trúc bảo mật
1. App Android **không bao giờ** chứa `service-account.json`.
2. Mỗi request từ app gửi lên `/api/push` đều phải đính kèm `Authorization: Bearer <Firebase Auth ID Token>`.
3. Server xác thực chữ ký JWT của Firebase Auth, kiểm tra trạng thái `BLOCKED` và vai trò (`CUSTOMER` / `SUPPORT` / `ADMIN`) trực tiếp từ Firestore:
   - Chỉ `ADMIN` mới được đẩy thông báo quảng bá tới Topic `all_customers`.
   - Khách hàng chỉ được đẩy tin nhắn chat tới đúng nhân viên CSKH đang phụ trách phòng chat của mình (`assignedStaffId`).
   - Tự động xóa các FCM token đã gỡ app (`registration-token-not-registered`) khỏi Firestore.

---

## 🛠️ Hướng dẫn Deploy Miễn phí trong 3 Phút (Vercel hoặc Render)

### Bước 1: Lấy khóa Service Account trên Firebase Console (Gói Spark miễn phí)
1. Vào **Firebase Console** -> Chọn dự án **SoleSense** -> biểu tượng bánh răng **Project settings** -> tab **Service accounts**.
2. Bấm **"Generate new private key"** -> tải file `service-account.json` về máy tính *(TUYỆT ĐỐI KHÔNG push file này lên GitHub!)*.
3. Mã hóa file đó sang 1 dòng Base64 bằng PowerShell trên Windows:
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\duong\dan\service-account.json")) | Set-Clipboard
   ```
   *(Lệnh trên đã tự copy chuỗi Base64 vào Clipboard của bạn).*

### Bước 2A: Deploy lên **Vercel** (Khuyên dùng — Miễn phí & không bị ngủ đông lâu)
1. Cài CLI: `npm i -g vercel` (hoặc tạo dự án mới trên [vercel.com](https://vercel.com) trỏ Root Directory vào thư mục `push-server`).
2. Vào **Settings -> Environment Variables** trên Vercel, thêm biến:
   - Key: `FIREBASE_SERVICE_ACCOUNT_BASE64`
   - Value: *(Dán chuỗi Base64 ở Bước 1)*
3. Deploy xong bạn sẽ có đường dẫn dạng: `https://solesense-push.vercel.app`.

### Bước 2B: Hoặc Deploy lên **Render.com** (Free Web Service)
1. Tạo **New Web Service** trên [render.com](https://render.com), chọn Root Directory là `push-server`.
2. Build Command: `npm install`, Start Command: `npm start`.
3. Thêm Environment Variable `FIREBASE_SERVICE_ACCOUNT_BASE64` = *(chuỗi Base64 ở Bước 1)*.
4. Kiểm tra hoạt động: mở `https://<ten-service>.onrender.com/health` -> thấy `{"status":"ok"}`.

### Bước 3: Dán URL vào ứng dụng Android SoleSense
Mở file `gradle.properties` (hoặc `local.properties` ở thư mục gốc `d:\SoleSense\`), điền URL bạn vừa deploy:
```properties
PUSH_RELAY_URL=https://solesense-push.vercel.app
```
Build lại ứng dụng Android (`Sync Gradle` -> `Run`). Từ lúc này, toàn bộ 3 tác nhân **USER**, **SUPPORT** và **ADMIN** sẽ nhận thông báo màn hình khóa tức thì 100%!
