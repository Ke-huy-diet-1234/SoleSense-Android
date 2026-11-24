const { handlePushRequest } = require('../lib/pushHandler');

module.exports = async function handler(req, res) {
  return handlePushRequest(req, res);
};
