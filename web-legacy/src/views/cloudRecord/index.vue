<template>
  <div id="app" class="app-container">
    <div style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="Search">
          <el-input
            v-model="search"
            style="margin-right: 1rem; width: auto;"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="initData"
          />
        </el-form-item>
        <el-form-item label="Call Id">
          <el-input
            v-model="callId"
            style="margin-right: 1rem; width: auto;"
            placeholder="Transaction ID"
            prefix-icon="el-icon-search"
            clearable
            @input="initData"
          />
        </el-form-item>
        <el-form-item label="start time">
          <el-date-picker
            v-model="startTime"
            type="datetime"
            size="mini"
            style="width: 12rem; margin-right: 1rem;"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="Select date time"
            @change="initData"
          />
        </el-form-item>
        <el-form-item label="end time">
          <el-date-picker
            v-model="endTime"
            type="datetime"
            size="mini"
            style="width: 12rem; margin-right: 1rem;"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="Select date time"
            @change="initData"
          />
        </el-form-item>
        <el-form-item label="Node selection">
          <el-select
            v-model="mediaServerId"
            size="mini"
            style="width: 12rem; margin-right: 1rem;"
            placeholder="Please select"
            @change="initData"
          >
            <el-option label="All" value="" />
            <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id"
            />
          </el-select>
          <el-button
            icon="el-icon-delete"
            style="margin-right: 1rem;"
            :disabled="multipleSelection.length === 0"
            type="danger"
            @click="deleteRecord"
          >Remove
          </el-button>
          <el-button
            icon="el-icon-download"
            style="margin-right: 1rem;"
            :disabled="multipleSelection.length === 0"
            type="primary"
            @click="downloadZip"
          >Download
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle :loading="loading" @click="initData()" />
        </el-form-item>
      </el-form>
      <!--Device list-->
      <el-table :data="recordList" style="width: 100%" size="small" :loading="loading" height="calc(100% - 64px)" @selection-change="handleSelectionChange">
        <el-table-column
          type="selection"
          width="55"
        />
        <el-table-column prop="app" label="Application name" />
        <el-table-column prop="stream" label="flowID" />
        <el-table-column prop="callId" label="Call Id"/>
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
        <el-table-column label="duration">
          <template v-slot:default="scope">
            <el-tag v-if="myServerId !== scope.row.serverId" style="border-color: #ecf1af">{{ formatTime(scope.row.timeLen) }}</el-tag>
            <el-tag v-if="myServerId === scope.row.serverId">{{ formatTime(scope.row.timeLen) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="File name" width="200" />
        <el-table-column prop="mediaServerId" label="streaming media" />
        <el-table-column label="Operation" fixed="right" width="260">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-video-play" type="text" @click="play(scope.row)">play
            </el-button>
            <el-button size="medium" icon="el-icon-download" type="text" @click="downloadFile(scope.row)">Download
            </el-button>
            <el-button size="medium" icon="el-icon-info" type="text" @click="showDetail(scope.row)">Details
            </el-button>
            <el-button
              size="medium"
              icon="el-icon-delete"
              type="text"
              style="color: #f56c6c"
              @click="deleteOneRecord(scope.row)"
            >Delete
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </div>
    <playerDialog ref="playerDialog"></playerDialog>
  </div>
</template>

<script>
import playerDialog from './playerDialog.vue'
import moment from 'moment'
import Vue from 'vue'

export default {
  name: 'CloudRecord',
  components: { playerDialog },
  data() {
    return {
      search: '',
      callId: '',
      startTime: '',
      endTime: '',
      playerTitle: '',
      videoUrl: '',
      mediaServerList: [], // List of dead nodes
      multipleSelection: [],
      mediaServerId: '', // media services
      mediaServerPath: null, // Media service address
      recordList: [], // Device list
      chooseRecord: null, // media services
      updateLooper: 0, // Data refresh rotation training flag
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false

    }
  },
  computed: {
    Vue() {
      return Vue
    },
    myServerId() {
      return this.$store.getters.serverId
    }
  },
  mounted() {
    this.initData()
    this.getMediaServerList()
  },
  destroyed() {
    // this.$destroy('recordVideoPlayer')
  },
  methods: {
    initData: function() {
      this.currentPage = 1
      this.getRecordList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getRecordList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getRecordList()
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    getMediaServerList: function() {
      this.$store.dispatch('server/getOnlineMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
    },
    getRecordList: function() {
      this.$store.dispatch('cloudRecord/queryList', {
        query: this.search,
        callId: this.callId,
        startTime: this.startTime,
        endTime: this.endTime,
        mediaServerId: this.mediaServerId,
        page: this.currentPage,
        count: this.count
      })
        .then((data) => {
          this.total = data.total
          this.recordList = data.list
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    play(row) {
      this.chooseRecord = row
      this.$refs.playerDialog.stopPlay()
      this.$store.dispatch('cloudRecord/loadRecord', {
        app: row.app,
        stream: row.stream,
        cloudRecordId: row.id
      })
        .then(data => {
          this.$refs.playerDialog.openDialog(data, row.timeLen, row.startTime)
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.playLoading = false
        })
    },
    downloadFile(row) {
      this.$store.dispatch('cloudRecord/getPlayPath', row.id)
        .then((data) => {
          const link = document.createElement('a')
          link.target = '_blank'
          if (location.protocol === 'https:') {
            if (data.httpsPath) {
              link.href = data.httpsPath + '&save_name=' + row.fileName
            }else if (data.httpPath){
              link.href = data.httpPath + '&save_name=' + row.fileName
            }else {
              this.$message.error({
                showClose: true,
                message: 'Failed to obtain download address'
              })
            }
          } else {
            if (data.httpPath) {
              link.href = data.httpPath + '&save_name=' + row.fileName
            }else if (data.httpsPath){
              link.href = data.httpsPath + '&save_name=' + row.fileName
            }else {
              this.$message.error({
                showClose: true,
                message: 'Failed to obtain download address'
              })
            }
          }
          link.click()
        })
        .catch((error) => {
          console.log(error)
        })
    },
    showDetail(row) {
      this.$router.push(`/cloudRecord/detail/${row.app}/${row.stream}`)
    },
    deleteRecord() {
      this.$confirm(`Confirm to delete selected${this.multipleSelection.length}files?`, 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        const ids = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          ids.push(this.multipleSelection[i].id)
        }
        this.$store.dispatch('cloudRecord/deleteRecord', ids)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: 'Delete successfully'
            })
            this.getRecordList()
          })
      }).catch(() => {

      })
    },
    downloadZip() {
      const ids = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        ids.push(this.multipleSelection[i].id)
      }
      let idsStr = ids.join(',')
      const link = document.createElement('a')
      link.target = '_blank'
      let baseUri = (process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : process.env.VUE_APP_BASE_API
      let downloadUrl = `${location.origin}${baseUri}/api/cloud/record/download/zip?ids=${idsStr}`
      console.log(downloadUrl)
      link.href = downloadUrl
      link.click()
    },
    deleteOneRecord(row) {
      this.$confirm('Confirm deletion?', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        const ids = []
        ids.push(row.id)
        this.$store.dispatch('cloudRecord/deleteRecord', ids)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: 'Delete successfully'
            })
            this.getRecordList()
          })
      }).catch(() => {

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
    }
  }
}
</script>

<style>
.el-dialog__body {
  padding: 20px 0 0 0 !important;
}
</style>
