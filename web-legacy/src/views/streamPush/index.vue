<template>
  <div id="pushList" class="app-container">
    <div v-if="!streamPush" style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="Search">
          <el-input
            v-model="searchStr"
            style="margin-right: 1rem; width: auto;"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="queryList"
          />
        </el-form-item>
        <el-form-item label="streaming media">
          <el-select
            v-model="mediaServerId"
            style="margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="queryList"
          >
            <el-option label="All" value="" />
            <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Push status">
          <el-select
            v-model="pushing"
            style="margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="queryList"
          >
            <el-option label="All" value="" />
            <el-option label="Pushing" value="true" />
            <el-option label="Stopped" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-plus" style="margin-right: 1rem;" type="primary" @click="addStream">add
          </el-button>
          <el-button-group>
            <el-button icon="el-icon-upload2" @click="importChannel">
              Channel import
            </el-button>
            <el-button icon="el-icon-download">
              <a
                style="text-align: center; text-decoration: none"
                href="/static/file/Push channel import.zip"
                download="Push channel import.zip"
              >Download template</a>
            </el-button>
          </el-button-group>
          <el-button
            icon="el-icon-delete"
            style="margin-left: 1rem;"
            :disabled="multipleSelection.length === 0"
            type="danger"
            @click="batchDel"
          >Remove
          </el-button>
          <el-button icon="el-icon-chicken" @click="buildPushStream">Generate push address</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="pushListTable"
        size="small"
        :data="pushList"
        style="width: 100%"
        height="calc(100% - 64px)"
        :loading="loading"
        :row-key="(row)=> row.app + row.stream"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" :reserve-selection="true" min-width="55" />
        <el-table-column prop="gbName" label="Name" min-width="150" />
        <el-table-column prop="app" label="Application name" min-width="100" />
        <el-table-column prop="stream" label="flowID" min-width="200" />
        <el-table-column label="Push status" min-width="100">
          <template v-slot:default="scope">
            <el-tag v-if="scope.row.pushing && myServerId !== scope.row.serverId" size="medium" style="border-color: #ecf1af">Pushing</el-tag>
            <el-tag v-if="scope.row.pushing && myServerId === scope.row.serverId" size="medium">Pushing</el-tag>
            <el-tag v-if="!scope.row.pushing" size="medium" type="info">Stopped</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gbDeviceId" label="National standard code" min-width="200" />
        <el-table-column label="National standard status" min-width="100">
          <template v-slot:default="scope">
            <el-tag v-if="scope.row.gbStatus === 'ON' " size="medium">online</el-tag>
            <el-tag v-if="scope.row.gbStatus !== 'ON' " size="medium" type="info">Offline</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="location information" min-width="150">
          <template v-slot:default="scope">
            <span v-if="scope.row.gbLongitude && scope.row.gbLatitude" size="medium">{{ scope.row.gbLongitude }}<br>{{ scope.row.gbLatitude }}</span>
            <span v-if="!scope.row.gbLongitude || !scope.row.gbLatitude" size="medium">None</span>
          </template>
        </el-table-column>
        <el-table-column prop="mediaServerId" label="streaming media" min-width="150" />
        <el-table-column label="start time" min-width="150">
          <template v-slot:default="scope">
            <el-button-group>
              {{ scope.row.pushTime == null? "-":scope.row.pushTime }}
            </el-button-group>
          </template>
        </el-table-column>

        <el-table-column label="Operation" min-width="300" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" :loading="scope.row.playLoading" icon="el-icon-video-play" type="text" @click="playPush(scope.row)">play
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c" @click="deletePush(scope.row.id)">Delete</el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-position" type="text" @click="edit(scope.row)">
              Edit
            </el-button>
            <el-button size="medium" icon="el-icon-cloudy" type="text" @click="queryCloudRecords(scope.row)">Cloud recording
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
    <streamPushPlayer ref="streamPushPlayer" />
    <addStreamTOGB ref="addStreamTOGB" />
    <importChannel ref="importChannel" />
    <stream-push-edit v-if="streamPush" :stream-push="streamPush" :close-edit="closeEdit" />
    <buildPushStreamUrl ref="buildPushStreamUrl" />
  </div>
</template>

<script>
import streamPushPlayer from './player.vue'
import addStreamTOGB from './dialog/pushStreamEdit.vue'
import importChannel from './dialog/importChannel.vue'
import StreamPushEdit from './edit.vue'
import buildPushStreamUrl from './buildPushStreamUrl.vue'

export default {
  name: 'PushList',
  components: {
    StreamPushEdit,
    streamPushPlayer,
    addStreamTOGB,
    importChannel,
    buildPushStreamUrl
  },
  data() {
    return {
      pushList: [], // Device list
      currentPusher: {}, // Current operating device object
      updateLooper: 0, // Data refresh rotation training flag
      currentDeviceChannelsLenth: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      searchStr: '',
      pushing: '',
      mediaServerId: '',
      mediaServerList: [],
      multipleSelection: [],
      loading: false,
      streamPush: null
    }
  },
  mounted() {
    this.initData()
    this.updateLooper = setInterval(this.getPushList, 2000)
  },
  destroyed() {
    clearTimeout(this.updateLooper)
  },
  computed: {
    myServerId() {
      return this.$store.getters.serverId
    }
  },
  methods: {
    initData: function() {
      this.loading = true
      this.$store.dispatch('server/getMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
      this.getPushList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getPushList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getPushList()
    },
    queryList: function() {
      this.currentPage = 1
      this.total = 0
      this.getPushList()
    },
    getPushList: function() {
      this.$store.dispatch('streamPush/queryList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        pushing: this.pushing,
        mediaServerId: this.mediaServerId
      })
        .then((data) => {
          this.total = data.total
          this.pushList = data.list
          this.pushList.forEach(e => {
            this.$set(e, 'location', '')
            this.$set(e, 'playLoading', false)
            if (e.gbLongitude && e.gbLatitude) {
              this.$set(e, 'location', e.gbLongitude + ',' + e.gbLatitude)
            }
          })
        })
        .finally(() => {
          this.loading = false
        })
    },

    playPush: function(row) {
      row.playLoading = true
      this.$store.dispatch('streamPush/play', row.id)
        .then((data) => {
          this.$refs.streamPushPlayer.openDialog(data, true)
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          row.playLoading = false
        })
    },
    deletePush: function(id) {
      this.$confirm('Confirm to delete channel?', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.loading = true
        this.$store.dispatch('streamPush/remove', id)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: 'Delete successfully'
            })
            this.initData()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }).catch(() => {

      })
    },
    edit: function(row) {
      this.streamPush = row
    },
    // End editing
    closeEdit: function() {
      this.streamPush = null
      this.getPushList()
    },
    queryCloudRecords: function(row) {
      this.$router.push(`/cloudRecord/detail/${row.app}/${row.stream}`)
    },
    importChannel: function() {
      this.$refs.importChannel.openDialog(() => {})
    },
    addStream: function() {
      this.streamPush = {}
    },
    batchDel: function() {
      this.$confirm(`Confirm to delete selected${this.multipleSelection.length}channels?`, 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        const ids = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          ids.push(this.multipleSelection[i].id)
        }
        this.$store.dispatch('streamPush/batchRemove', ids)
          .then((data) => {
            this.initData()
            this.$refs.pushListTable.clearSelection()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }).catch(() => {

      })
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    refresh: function() {
      this.initData()
    },
    buildPushStream: function() {
      this.$refs.buildPushStreamUrl.openDialog()
    }
  }
}
</script>

