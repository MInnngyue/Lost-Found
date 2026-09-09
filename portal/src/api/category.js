import request from './request'

function normalizeType(type) {
  const types = { item_category: 'ITEM', color: 'COLOR', location: 'LOCATION' }
  return types[type] || type
}

function normalizeCategory(category) {
  return {
    ...category,
    id: category.categoryId,
    name: category.categoryName,
    children: (category.children || []).map(normalizeCategory),
  }
}

function flattenCategories(categories) {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children || [])])
}

export function getCategories(type) {
  // Campus, area and location are different category types in the same tree.
  return request.get('/lostfound/category/tree', {
    params: type === 'location' ? {} : { categoryType: normalizeType(type) },
  }).then((res) => ({
    ...res,
    data: (res.data || []).filter(category => type !== 'location' || category.categoryType === 'CAMPUS')
      .map(normalizeCategory),
  }))
}

export function getCategoryChildren(type, parentId) {
  return getCategories(type).then((res) => ({
    ...res,
    data: flattenCategories(res.data).find((category) => category.id === parentId)?.children || [],
  }))
}
