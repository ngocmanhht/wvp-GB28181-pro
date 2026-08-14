<template>
  <div id="queryTrace" >
    <el-dialog
      title="Query track"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div v-loading="isLoging">
        <el-date-picker v-model="searchFrom" type="datetime" placeholder="Select start date and time" default-time="00:00:00" value-format="yyyy-MM-dd HH:mm:ss" size="mini" style="width: 11rem;" align="right" :picker-options="pickerOptions"></el-date-picker>
        <el-date-picker v-model="searchTo" type="datetime" placeholder="Select end date and time" default-time="00:00:00" value-format="yyyy-MM-dd HH:mm:ss" size="mini" style="width: 11rem;" align="right" :picker-options="pickerOptions"></el-date-picker>
        <el-button icon="el-icon-search" size="mini" type="primary" @click="onSubmit">Query</el-button>
      </div>

    </el-dialog>
  </div>
</template>

<script>

export default {
  name: 'deviceEdit',
  props: [],
  computed: {},
  created() {},
  data() {
    return {
      pickerOptions: {
        shortcuts: [{
          text: 'today',
          onClick(picker) {
            picker.$emit('pick', new Date())
          }
        }, {
          text: 'yesterday',
          onClick(picker) {
            const date = new Date()
            date.setTime(date.getTime() - 3600 * 1000 * 24)
            picker.$emit('pick', date)
          }
        }, {
          text: 'a week ago',
          onClick(picker) {
            const date = new Date()
            date.setTime(date.getTime() - 3600 * 1000 * 24 * 7)
            picker.$emit('pick', date)
          }
        }]
      },
      searchFrom: null,
      searchTo: null,
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      channel: null,
      callback: null
    }
  },
  methods: {
    openDialog: function (channel, callback) {
      console.log(channel)
      this.showDialog = true
      this.callback = callback
      this.channel = channel
    },

    onSubmit: function () {
      console.log('onSubmit')
      this.isLoging = true
      let url = `/api/position/history/${this.channel.deviceId}?start=${this.searchFrom}&end=${this.searchTo}`
      if (this.channel.channelId) {
        url+='&channelId=${this.channel.channelId}'
      }
      this.$axios.get(url, {
      }).then((res) => {
        this.isLoging = false
        if (typeof this.callback === 'function') {
          if (res.data.code == 0) {
            this.callback(res.data.data)
            this.close()
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }

        }
      }).catch(function (error) {
          this.isLoging = false
          console.error(error)
        })
    },
    close: function () {
      this.showDialog = false
      this.isLoging = false
      this.callback = null
      this.channel = null
    }
  }
}
</script>
