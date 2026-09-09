import { computed, reactive, ref } from 'vue'
import { getCaptcha, getUserInfo, login, register } from '@/api/user'
import { saveAuthSession } from '@/utils/authSession'
import { getErrorMessage } from '@/utils/error'

const MODES = {
  LOGIN: 'login',
  REGISTER: 'register',
}

const INITIAL_FORM = {
  username: '',
  password: '',
  confirmPassword: '',
  code: '',
}

function validateUsername(value) {
  const username = value.trim()
  if (!username) return '请输入用户名'
  if (username.length < 2 || username.length > 20) return '用户名长度为 2-20 个字符'
  return ''
}

function validatePassword(value) {
  if (!value) return '请输入密码'
  if (value.length < 5 || value.length > 20) return '密码长度为 5-20 个字符'
  return ''
}

export function useAuthForm() {
  const mode = ref(MODES.LOGIN)
  const form = reactive({ ...INITIAL_FORM })
  const captcha = reactive({ enabled: true, image: '', uuid: '', ready: false, loading: false, error: '' })
  const errors = reactive({})
  const loading = ref(false)
  const alert = ref(null)
  const isRegister = computed(() => mode.value === MODES.REGISTER)

  function clearErrors() {
    Object.keys(errors).forEach((field) => delete errors[field])
  }

  function clearFieldError(field) {
    delete errors[field]
    if (alert.value?.type === 'error') alert.value = null
  }

  function updateField(field, value) {
    if (!(field in form)) return
    form[field] = value
    clearFieldError(field)
    if (field === 'password' && errors.confirmPassword) delete errors.confirmPassword
  }

  function validateForm() {
    clearErrors()
    errors.username = validateUsername(form.username)
    errors.password = validatePassword(form.password)
    if (!captcha.ready) errors.code = '验证码未就绪，请刷新后重试'
    if (captcha.enabled && !form.code.trim()) errors.code = '请输入验证码'

    if (isRegister.value) {
      errors.confirmPassword = !form.confirmPassword
        ? '请再次输入密码'
        : form.confirmPassword !== form.password
          ? '两次输入的密码不一致'
          : ''
    }

    Object.keys(errors).forEach((field) => {
      if (!errors[field]) delete errors[field]
    })

    if (Object.keys(errors).length) {
      alert.value = { type: 'error', message: '请检查表单中的错误项' }
      return false
    }
    return true
  }

  async function refreshCaptcha() {
    if (captcha.loading) return
    captcha.loading = true
    captcha.ready = false
    captcha.error = ''
    captcha.image = ''
    captcha.uuid = ''
    form.code = ''
    try {
      const response = await getCaptcha()
      captcha.enabled = response.captchaEnabled !== false
      if (captcha.enabled && (!response.img || !response.uuid)) throw new Error('验证码响应不完整，请刷新')
      captcha.image = response.img ? `data:image/jpeg;base64,${response.img}` : ''
      captcha.uuid = response.uuid || ''
      captcha.ready = true
      delete errors.code
    } catch (error) {
      captcha.enabled = true
      captcha.error = getErrorMessage(error, '验证码加载失败，请刷新后重试')
    } finally {
      captcha.loading = false
    }
  }

  function resetForm() {
    Object.assign(form, INITIAL_FORM)
    clearErrors()
    alert.value = null
  }

  function switchMode() {
    if (loading.value) return
    mode.value = isRegister.value ? MODES.LOGIN : MODES.REGISTER
    resetForm()
    void refreshCaptcha()
  }

  async function submit() {
    if (loading.value || captcha.loading || !validateForm()) return null
    loading.value = true
    alert.value = null

    try {
      if (isRegister.value) {
        await register({
          username: form.username.trim(),
          password: form.password,
          code: form.code.trim(),
          uuid: captcha.uuid,
        })
        mode.value = MODES.LOGIN
        form.password = ''
        form.confirmPassword = ''
        await refreshCaptcha()
        alert.value = { type: 'success', message: '注册成功，请使用新账号登录' }
        return { type: 'registered' }
      }

      const response = await login({
        username: form.username.trim(),
        password: form.password,
        code: form.code.trim(),
        uuid: captcha.uuid,
      })
      const userResponse = await getUserInfo(response.data?.token)
      if (!userResponse.data?.userId) throw new Error('用户信息不完整，请重新登录')
      saveAuthSession({ ...response.data, ...userResponse.data })
      return { type: 'authenticated' }
    } catch (error) {
      alert.value = { type: 'error', message: getErrorMessage(error, '登录失败，请稍后重试') }
      if (captcha.enabled) await refreshCaptcha()
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    alert,
    captcha,
    errors,
    form,
    isRegister,
    loading,
    mode,
    clearFieldError,
    refreshCaptcha,
    submit,
    switchMode,
    updateField,
  }
}
