<template>
  <div id="timeStatistics" v-loading="loading">
    <el-dialog
      v-el-drag-dialog
      :title="title"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close"
    >
      <div style="margin-right: 20px;">
        <el-row type="flex" justify="space-between" align="middle" style="margin-bottom: 12px;">
          <div>
            <el-button-group>
              <el-button type="primary" :plain="viewMode !== 'table'" size="mini" @click="viewMode = 'table'">table</el-button>
              <el-button type="primary" :plain="viewMode !== 'chart'" size="mini" @click="viewMode = 'chart'">Line chart</el-button>
            </el-button-group>
            <el-button icon="el-icon-refresh" size="mini" @click="fetchData" style="margin-left: 8px;">Refresh</el-button>
          </div>
          <el-form :inline="true" size="mini">
            <el-form-item label="Quantity">
              <el-input-number v-model="count" :min="1" @change="fetchData" />
            </el-form-item>
          </el-form>
        </el-row>

        <el-table
          v-if="viewMode === 'table'"
          :data="tableData"
          border
          stripe
          size="mini"
          height="400px"
          style="width: 100%;"
        >
          <el-table-column prop="time" label="time" min-width="180" />
          <el-table-column prop="timeDiff" label="interval(seconds)" min-width="120" />
        </el-table>

        <ve-line
          v-else
          :data="chartData"
          :extend="extend"
          height="400px"
          :legend-visible="false"
        />
      </div>
      <div style="margin-top: 12px; text-align: right;">
        <span>Maximum fluctuation：{{ timeDiffDelta }} seconds</span>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import moment from 'moment/moment'
import veLine from 'v-charts/lib/line'
import request from '@/utils/request'
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'TimeStatistics',
  components: { veLine },
  directives: { elDragDialog },
  data() {
    return {
      title: null,
      url: null,
      deviceId: null,
      count: 50,
      showDialog: false,
      loading: false,
      viewMode: 'table',
      list: [],
      extend: {
        grid: { right: '30px', containLabel: true },
        xAxis: {
          boundaryGap: false,
          axisLabel: {
            formatter: (v) => moment(v).format('HH:mm:ss')
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          splitNumber: 6,
          axisLabel: { formatter: (v) => `${v} seconds` }
        },
        tooltip: {
          trigger: 'axis',
          formatter: (data) => {
            if (!data || !data.length) return ''
            const [item] = data
            return `${moment(item.data[0]).format('HH:mm:ss')}<br/>interval：${item.data[1]} seconds`
          }
        },
        series: {
          itemStyle: { color: '#409EFF' }
        }
      }
    }
  },
  computed: {
    chartData() {
      return {
        columns: ['time', 'timeDiff'],
        rows: this.list
      }
    },
    tableData() {
      return this.list.slice().reverse();
    },
    timeDiffDelta() {
      if (!this.list.length) return 0
      const nums = this.list
        .map(item => Number(item.timeDiff))
        .filter(v => !Number.isNaN(v))
      if (!nums.length) return 0
      const max = Math.max(...nums)
      const min = Math.min(...nums)
      return (max - min).toFixed(2)
    }
  },
  methods: {
    openDialog(title, url, deviceId, count = 50) {
      this.title = title
      this.url = url
      this.deviceId = deviceId
      this.count = count
      this.showDialog = true
      this.viewMode = 'table'
      this.fetchData()
    },
    fetchData() {
      console.log(this.url)
      if (!this.url || !this.deviceId) return
      this.loading = true
      this.$store.dispatch(this.url, {
        deviceId: this.deviceId,
        count: this.count
      }).then(data => {
          this.list = data
      }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error.message
          })
      })
    },
    close() {
      this.title = null
      this.url = null
      this.deviceId = null
      this.list = []
      this.showDialog = false
      this.loading = false
    }
  }
}
</script>
