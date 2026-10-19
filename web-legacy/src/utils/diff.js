// utils/diff.js
// Returns the changed part of newObj relative to oldObj (new/Modify）。
// Optional includeRemoved=true When the removed field is returned, it is marked with removedValue.。
// Usage example： diff(oldObj, newObj) or diff(oldObj, newObj, { includeRemoved: true })

export function diff(oldObj, newObj, options = {}) {
  const { includeRemoved = false, removedValue = null, comparator } = options

  function isObject(v) {
    return v && typeof v === 'object' && !Array.isArray(v) && !(v instanceof Date)
  }
  function isDate(v) {
    return v instanceof Date
  }

  function equal(a, b) {
    if (typeof comparator === 'function') return comparator(a, b)
    if (isDate(a) && isDate(b)) return a.getTime() === b.getTime()
    if (Array.isArray(a) && Array.isArray(b)) {
      if (a.length !== b.length) return false
      for (let i = 0; i < a.length; i++) if (!equal(a[i], b[i])) return false
      return true
    }
    if (isObject(a) && isObject(b)) {
      const aKeys = Object.keys(a)
      const bKeys = Object.keys(b)
      if (aKeys.length !== bKeys.length) return false
      for (const k of aKeys) {
        if (!Object.prototype.hasOwnProperty.call(b, k) || !equal(a[k], b[k])) return false
      }
      return true
    }
    return a === b
  }

  function inner(o, n) {
    // Normalize undefined -> empty object for easier handling in object branch.
    const oIsObj = isObject(o)
    const nIsObj = isObject(n)

    if (equal(o, n)) return undefined

    if (oIsObj && nIsObj) {
      const result = {}
      const keys = new Set([...Object.keys(o || {}), ...Object.keys(n || {})])
      for (const k of keys) {
        console.log(k)
        const hasO = Object.prototype.hasOwnProperty.call(o || {}, k)
        const hasN = Object.prototype.hasOwnProperty.call(n || {}, k)

        if (hasN) {
          const sub = inner(hasO ? o[k] : undefined, n[k])
          if (sub !== undefined) result[k] = sub
        } else if (hasO && includeRemoved) {
          // key existed before but removed now
          result[k] = removedValue
        }
      }
      return Object.keys(result).length ? result : undefined
    }

    if (Array.isArray(o) && Array.isArray(n)) {
      return equal(o, n) ? undefined : n
    }

    if (isDate(n)) return n

    // Different primitive or type change -> return new value
    return n
  }

  const res = inner(oldObj ?? {}, newObj ?? {})
  return res === undefined ? {} : res
}

export default diff
