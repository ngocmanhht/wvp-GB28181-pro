import defaultSettings from '@/settings'

const title = defaultSettings.title || 'WVPVideo platform'

export default function getPageTitle(pageTitle) {
  if (pageTitle) {
    return `${pageTitle} - ${title}`
  }
  return `${title}`
}
