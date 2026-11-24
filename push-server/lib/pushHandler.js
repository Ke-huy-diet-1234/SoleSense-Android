const admin = require('firebase-admin');

/**
 * Khởi tạo Firebase Admin SDK một lần duy nhất (Singleton) từ biến môi trường:
 * - FIREBASE_SERVICE_ACCOUNT_BASE64 (khuyên dùng trên Vercel/Render để tránh lỗi xuống dòng \n)
 * - hoặc FIREBASE_SERVICE_ACCOUNT_JSON (chuỗi JSON thô của file service-account.json)
 */
function ensureFirebaseAdmin() {
  if (admin.apps.length > 0) return admin;

  const base64 = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64;
  const rawJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;

  let serviceAccount = null;
  if (base64 && base64.trim()) {
    const decoded = Buffer.from(base64.trim(), 'base64').toString('utf8');
    serviceAccount = JSON.parse(decoded);
  } else if (rawJson && rawJson.trim()) {
    serviceAccount = JSON.parse(rawJson.trim());
  } else {
    throw new Error(
      'Thiếu biến môi trường FIREBASE_SERVICE_ACCOUNT_BASE64 hoặc FIREBASE_SERVICE_ACCOUNT_JSON'
    );
  }

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
  return admin;
}

const ALLOWED_TOPICS = new Set(['all_customers', 'role_support', 'role_admin']);

/**
 * Chuẩn hóa toàn bộ trường trong FCM data payload thành String (yêu cầu bắt buộc của FCM HTTP v1).
 */
function buildStringDataPayload(body) {
  const data = {
    title: String(body.title || 'SoleSense').slice(0, 120),
    body: String(body.body || '').slice(0, 400),
    type: String(body.type || 'SYSTEM'),
    targetRole: String(body.targetRole || ''),
    channelId: String(body.channelId || ''),
    deepLinkRoute: String(body.deepLinkRoute || ''),
    referenceId: String(body.referenceId || ''),
    chatId: String(body.chatId || ''),
    notificationId: String(body.notificationId || ''),
  };
  return data;
}

/**
 * Kiểm tra quyền gửi Push theo Ma trận phân quyền 3 tác nhân (USER / SUPPORT / ADMIN)
 * nhằm ngăn chặn tài khoản khách hàng lạm dụng API gửi spam toàn sàn.
 */
async function authorizeDispatch({ callerUid, callerRole, targetUserId, topic, type, db }) {
  const role = String(callerRole || 'CUSTOMER').toUpperCase();
  const isAdmin = role === 'ADMIN';
  const isStaff = role === 'ADMIN' || role === 'SUPPORT';

  // 1. Gửi theo Topic
  if (topic) {
    if (!ALLOWED_TOPICS.has(topic)) {
      return { allowed: false, reason: `Topic không hợp lệ: ${topic}` };
    }
    // Chỉ ADMIN mới được gửi chiến dịch quảng bá tới toàn bộ khách hàng (`all_customers`)
    if (topic === 'all_customers' && !isAdmin) {
      return { allowed: false, reason: 'Chỉ ADMIN mới được phát hành thông báo tới toàn bộ khách hàng.' };
    }
    // Mọi tài khoản hợp lệ đều có thể kích hoạt sự kiện báo cho `role_admin` (đặt/hủy đơn) hoặc `role_support` (gặp nhân viên)
    return { allowed: true };
  }

  // 2. Gửi đích danh 1 người dùng (`targetUserId`)
  if (targetUserId) {
    if (isStaff || callerUid === targetUserId) {
      return { allowed: true };
    }
    // Khách hàng được phép đẩy tin nhắn chat tới đúng nhân viên CSKH đang phụ trách phòng chat của mình
    if (type === 'SUPPORT_CHAT') {
      const chatSnap = await db.collection('chats').doc(`chat_${callerUid}`).get();
      if (chatSnap.exists && chatSnap.data()?.assignedStaffId === targetUserId) {
        return { allowed: true };
      }
    }
    return { allowed: false, reason: 'Không có quyền gửi thông báo tới tài khoản người dùng khác.' };
  }

  return { allowed: false, reason: 'Cần cung cấp targetUserId hoặc topic.' };
}

/**
 * Xử lý HTTP POST /api/push (dùng chung cho cả Vercel Serverless và Express/Render).
 */
async function handlePushRequest(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  try {
    const firebaseAdmin = ensureFirebaseAdmin();
    const db = firebaseAdmin.firestore();

    // 1. Xác thực Firebase Auth ID Token từ header Authorization: Bearer <token>
    const authHeader = req.headers.authorization || '';
    const idToken = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
    if (!idToken) {
      return res.status(401).json({ error: 'Thiếu Firebase Authorization Bearer token.' });
    }

    let decodedToken;
    try {
      decodedToken = await firebaseAdmin.auth().verifyIdToken(idToken);
    } catch (err) {
      return res.status(401).json({ error: 'Firebase ID Token không hợp lệ hoặc đã hết hạn.' });
    }

    const callerUid = decodedToken.uid;
    const callerSnap = await db.collection('users').doc(callerUid).get();
    const callerData = callerSnap.exists ? callerSnap.data() : {};
    if (String(callerData.status || 'ACTIVE').toUpperCase() === 'BLOCKED') {
      return res.status(403).json({ error: 'Tài khoản đã bị khóa.' });
    }
    const callerRole = String(callerData.role || 'CUSTOMER').toUpperCase();

    const body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
    const targetUserId = body.targetUserId ? String(body.targetUserId).trim() : null;
    const topic = body.topic ? String(body.topic).trim() : null;
    const type = String(body.type || 'SYSTEM');

    // 2. Kiểm tra phân quyền gửi
    const authz = await authorizeDispatch({
      callerUid,
      callerRole,
      targetUserId,
      topic,
      type,
      db,
    });
    if (!authz.allowed) {
      return res.status(403).json({ error: authz.reason });
    }

    const dataPayload = buildStringDataPayload(body);

    // 3A. Gửi theo Topic (role_admin / role_support / all_customers) bằng Data-Only High Priority
    // Giúp SoleSenseMessagingService.onMessageReceived() luôn được gọi kể cả khi app đang tắt!
    if (topic) {
      const messageId = await firebaseAdmin.messaging().send({
        topic,
        data: dataPayload,
        android: {
          priority: 'high',
          ttl: 24 * 60 * 60 * 1000, // 24 giờ
        },
      });
      return res.status(200).json({ ok: true, mode: 'topic', topic, messageId });
    }

    // 3B. Gửi đích danh toàn bộ thiết bị (fcmTokens) của 1 tài khoản
    const targetSnap = await db.collection('users').doc(targetUserId).get();
    if (!targetSnap.exists) {
      return res.status(200).json({ ok: true, skipped: true, reason: 'Không tìm thấy user đích' });
    }

    const targetData = targetSnap.data() || {};
    const tokenSet = new Set();
    if (Array.isArray(targetData.fcmTokens)) {
      targetData.fcmTokens.forEach((t) => {
        if (typeof t === 'string' && t.trim()) tokenSet.add(t.trim());
      });
    }
    if (typeof targetData.fcmToken === 'string' && targetData.fcmToken.trim()) {
      tokenSet.add(targetData.fcmToken.trim());
    }

    const tokens = Array.from(tokenSet);
    if (tokens.length === 0) {
      return res.status(200).json({ ok: true, skipped: true, reason: 'Người nhận chưa có FCM token' });
    }

    const multicastResponse = await firebaseAdmin.messaging().sendEachForMulticast({
      tokens,
      data: dataPayload,
      android: {
        priority: 'high',
        ttl: 24 * 60 * 60 * 1000,
      },
    });

    // Tự động dọn dẹp các FCM token đã chết / gỡ app khỏi Firestore
    const staleTokens = [];
    multicastResponse.responses.forEach((resp, idx) => {
      if (!resp.success) {
        const code = resp.error?.code || '';
        if (
          code === 'messaging/registration-token-not-registered' ||
          code === 'messaging/invalid-registration-token'
        ) {
          staleTokens.push(tokens[idx]);
        }
      }
    });

    if (staleTokens.length > 0) {
      try {
        await db.collection('users').doc(targetUserId).update({
          fcmTokens: admin.firestore.FieldValue.arrayRemove(...staleTokens),
        });
      } catch (_) {}
    }

    return res.status(200).json({
      ok: true,
      mode: 'multicast',
      targetUserId,
      successCount: multicastResponse.successCount,
      failureCount: multicastResponse.failureCount,
      cleanedStaleTokens: staleTokens.length,
    });
  } catch (err) {
    console.error('[SoleSense Push Relay Error]', err);
    return res.status(500).json({ error: err.message || 'Internal Server Error' });
  }
}

module.exports = { handlePushRequest };
