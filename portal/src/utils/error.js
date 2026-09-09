const HTTP_ERROR_MESSAGES = {
  401: '未授权，请重新登录',
  403: '拒绝访问',
  404: '请求地址不存在',
  500: '服务器内部错误',
}

export function getErrorMessage(error, fallback = '请求失败，请稍后重试') {
  const responseMessage = error?.response?.data?.msg || error?.response?.data?.message
  if (responseMessage) return responseMessage

  const businessMessage = error?.msg || error?.message
  if (businessMessage && businessMessage !== 'Network Error') return businessMessage

  if (error?.request && !error?.response) return '无法连接服务器，请检查网络后重试'

  return HTTP_ERROR_MESSAGES[error?.response?.status] || fallback
}
