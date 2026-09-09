const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'

export function saveAuthSession(authData) {
  if (!authData?.token) {
    throw new Error('登录响应缺少认证信息，请稍后重试')
  }

  const { token, ...userInfo } = authData
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}
