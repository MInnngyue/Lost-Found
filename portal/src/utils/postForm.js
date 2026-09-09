function validateEventDate(rule, value, callback) {
  if (!value) return callback()
  const date = new Date(`${value}T00:00:00`)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const parts = String(value).split('-').map(Number)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || Number.isNaN(date.getTime()) ||
      date.getFullYear() !== parts[0] || date.getMonth() + 1 !== parts[1] ||
      date.getDate() !== parts[2] || date > today) {
    return callback(new Error('请选择有效日期，且不能晚于今天'))
  }
  callback()
}

export function disabledEventDate(date) {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date > today
}

export const postRules = {
  type: [{ required: true, type: 'enum', enum: [0, 1], message: '请选择类型', trigger: 'change' }],
  title: [
    { required: true, whitespace: true, message: '请输入帖子标题', trigger: 'blur' },
    { max: 128, message: '标题不能超过128个字符', trigger: 'blur' },
  ],
  itemCategory: [{ required: true, type: 'integer', min: 1, message: '请选择物品分类', trigger: 'change' }],
  color: [{ required: true, type: 'integer', min: 1, message: '请选择颜色', trigger: 'change' }],
  locationCampus: [{ required: true, type: 'integer', min: 1, message: '请选择校区', trigger: 'change' }],
  locationDetail: [{ max: 255, message: '具体地点不能超过255个字符', trigger: 'blur' }],
  description: [{ max: 10000, message: '描述不能超过10000个字符', trigger: 'blur' }],
  lostTime: [
    { required: true, message: '请选择时间', trigger: 'change' },
    { validator: validateEventDate, trigger: 'change' },
  ],
}
