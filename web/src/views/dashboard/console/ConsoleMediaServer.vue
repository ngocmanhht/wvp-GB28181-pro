<template>
  <div id="ConsoleMediaServer" style="width: 100%; height: 100%; background: #FFFFFF; text-align: center">
    <ve-histogram ref="ConsoleMEM" :data="chartData" :extend="extend" :settings="chartSettings" width="100%" height="100%" />
  </div>
</template>

<script>

import veHistogram from 'v-charts/lib/histogram'
import moment from 'moment/moment'

export default {
  name: 'ConsoleMediaServer',
  components: {
    veHistogram
  },
  data() {
    return {
      chartData: {
        columns: ['time', 'in', 'out'],
        rows: [
        ]
      },
      chartSettings: {
        area: true,
        labelMap: {
          'in': 'Download',
          'out': 'upload'
        }
      },
      extend: {
        title: {
          show: true,
          text: 'network',
          left: 'center',
          top: 20

        },
        grid: {
          show: true,
          right: '30px',
          containLabel: true
        },
        xAxis: {
          time: 'time',
          max: 'dataMax',
          boundaryGap: ['20%', '20%'],
          axisLabel: {
            formatter: (v) => {
              return moment(v).format('HH:mm:ss')
            },
            showMaxLabel: true
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: (data) => {
            console.log(parseFloat(data[0].data[1]).toFixed(2))
            console.log(parseFloat(data[1].data[1]).toFixed(2))
            console.log('############')
            return 'Download：' + parseFloat(data[0].data[1]).toFixed(2) + 'Mbps' + '</br> upload：' + parseFloat(data[1].data[1]).toFixed(2) + 'Mbps'
          }
        },
        legend: {
          left: 'center',
          bottom: '15px'
        }
      }
    }
  },
  mounted() {
    this.$nextTick(_ => {
      setTimeout(() => {
        this.$refs.ConsoleMEM.echarts.resize()
      }, 100)
    })
  },
  destroyed() {
  },
  methods: {
    setData: function(data) {
      console.log(data)
      this.chartData.rows = data
    }

  }
}
</script>
