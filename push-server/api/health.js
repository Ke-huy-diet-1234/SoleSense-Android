module.exports = function handler(_req, res) {
  const hasBase64 = Boolean(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64 && process.env.FIREBASE_SERVICE_ACCOUNT_BASE64.trim());
  const hasRaw = Boolean(process.env.FIREBASE_SERVICE_ACCOUNT_JSON && process.env.FIREBASE_SERVICE_ACCOUNT_JSON.trim());
  res.status(200).json({
    status: 'ok',
    service: 'solesense-push-relay',
    firebaseConfigured: hasBase64 || hasRaw,
    endpoint: '/api/push',
  });
};
