import request from './request'

function normalizeClaim(claim) {
  if (!claim) return claim
  return {
    ...claim,
    postType: claim.postType === 'LOST' ? 0 : claim.postType === 'FOUND' ? 1 : claim.postType,
    applicantConfirmed: claim.applicantConfirmed === true || claim.applicantConfirmed === 1 || claim.applicantConfirmed === '1',
    ownerConfirmed: claim.ownerConfirmed === true || claim.ownerConfirmed === 1 || claim.ownerConfirmed === '1',
  }
}

export function createClaim(postId, matchId, evidenceText) {
  return request.post('/lostfound/claim', { postId, matchId, evidenceText })
}

export function confirmClaim(id) {
  return request.put(`/lostfound/claim/${id}/confirm`)
}

export function cancelClaim(id) {
  return request.put(`/lostfound/claim/${id}/cancel`)
}

export function rejectClaim(id, reason) {
  return request.put(`/lostfound/claim/${id}/reject`, { ownerRemark: reason })
}

export function getClaimsByPost(postId) {
  return request.get(`/lostfound/claim/post/${postId}`).then((res) => ({
    ...res,
    data: (res.data || []).map(normalizeClaim),
  }))
}

/** 获取当前用户的认领进度列表 */
export function getMyClaims() {
  return request.get('/lostfound/claim/my').then((res) => ({
    ...res,
    data: (res.data || []).map(normalizeClaim),
  }))
}
