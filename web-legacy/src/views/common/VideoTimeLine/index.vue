<template>
  <div
    ref="timeLineContainer"
    class="timeLineContainer"
    :style="{
      backgroundColor: backgroundColor,
    }"
    @touchstart="onTouchstart"
    @touchmove="onTouchmove"
    @mousedown="onMousedown"
    @mouseout="onMouseout"
    @mousemove="onMousemove"
    @mouseleave="onMouseleave"
  >
    <canvas
      ref="canvas"
      class="canvas"
      @mousewheel.stop.prevent="onMouseweel"
    />
    <div
      v-if="showWindowList && windowList && windowList.length > 1"
      ref="windowList"
      class="windowList"
      @scroll="onWindowListScroll"
    >
      <WindowListItem
        v-for="(item, index) in windowListInner"
        ref="WindowListItem"
        :key="index"
        :index="index"
        :data="item"
        :total-m-s="totalMS"
        :start-timestamp="startTimestamp"
        :width="width"
        :active="item.active"
        @click_window_timeSegments="triggerClickWindowTimeSegments"
        @click="toggleActive(index)"
      />
    </div>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import WindowListItem from './WindowListItem'
import {
  ONE_HOUR_STAMP,
  ZOOM,
  ZOOM_HOUR_GRID,
  ZOOM_DATE_SHOW_RULE,
  MOBILE_ZOOM_HOUR_GRID,
  MOBILE_ZOOM_DATE_SHOW_RULE
} from './constant'

/**
 * @Author: Wang Lin25
 * @Date: 2021-01-19 20:15:07
 * @Desc: Timeline component
 */
export default {
  name: 'TimeLine',
  components: {
    WindowListItem
  },
  props: {
    // Initial time, the time where the midpoint is located, defaults to 0 o'clock on the day
    initTime: {
      type: [Number, String],
      default: ''
    },
    // The time range to show the preview, i.e. the time range allowed by the middle tick
    /*
      {
        start: '2020-12-19 18:30:00',// Minimum time allowed to display
        end: '2021-01-20 10:0:00'// Maximum time allowed to display
      }
    */
    timeRange: {
      type: Object,
      default() {
        return {}
      }
    },
    // initial time resolution
    initZoomIndex: {
      type: Number,
      default: 5 // 24hours
    },
    // Whether to display the vertical line in the middle
    showCenterLine: {
      type: Boolean,
      default: true
    },
    // The style of the middle vertical line
    centerLineStyle: {
      type: Object,
      default() {
        return {
          width: 2,
          color: '#fff'
        }
      }
    },
    // Date time text color
    textColor: {
      type: String,
      default: 'rgba(151,158,167,1)'
    },
    // The color of the time text displayed when the mouse rolls over
    hoverTextColor: {
      type: String,
      default: 'rgb(194, 202, 215)'
    },
    // Timeline segment color
    lineColor: {
      type: String,
      default: 'rgba(151,158,167,1)'
    },
    // The ratio of the height of the timeline segment to the height of the timeline
    lineHeightRatio: {
      type: Object,
      default() {
        return {
          date: 0.3, // 0Date line segment height at point
          time: 0.2, // The height of the line segment showing time
          none: 0.1, // Line segment height without showing time
          hover: 0.3 // The height of the time period displayed when the mouse rolls over
        }
      }
    },
    // Whether to display the real-time time when the mouse rolls over
    showHoverTime: {
      type: Boolean,
      default: true
    },
    // Format mouseover time
    hoverTimeFormat: {
      type: Function
    },
    // The time color segment to be displayed
    /*
      {
        beginTime: new Date('2021-01-19 14:30:00').getTime(),// start timestamp
        endTime: new Date('2021-01-20 18:00:00').getTime(),// end timestamp
        color: '#FA3239',// color
        startRatio: 0.65,// The starting proportion of the height, i.e.top=Timeline height*startRatio
        endRatio: 0.9// The ending proportion of the height, i.e.bottom=Timeline height*endRatio
      }
    */
    timeSegments: {
      type: Array,
      default: () => {}
    },
    // Timeline background color
    backgroundColor: {
      type: String,
      default: '#262626'
    },
    // Whether to allow resolution switching
    enableZoom: {
      type: Boolean,
      default: true
    },
    // Whether to allow dragging
    enableDrag: {
      type: Boolean,
      default: true
    },
    // Window list. If the number of windows is greater than 1, you can configure this item. The corresponding number of timelines will be displayed. If there is only one window, please use the basic timeline directly.
    /*
      {
        timeSegments: [// time period
          {
            beginTime: new Date('2021-01-19 14:30:00').getTime(),// start timestamp
            endTime: new Date('2021-01-20 18:00:00').getTime(),// end timestamp
            color: '#FA3239',// color
            startRatio: 0.65,// The starting proportion of the height, i.e.top=Timeline height*startRatio
            endRatio: 0.9// The ending proportion of the height, i.e.bottom=Timeline height*endRatio
          }
        ],
        // Your other additional information...
      }
    */
    windowList: {
      type: Array,
      default() {
        return []
      }
    },
    // Base timeline height when windowList is displayed
    baseTimeLineHeight: {
      type: Number,
      default: 50
    },
    // Initial selected window timeline
    initSelectWindowTimeLineIndex: {
      type: Number,
      default: -1
    },
    // Is it a mobile phone version?
    isMobile: {
      type: Boolean,
      default: false
    },
    // If the distance between pressing and releasing the mouse is less than this value, it is considered a click event.
    maxClickDistance: {
      type: Number,
      default: 3
    },
    // When drawing time periods, the calculated coordinates are rounded to prevent gaps between connected time periods.
    roundWidthTimeSegments: {
      type: Boolean,
      default: true
    },
    // Customize which times are displayed
    customShowTime: {
      type: Function
    },
    // 0Whether to display the date at the point
    showDateAtZero: {
      type: Boolean,
      default: true
    },
    // Expand the ZOOM list. The data of this array will be appended to the internal ZOOM array. The corresponding zoomIndex can be accumulated later. There are 11 zooms in total. If you add an item, the corresponding zoomIndex is 11 because counting starts from zero.
    // Array type, each item of the array is：
    /*
      {
        zoom: 26,// Time resolution, the time range represented by the entire timeline, unit: hour
        zoomHourGrid: 0.5,// The number of hours per grid corresponding to the time resolution, that is, how many hours the smallest grid on the timeline represents
        mobileZoomHourGrid: 2, // The number of hours per grid corresponding to the time resolution in mobile phone mode. If you do not need to adapt to the mobile phone, you do not need to set it.
      }
    */
    // At the same time, you need to pass the customShowTime attribute to customize the time display, otherwise an error will be reported because there are only 11 built-in rules.
    extendZOOM: {
      type: Array,
      default() {
        return []
      }
    },
    // Format timeline to display time
    formatTime: {
      type: Function
    }
  },
  data() {
    return {
      width: 0,
      height: 0,
      ctx: null,
      currentZoomIndex: 0,
      currentTime: 0,
      startTimestamp: 0,
      mousedown: false,
      mousedownX: 0,
      mousedownY: 0,
      mousedownCacheStartTimestamp: 0,
      showWindowList: false,
      windowListInner: [],
      mousemoveX: -1,
      watchTimeList: []
    }
  },
  computed: {
    // The number of milliseconds represented by the entire timeline
    totalMS() {
      return ZOOM[this.currentZoomIndex] * ONE_HOUR_STAMP
    },
    // Timestamp representation of time range
    timeRangeTimestamp() {
      const t = {}
      if (this.timeRange.start) {
        t.start = typeof this.timeRange.start === 'number' ? this.timeRange.start : new Date(this.timeRange.start).getTime()
      }
      if (this.timeRange.end) {
        t.end = typeof this.timeRange.end === 'number' ? this.timeRange.end : new Date(this.timeRange.end).getTime()
      }
      return t
    },
    ACT_ZOOM_HOUR_GRID() {
      return this.isMobile ? MOBILE_ZOOM_HOUR_GRID : ZOOM_HOUR_GRID
    },
    ACT_ZOOM_DATE_SHOW_RULE() {
      return this.isMobile ? MOBILE_ZOOM_DATE_SHOW_RULE : ZOOM_DATE_SHOW_RULE
    },
    // Year and month mode
    yearMonthMode() {
      return this.currentZoomIndex === 9
    },
    // year pattern
    yearMode() {
      return this.currentZoomIndex === 10
    }
  },
  watch: {
    timeSegments: {
      deep: true,
      handler: 'reRender'
    }
  },
  created() {
    this.extendZOOM.forEach((item) => {
      ZOOM.push(item.zoom)
      ZOOM_HOUR_GRID.push(item.zoomHourGrid)
      MOBILE_ZOOM_HOUR_GRID.push(item.mobileZoomHourGrid)
    })
  },
  mounted() {
    this.setInitData()
    this.init()
    this.draw()
    this.onMouseup = this.onMouseup.bind(this)
    this.onResize = this.onResize.bind(this)
    this.onTouchend = this.onTouchend.bind(this)
    if (this.isMobile) {
      window.addEventListener('touchend', this.onTouchend)
    } else {
      window.addEventListener('mouseup', this.onMouseup)
    }
    window.addEventListener('resize', this.onResize)
  },
  beforeDestroy() {
    if (this.isMobile) {
      window.removeEventListener('touchend', this.onTouchend)
    } else {
      window.removeEventListener('mouseup', this.onMouseup)
    }
    window.removeEventListener('resize', this.onResize)
  },
  methods: {
    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-19 20:20:45
     * @Desc: Set initial data
     */
    setInitData() {
      // Internal window list data
      this.windowListInner = this.windowList.map((item, index) => {
        return {
          ...item,
          active: this.initSelectWindowTimeLineIndex === index
        }
      })
      // Must be set firstcurrentZoomIndex
      // initial time resolution
      this.currentZoomIndex =
        this.initZoomIndex >= 0 && this.initZoomIndex < ZOOM.length
          ? this.initZoomIndex
          : 5
      // initial current time
      this.startTimestamp =
        (this.initTime
          ? typeof this.initTime === 'number' ? this.initTime : new Date(this.initTime).getTime()
          : new Date(dayjs().format('YYYY-MM-DD 00:00:00')).getTime()) -
        this.totalMS / 2
      // Check and correct start time based on time range
      this.fixStartTimestamp()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 16:01:21
     * @Desc: Check and correct start time based on time range
     */
    fixStartTimestamp() {
      const hfms = this.totalMS / 2
      const ct = this.startTimestamp + hfms
      if (this.timeRangeTimestamp.start && ct < this.timeRangeTimestamp.start) {
        this.startTimestamp = this.timeRangeTimestamp.start - hfms
      }
      if (this.timeRangeTimestamp.end && ct > this.timeRangeTimestamp.end) {
        this.startTimestamp = this.timeRangeTimestamp.end - hfms
      }
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 09:20:22
     * @Desc: initialization
     */
    init() {
      const {
        width,
        height
      } = this.$refs.timeLineContainer.getBoundingClientRect()
      this.width = width
      this.height =
        this.windowList.length > 1 ? this.baseTimeLineHeight : height
      this.$refs.canvas.width = this.width
      this.$refs.canvas.height = this.height
      this.ctx = this.$refs.canvas.getContext('2d')
      this.showWindowList = true
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 09:27:18
     * @Desc: drawing method
     */
    draw() {
      // The order is very important, otherwise the level will be wrong.
      this.drawTimeSegments()
      this.addGraduations()
      this.drawMiddleLine()

      this.currentTime = this.startTimestamp + this.totalMS / 2
      this.$emit('timeChange', this.currentTime)

      // Notification window timeline rendering
      try {
        this.$refs.WindowListItem.forEach((item) => {
          item.draw()
        })
        // eslint-disable-next-line no-empty
      } catch (error) { }

      // Update the time position of the observation
      this.updateWatchTime()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-21 10:50:11
     * @Desc:  Update the time position of the observation
     */
    updateWatchTime() {
      this.watchTimeList.forEach((item) => {
        // Currently not within the display range
        if (item.time < this.startTimestamp || item.time > this.startTimestamp + this.totalMS) {
          item.callback(-1, -1)
        } else { // within range
          const x = (item.time - this.startTimestamp) * (this.width / this.totalMS)
          let y = 0
          const { left, top } = this.$refs.canvas.getBoundingClientRect()
          if (item.windowTimeLineIndex !== -1 && this.windowList.length > 1 && item.windowTimeLineIndex >= 0 && item.windowTimeLineIndex < this.windowList.length) {
            const rect = this.$refs.WindowListItem[item.windowTimeLineIndex].getRect()
            y = rect ? rect.top : top
          } else {
            y = top
          }
          item.callback(x + left, y)
        }
      })
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 09:27:46
     * @Desc: Draw the vertical line in the middle
     */
    drawMiddleLine() {
      if (!this.showCenterLine) {
        return
      }
      this.ctx.beginPath()
      const { width, color } = this.centerLineStyle
      const x = this.width / 2
      this.drawLine(x, 0, x, this.height, width, color)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 11:03:44
     * @Desc: Draw time scale
     */
    addGraduations() {
      this.ctx.beginPath()
      // The total number of grids that can be drawn
      const gridNum =
        ZOOM[this.currentZoomIndex] / this.ACT_ZOOM_HOUR_GRID[this.currentZoomIndex]
      // How many milliseconds per grid?
      const msPerGrid = this.ACT_ZOOM_HOUR_GRID[this.currentZoomIndex] * ONE_HOUR_STAMP
      // Each grid spacing, how many pixels wide is one grid?
      const pxPerGrid = this.width / gridNum
      // Starting offset distance
      const msOffset = msPerGrid - (this.startTimestamp % msPerGrid)
      const pxOffset = (msOffset / msPerGrid) * pxPerGrid
      for (let i = 0; i < gridNum; i++) {
        const currentStartTimestamp = this.startTimestamp + msOffset + i * msPerGrid
        let adjustMsOffset = 0
        // Resolution is in years
        if (this.yearMode) {
          adjustMsOffset = currentStartTimestamp - new Date(`${dayjs(currentStartTimestamp).format('YYYY')}-01-01 00:00:00`).getTime()
        } else if (this.yearMonthMode) {
          // Resolution is in months
          adjustMsOffset = currentStartTimestamp - new Date(`${dayjs(currentStartTimestamp).format('YYYY')}-${dayjs(currentStartTimestamp).format('MM')}-01 00:00:00`).getTime()
        }
        const x = pxOffset + i * pxPerGrid - (adjustMsOffset / msPerGrid) * pxPerGrid
        const graduationTime = currentStartTimestamp - adjustMsOffset
        let h = 0
        const date = new Date(graduationTime)
        // 0Click to show date
        if (this.showDateAtZero && date.getHours() === 0 && date.getMinutes() === 0) {
          h = this.height * (this.lineHeightRatio.date === undefined ? 0.3 : this.lineHeightRatio.date)
          this.ctx.fillStyle = this.textColor
          this.ctx.fillText(
            this.graduationTitle(graduationTime),
            x - 13,
            h + 15
          )
        } else if (this.checkShowTime(date)) {
          // The rest of the time is displayed according to respective rules
          h = this.height * (this.lineHeightRatio.time === undefined ? 0.2 : this.lineHeightRatio.time)
          this.ctx.fillStyle = this.textColor
          this.ctx.fillText(
            this.graduationTitle(graduationTime),
            x - 13,
            h + 15
          )
        } else {
          // Line segments without showing time
          h = this.height * (this.lineHeightRatio.none === undefined ? 0.1 : this.lineHeightRatio.none)
        }
        this.drawLine(x, 0, x, h, 1, this.lineColor)
      }
    },

    // Determine whether the time needs to be displayed
    checkShowTime(date) {
      if (this.customShowTime) {
        const res = this.customShowTime(date, this.currentZoomIndex)
        if (res === true) {
          return true
        } else if (res === false) {
          return false
        }
      }
      return this.ACT_ZOOM_DATE_SHOW_RULE[this.currentZoomIndex](date)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 15:42:49
     * @Desc: Draw time period
     */
    drawTimeSegments(callback, path) {
      const PX_PER_MS = this.width / this.totalMS // px/ms，Pixels per millisecond
      this.timeSegments.forEach((item) => {
        if (
          item.beginTime <= this.startTimestamp + this.totalMS
        ) {
          const hasEndTime = item.endTime >= this.startTimestamp
          this.ctx.beginPath()
          let x = (item.beginTime - this.startTimestamp) * PX_PER_MS
          let w
          if (x < 0) {
            x = 0
            w = hasEndTime ? (item.endTime - this.startTimestamp) * PX_PER_MS : 1
          } else {
            w = hasEndTime ? (item.endTime - item.beginTime) * PX_PER_MS : 1
          }
          const heightStartRatio = item.startRatio === undefined ? 0.6 : item.startRatio
          const heightEndRatio = item.endRatio === undefined ? 0.9 : item.endRatio
          if (this.roundWidthTimeSegments) {
            x = Math.round(x)
            w = Math.round(w)
          }
          // Avoid time periods smaller than 1px from being drawn.
          w = Math.max(1, w)
          if (path) {
            this.ctx.rect(
              x,
              this.height * heightStartRatio,
              w,
              this.height * (heightEndRatio - heightStartRatio)
            )
          } else {
            this.ctx.fillStyle = item.color
            this.ctx.fillRect(
              x,
              this.height * heightStartRatio,
              w,
              this.height * (heightEndRatio - heightStartRatio)
            )
          }
          callback && callback(item)
        }
      })
    },

    // touch start event
    onTouchstart(e) {
      if (!this.isMobile) {
        return
      }
      e = e.touches[0]
      this.onPointerdown(e)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 14:29:40
     * @Desc: mouse press event
     */
    onMousedown(e) {
      if (this.isMobile) {
        return
      }
      this.onPointerdown(e)
    },

    // press event
    onPointerdown(e) {
      const pos = this.getClientOffset(e)
      this.mousedownX = pos[0]
      this.mousedownY = pos[1]
      this.mousedown = true
      this.mousedownCacheStartTimestamp = this.startTimestamp
      this.$emit('mousedown', e)
    },

    // touch end event
    onTouchend(e) {
      if (!this.isMobile) {
        return
      }
      e = e.touches[0]
      this.onPointerup(e)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 14:38:30
     * @Desc: Release mouse
     */
    onMouseup(e) {
      if (this.isMobile) {
        return
      }
      this.onPointerup(e)
    },

    // Release event
    onPointerup(e) {
      // trigger click event
      const pos = this.getClientOffset(e)
      const reset = () => {
        this.mousedown = false
        this.mousedownX = 0
        this.mousedownY = 0
        this.mousedownCacheStartTimestamp = 0
      }
      if (
        Math.abs(pos[0] - this.mousedownX) <= this.maxClickDistance &&
        Math.abs(pos[1] - this.mousedownY) <= this.maxClickDistance
      ) {
        reset()
        this.onClick(...pos)
        return
      }
      if (this.mousedown && this.enableDrag) {
        reset()
        this.$emit('dragTimeChange', this.currentTime)
      } else {
        reset()
      }
      this.$emit('mouseup', e)
    },

    // touch move event
    onTouchmove(e) {
      if (!this.isMobile) {
        return
      }
      e = e.touches[0]
      this.onPointermove(e)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 14:17:02
     * @Desc: mouse move event
     */
    onMousemove(e) {
      if (this.isMobile) {
        return
      }
      this.onPointermove(e)
    },

    // move event
    onPointermove(e) {
      const x = this.getClientOffset(e)[0]
      this.mousemoveX = x
      // press drag
      if (this.mousedown && this.enableDrag) {
        this.drag(x)
      } else if (this.showHoverTime) {
        // Display mouse location time when not pressed
        this.hoverShow(x)
      }
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-21 10:40:37
     * @Desc: mouse out event
     */
    onMouseleave() {
      this.mousemoveX = -1
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 15:29:46
     * @Desc: press drag
     */
    drag(x) {
      if (!this.enableDrag) {
        return
      }
      const PX_PER_MS = this.width / this.totalMS // px/ms
      const diffX = x - this.mousedownX
      // Determine whether the limit is exceeded
      const hfms = this.totalMS / 2
      let _newStartTimestamp =
        this.mousedownCacheStartTimestamp - Math.round(diffX / PX_PER_MS)
      const ct = _newStartTimestamp + hfms
      if (this.timeRangeTimestamp.start && ct < this.timeRangeTimestamp.start) {
        _newStartTimestamp = this.timeRangeTimestamp.start - hfms
      }
      if (this.timeRangeTimestamp.end && ct > this.timeRangeTimestamp.end) {
        _newStartTimestamp = this.timeRangeTimestamp.end - hfms
      }
      this.startTimestamp = _newStartTimestamp
      this.clearCanvas(this.width, this.height)
      this.draw()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 15:29:52
     * @Desc: Display mouse location time when not pressed
     */
    hoverShow(x, noDraw) {
      const PX_PER_MS = this.width / this.totalMS // px/ms
      const time = this.startTimestamp + x / PX_PER_MS
      if (!noDraw) {
        this.clearCanvas(this.width, this.height)
        this.draw()
      }
      const h = this.height * (this.lineHeightRatio.hover === undefined ? 0.3 : this.lineHeightRatio.hover)
      this.drawLine(x, 0, x, h, 1, this.lineColor)
      this.ctx.fillStyle = this.hoverTextColor
      const t = this.hoverTimeFormat ? this.hoverTimeFormat(time) : dayjs(time).format('YYYY-MM-DD HH:mm:ss')
      const w = this.ctx.measureText(t).width
      this.ctx.fillText(t, x - w / 2, h + 20)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 14:28:48
     * @Desc: mouse out event
     */
    onMouseout() {
      this.clearCanvas(this.width, this.height)
      this.draw()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 15:14:12
     * @Desc: mouse scroll
     */
    onMouseweel(event) {
      if (!this.enableZoom) {
        return
      }
      const e = window.event || event
      const delta = Math.max(-1, Math.min(1, e.wheelDelta || -e.detail))
      if (delta < 0) {
        if (this.currentZoomIndex + 1 >= ZOOM.length - 1) {
          this.currentZoomIndex = ZOOM.length - 1
        } else {
          this.currentZoomIndex++
        }
      } else if (delta > 0) {
        // Zoom in
        if (this.currentZoomIndex - 1 <= 0) {
          this.currentZoomIndex = 0
        } else {
          this.currentZoomIndex--
        }
      }
      this.clearCanvas(this.width, this.height)
      this.startTimestamp = this.currentTime - this.totalMS / 2 // current time-Half of the new time frame
      this.draw()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 16:22:04
     * @Desc: click event
     */
    onClick(x, y) {
      const PX_PER_MS = this.width / this.totalMS // px/ms
      const time = this.startTimestamp + x / PX_PER_MS
      const date = dayjs(time).format('YYYY-MM-DD HH:mm:ss')
      const timeSegments = this.getClickTimeSegments(x, y)
      if (timeSegments && timeSegments.length > 0) {
        this.$emit('click_timeSegments', timeSegments, time, date, x)
      } else {
        this.onCanvasClick(time, date, x)
      }
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 16:24:54
     * @Desc: Detect whether a certain time period is currently clicked
     */
    getClickTimeSegments(x, y) {
      const inItems = []
      this.drawTimeSegments((item) => {
        if (this.ctx.isPointInPath(x, y)) {
          inItems.push(item)
        }
      }, true)
      return inItems
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 11:14:30
     * @Desc: Get the mouse distance equivalent to the timeline
     */
    getClientOffset(e) {
      if (!this.$refs.timeLineContainer || !e) {
        return [0, 0]
      }
      const { left, top } = this.$refs.timeLineContainer.getBoundingClientRect()
      return [e.clientX - left, e.clientY - top]
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 14:25:43
     * @Desc: clear canvas
     */
    clearCanvas(w, h) {
      this.ctx.clearRect(0, 0, w, h)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 14:15:25
     * @Desc: time formatting
     */
    graduationTitle(datetime) {
      const time = dayjs(datetime)
      let res = ''
      if (this.formatTime) {
        res = this.formatTime(time)
      }
      if (res) {
        return res
      }
      if (this.yearMode) {
        return time.format('YYYY')
      } else if (this.yearMonthMode) {
        return time.format('YYYY-MM')
      } else if (
        time.hour() === 0 &&
        time.minute() === 0 &&
        time.millisecond() === 0
      ) {
        return time.format('MM-DD')
      } else {
        return time.format('HH:mm')
      }
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2020-04-14 11:28:37
     * @Desc: Draw line segments
     */
    drawLine(x1, y1, x2, y2, lineWidth = 1, color = '#fff') {
      this.ctx.beginPath()
      this.ctx.strokeStyle = color
      this.ctx.lineWidth = lineWidth
      this.ctx.moveTo(x1, y1)
      this.ctx.lineTo(x2, y2)
      this.ctx.stroke()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 15:57:11
     * @Desc: Re-render
     */
    reRender() {
      this.$nextTick(() => {
        this.clearCanvas(this.width, this.height)
        this.reset()
        this.setInitData()
        this.init()
        this.draw()
      })
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 16:07:53
     * @Desc: reset
     */
    reset() {
      this.width = 0
      this.height = 0
      this.ctx = null
      this.currentZoomIndex = 0
      this.currentTime = 0
      this.startTimestamp = 0
      this.mousedown = false
      this.mousedownX = 0
      this.mousedownCacheStartTimestamp = 0
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 15:57:26
     * @Desc: Set current time
     */
    setTime(t) {
      if (this.mousedown) {
        return
      }
      const ts = typeof t === 'number' ? t : new Date(t).getTime()
      this.startTimestamp = ts - this.totalMS / 2
      this.fixStartTimestamp()
      this.clearCanvas(this.width, this.height)
      this.draw()
      if (this.mousemoveX !== -1 && !this.isMobile) {
        this.hoverShow(this.mousemoveX, true)
      }
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-20 19:32:39
     * @Desc: Forward events from window timeline
     */
    triggerClickWindowTimeSegments(data, index, item) {
      this.$emit('click_window_timeSegments', data, index, item)
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-21 09:58:17
     * @Desc: Set resolution
     */
    setZoom(index) {
      this.currentZoomIndex =
        index >= 0 && index < ZOOM.length
          ? index
          : 5
      this.clearCanvas(this.width, this.height)
      this.startTimestamp = this.currentTime - this.totalMS / 2 // current time-Half of the new time frame
      this.draw()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-21 10:15:30
     * @Desc: Toggle window timeline selection
     */
    toggleActive(index) {
      this.windowListInner.forEach((item) => {
        item.active = false
      })
      this.windowListInner[index].active = true
      this.$emit('change_window_time_line', index, this.windowListInner[index])
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-21 10:47:28
     * @Desc: The time point to be observed will return the real-time position at that time point. You can set some of your custom elements based on this position. The position is relative to the browser's visual window.
     */
    watchTime(time, callback, windowTimeLineIndex) {
      if (!time || !callback) {
        return
      }
      this.watchTimeList.push({
        time: typeof time === 'number' ? time : new Date(time).getTime(),
        callback,
        windowTimeLineIndex: typeof windowTimeLineIndex === 'number' ? windowTimeLineIndex - 1 : -1
      })
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-21 13:36:37
     * @Desc: Window timeline scrolling
     */
    onWindowListScroll() {
      this.updateWatchTime()
    },

    /**
     * @Author: Wang Lin25
     * @Date: 2021-01-21 13:40:53
     * @Desc: size refit
     */
    onResize() {
      this.init()
      this.draw()
      try {
        this.$refs.WindowListItem.forEach((item) => {
          item.init()
        })
        // eslint-disable-next-line no-empty
      } catch (error) { }
    },

    // Timeline click event
    onCanvasClick(...args) {
      this.$emit('click_timeline', ...args)
    }
  }
}
</script>

<style>
.timeLineContainer {
  width: 100%;
  height: 100%;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}
.timeLineContainer .canvas {
  flex-grow: 0;
  flex-shrink: 0;
}
.timeLineContainer .windowList {
  width: 100%;
  height: 100%;
  overflow: auto;
  overflow-x: hidden;
  border-top: 1px solid #999999;
  display: flex;
  flex-direction: column;
}
.timeLineContainer .windowList::-webkit-scrollbar {
  display: none;
}

</style>
