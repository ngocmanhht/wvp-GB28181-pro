// milliseconds in an hour
export const ONE_HOUR_STAMP = 60 * 60 * 1000
// Time resolution, that is, the time range represented by the entire timeline
export const ZOOM = [0.5, 1, 2, 6, 12, 24, 72, 360, 720, 8760, 87600]// Half an hour, 1 hour, 2 hours, 6 hours, 12 hours, 1 day, 3 days, 15 days, 30 days, 365 days、365*10day
// The number of hours per grid corresponding to the time resolution, that is, how many hours the smallest grid represents
export const ZOOM_HOUR_GRID = [1 / 60, 1 / 60, 2 / 60, 1 / 6, 0.25, 0.5, 1, 4, 4, 720, 7200]
export const MOBILE_ZOOM_HOUR_GRID = [
  1 / 20,
  1 / 30,
  1 / 20,
  1 / 3,
  0.5,
  2,
  4,
  4,
  4,
  720, 7200
]
// Time display judgment conditions corresponding to time resolution
export const ZOOM_DATE_SHOW_RULE = [
  () => { // Show all
    return true
  },
  date => { // Show every five minutes
    return date.getMinutes() % 5 === 0
  },
  date => { // Show every ten minutes
    return date.getMinutes() % 10 === 0
  },
  date => { // Hourly and half hour display
    return date.getMinutes() === 0 || date.getMinutes() === 30
  },
  date => { // Hourly display
    return date.getMinutes() === 0
  },
  date => { // Even hours on the hour
    return date.getHours() % 2 === 0 && date.getMinutes() === 0
  },
  date => { // every three hours
    return date.getHours() % 3 === 0 && date.getMinutes() === 0
  },
  date => { // every 12 hours
    return date.getHours() % 12 === 0 && date.getMinutes() === 0
  },
  date => { // Don't show anything
    return false
  },
  date => {
    return true
  },
  date => {
    return true
  }
]
export const MOBILE_ZOOM_DATE_SHOW_RULE = [
  () => { // Show all
    return true
  },
  date => { // Show every five minutes
    return date.getMinutes() % 5 === 0
  },
  date => { // Show every ten minutes
    return date.getMinutes() % 10 === 0
  },
  date => { // Hourly and half hour display
    return date.getMinutes() === 0 || date.getMinutes() === 30
  },
  date => { // Even hours on the hour
    return date.getHours() % 2 === 0 && date.getMinutes() === 0
  },
  date => { // Even hours on the hour
    return date.getHours() % 4 === 0 && date.getMinutes() === 0
  },
  date => { // every three hours
    return date.getHours() % 3 === 0 && date.getMinutes() === 0
  },
  date => { // every 12 hours
    return date.getHours() % 12 === 0 && date.getMinutes() === 0
  },
  date => { // Don't show anything
    return false
  },
  date => {
    return true
  },
  date => {
    return true
  }
]
