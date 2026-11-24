const express = require('express');
const { handlePushRequest } = require('./lib/pushHandler');

const app = express();
app.use(express.json({ limit: '256kb' }));

app.get('/health', (_req, res) => {
  res.status(200).json({ status: 'ok', service: 'solesense-push-relay' });
});

app.post('/api/push', handlePushRequest);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`SoleSense Push Relay đang chạy tại cổng ${PORT}`);
});
