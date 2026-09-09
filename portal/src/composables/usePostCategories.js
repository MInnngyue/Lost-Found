import { computed, ref } from 'vue'
import { getCategories } from '@/api/category'

export function usePostCategories(form) {
  const itemCategories = ref([])
  const colors = ref([])
  const campuses = ref([])
  const loading = ref(false)
  const error = ref('')
  const loaded = ref(false)
  const ready = computed(() => loaded.value && !loading.value && !error.value)
  const areas = computed(() =>
    campuses.value.find(campus => campus.id === form.value.locationCampus)?.children || [])
  const details = computed(() =>
    areas.value.find(area => area.id === form.value.locationArea)?.children || [])

  async function loadCategories() {
    if (loading.value) return
    loading.value = true
    error.value = ''
    itemCategories.value = []
    colors.value = []
    campuses.value = []
    try {
      const results = await Promise.all([
        getCategories('item_category'), getCategories('color'), getCategories('location'),
      ])
      itemCategories.value = results[0].data || []
      colors.value = results[1].data || []
      campuses.value = results[2].data || []
      for (const [field, options] of [
        ['itemCategory', itemCategories.value], ['color', colors.value],
        ['locationCampus', campuses.value],
      ]) {
        if (form.value[field] && !options.some(option => option.id === form.value[field])) {
          form.value[field] = ''
          if (field === 'locationCampus') onCampusChange()
        }
      }
      if (form.value.locationArea && !areas.value.some(area => area.id === form.value.locationArea)) {
        onCampusChange()
      }
      const missing = [
        !itemCategories.value.length && '物品分类',
        !colors.value.length && '颜色',
        !campuses.value.length && '校区',
      ].filter(Boolean)
      if (missing.length) error.value = `暂不能发布：${missing.join('、')}尚未配置，请联系管理员。`
      loaded.value = true
    } catch {
      error.value = '分类加载失败，请重试。'
      loaded.value = false
    } finally {
      loading.value = false
    }
  }

  function onCampusChange() {
    form.value.locationArea = ''
    form.value.locationDetail = ''
  }

  function onAreaChange() {
    form.value.locationDetail = ''
  }

  return { itemCategories, colors, campuses, areas, details, loading, error, ready,
    loadCategories, onCampusChange, onAreaChange }
}
