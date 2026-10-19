<template>
  <div id="app" class="app-container">
    <div style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="Search">
          <el-input
            v-model="search"
            style="margin-right: 1rem; width: auto;"
            size="mini"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="getFileList"
          />
        </el-form-item>
        <el-form-item label="start time">
          <el-date-picker
            v-model="startTime"
            size="mini"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="Select date time"
            @change="getFileList"
          />
        </el-form-item>
        <el-form-item label="end time">
          <el-date-picker
            v-model="endTime"
            size="mini"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="Select date time"
            @change="getFileList"
          />
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="getFileList()" />
        </el-form-item>
      </el-form>
      <!--Log list-->
      <el-table size="medium" :data="fileList" style="width: 100%" :height="winHeight">
        <el-table-column
          type="selection"
          width="55"
        />
        <el-table-column prop="fileName" label="file name" />
        <el-table-column prop="fileSize" label="file size">
          <template v-slot:default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="start time">
          <template v-slot:default="scope">
            {{ formatTimeStamp(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="end time">
          <template v-slot:default="scope">
            {{ formatTimeStamp(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="Operation" width="200" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-document" type="text" @click="showLogView(scope.row)">View
            </el-button>
            <el-button size="medium" icon="el-icon-download" type="text" @click="downloadFile(scope.row)">Download
            </el-button>
            <!--            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c"-->
            <!--                       @click="deleteRecord(scope.row)">Delete-->
            <!--            </el-button>-->
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      top="10vh"
      :title="playerTitle"
      :visible.sync="showLog"
      width="90%"
    >
      <div style="height: 600px">
        <showLog ref="recordVideoPlayer" :file-url="fileUrl" :load-end="loadEnd" />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import showLog from './showLog.vue'
import moment from 'moment'
import { getToken } from '@/utils/auth'

export default {
  name: 'OperationsHistoryLog',
  components: {
    showLog
  },
  data() {
    return {
      search: '',
      startTime: '',
      endTime: '',
      showLog: false,
      playerTitle: '',
      fileUrl: '',
      playerStyle: {
        'margin': 'auto',
        'margin-bottom': '20px',
        'width': window.innerWidth / 2 + 'px',
        'height': this.winHeight / 2 + 'px'
      },
      mediaServerList: [], // List of dead nodes
      mediaServerId: '', // media services
      mediaServerPath: null, // Media service address
      fileList: [], // Device list
      chooseRecord: null, // media services

      updateLooper: 0, // Data refresh rotation training flag
      winHeight: window.innerHeight - 180,
      loading: false

    }
  },
  computed: {},
  mounted() {
    this.initData()
  },
  destroyed() {
    this.$destroy('recordVideoPlayer')
  },
  methods: {
    initData: function() {
      this.getFileList()
    },
    getFileList: function() {
      this.$store.dispatch('log/queryList', {
        query: this.search,
        startTime: this.startTime,
        endTime: this.endTime
      })
        .then((data) => {
          this.fileList = data
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    showLogView(file) {
      this.playerTitle = 'Loading logs...'
      this.fileUrl = `/api/log/file/${file.fileName}`
      this.showLog = true
      this.file = file
    },
    downloadFile(file) {
      // const link = document.createElement('a');
      // link.target = "_blank";
      // link.download = file.fileName;
      // if (process.env.NODE_ENV === 'development') {
      //   link.href = `/debug/api/log/file/${file.fileName}`
      // }else {
      //   link.href = `/api/log/file/${file.fileName}`
      // }
      //
      // link.click();

      // File download address
      const fileUrl = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : window.baseUrl) + `/api/log/file/${file.fileName}`

      // Set request header
      const headers = new Headers()
      headers.append('access-token', getToken()) // Set the authorization header and replace YourAccessToken with the actual access token
      // Make a request
      fetch(fileUrl, {
        method: 'GET',
        headers: headers
      })
        .then(response => response.blob())
        .then(blob => {
          console.log(blob)
          // Create a virtual link element to simulate a click to download
          const link = document.createElement('a')
          link.target = '_blank'
          link.href = window.URL.createObjectURL(blob)
          link.download = file.fileName // Set the download file name and replace filename.ext with the actual file name and extension.
          document.body.appendChild(link)

          // simulate click
          link.click()

          // Remove virtual link element
          document.body.removeChild(link)
          this.$message.success('Screenshot requested', { closed: true })
        })
        .catch(error => console.error('Download failed：', error))
    },
    loadEnd() {
      this.playerTitle = this.file.fileName
    },
    deleteRecord() {
      // TODO
      const that = this
      this.$axios({
        method: 'delete',
        url: `/record_proxy/api/record/delete`,
        params: {
          page: that.currentPage,
          count: that.count
        }
      }).then(function(res) {
        console.log(res)
        if (res.data.code === 0) {
          that.total = res.data.data.total
          that.fileList = res.data.data.list
        }
      }).catch(function(error) {
        console.log(error)
      })
    },
    formatTime(time) {
      const h = parseInt(time / 3600 / 1000)
      const minute = parseInt((time - h * 3600 * 1000) / 60 / 1000)
      let second = Math.ceil((time - h * 3600 * 1000 - minute * 60 * 1000) / 1000)
      if (second < 0) {
        second = 0
      }
      return (h > 0 ? h + `hours` : '') + (minute > 0 ? minute + 'points' : '') + (second > 0 ? second + 'seconds' : '')
    },
    formatTimeStamp(time) {
      return moment.unix(time / 1000).format('yyyy-MM-DD HH:mm:ss')
    },
    formatFileSize(fileSize) {
      if (fileSize < 1024) {
        return fileSize + 'B'
      } else if (fileSize < (1024 * 1024)) {
        let temp = fileSize / 1024
        temp = temp.toFixed(2)
        return temp + 'KB'
      } else if (fileSize < (1024 * 1024 * 1024)) {
        let temp = fileSize / (1024 * 1024)
        temp = temp.toFixed(2)
        return temp + 'MB'
      } else {
        let temp = fileSize / (1024 * 1024 * 1024)
        temp = temp.toFixed(2)
        return temp + 'GB'
      }
    }

  }
}
</script>

<style>

</style>
