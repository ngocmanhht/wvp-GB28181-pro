<template>
  <div id="linkChannelRecord" style="width: 100%;  background-color: #FFFFFF;">
    <el-dialog v-el-drag-dialog v-if="showDialog" v-loading="dialogLoading" title="Channel association" top="2rem" width="80%" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()">
      <div style="display: grid; grid-template-columns: 100px minmax(0, 1fr);">
        <el-tabs v-model="hasLink" tab-position="left" style="" @tab-click="search">
          <el-tab-pane label="Not associated" name="false" />
          <el-tab-pane label="Already linked" name="true" />
        </el-tabs>
        <div>
          <el-form :inline="true" size="mini">
            <el-form-item label="Search">
              <el-input
                v-model="searchStr"
                style="margin-right: 1rem; width: auto;"
                placeholder="Keywords"
                prefix-icon="el-icon-search"
                clearable
                @input="search"
              />
            </el-form-item>
            <el-form-item label="online status">
              <el-select
                v-model="online"
                size="mini"
                style="width: 8rem; margin-right: 1rem;"
                placeholder="Please select"
                default-first-option
                @change="search"
              >
                <el-option label="All" value="" />
                <el-option label="online" value="true" />
                <el-option label="Offline" value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="Type">
              <el-select
                v-model="channelType"
                style="width: 8rem; margin-right: 1rem;"
                placeholder="Please select"
                default-first-option
                @change="search"
              >
                <el-option label="All" value="" />
                <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <div v-if="hasLink !=='true'">
                <el-button size="mini" type="primary" @click="add()">add</el-button>
                <el-button v-if="hasLink !=='true'" size="mini" @click="addByDevice()">Add by device</el-button>
                <el-button v-if="hasLink !=='true'" size="mini" @click="addAll()">Add all channels</el-button>
              </div>
              <div v-else>
                <el-button v-if="hasLink ==='true'" size="mini" type="danger" @click="remove()">Remove</el-button>
                <el-button v-if="hasLink ==='true'" size="mini" @click="removeByDevice()">Remove by device</el-button>
                <el-button v-if="hasLink ==='true'" size="mini" @click="removeAll()">Remove all channels</el-button>
              </div>
            </el-form-item>
            <el-form-item style="float: right;">
              <el-button icon="el-icon-refresh-right" circle size="mini" @click="getChannelList()" />
            </el-form-item>
          </el-form>
          <el-table
            ref="channelListTable"
            size="small"
            :data="channelList"
            height="calc(100vh - 250px)"
            header-row-class-name="table-header"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="gbName" label="Name" min-width="180" />
            <el-table-column prop="gbDeviceId" label="No." min-width="180" />
            <el-table-column prop="gbManufacturer" label="Manufacturer" min-width="100" />
            <el-table-column label="Type" min-width="100">
              <template v-slot:default="scope">
                <div slot="reference" class="name-wrapper">
                  <el-tag size="medium" effect="plain" type="success" :style="$channelTypeList[scope.row.dataType].style">{{ $channelTypeList[scope.row.dataType].name }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Status" min-width="100">
              <template v-slot:default="scope">
                <div slot="reference" class="name-wrapper">
                  <el-tag v-if="scope.row.gbStatus === 'ON'" size="medium">online</el-tag>
                  <el-tag v-if="scope.row.gbStatus !== 'ON'" size="medium" type="info">Offline</el-tag>
                </div>
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
          <gbDeviceSelect ref="gbDeviceSelect" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import gbDeviceSelect from '../../dialog/GbDeviceSelect.vue'

export default {
  name: 'LinkChannelRecord',
  directives: { elDragDialog },
  components: { gbDeviceSelect },
  data() {
    return {
      dialogLoading: false,
      showDialog: false,
      chooseData: {},
      channelList: [],
      searchStr: '',
      channelType: '',
      online: '',
      hasLink: 'false',
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      planId: null,
      loadSnap: {},
      multipleSelection: []
    }
  },

  created() {},
  destroyed() {},
  methods: {
    openDialog(planId, closeCallback) {
      this.planId = planId
      this.showDialog = true
      this.closeCallback = closeCallback
      this.initData()
    },
    initData: function() {
      this.currentPage = 1
      this.count = 15
      this.total = 0
      this.getChannelList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.initData()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getChannelList()
    },
    getChannelList: function() {
      this.$store.dispatch('recordPlan/queryChannelList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online,
        channelType: this.channelType,
        planId: this.planId,
        hasLink: this.hasLink
      })
        .then(data => {
          this.total = data.total
          this.channelList = data.list
          // Prevent form misalignment
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
        .catch((error) => {
          console.log(error)
        })
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },

    linkPlan: function(data) {
      this.loading = true
      return this.$store.dispatch('recordPlan/linkPlan', data)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: 'Saved successfully'
          })
          this.getChannelList()
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          this.loading = false
        })
    },

    add: function(row) {
      const channels = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channels.push(this.multipleSelection[i].gbId)
      }
      if (channels.length === 0) {
        this.$message.info({
          showClose: true,
          message: 'Please select channel'
        })
        return
      }
      this.linkPlan({
        planId: this.planId,
        channelIds: channels
      })
    },
    addAll: function(row) {
      this.$confirm('Adding all channels will include channels that have been added to other plans, be sure to add all channels？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.linkPlan({
          planId: this.planId,
          allLink: true
        })
      }).catch(() => {
      })
    },

    addByDevice: function(row) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.linkPlan({
          planId: this.planId,
          deviceDbIds: deviceIds
        })
      })
    },

    removeByDevice: function(row) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.linkPlan({
          deviceDbIds: deviceIds
        })
      })
    },
    remove: function(row) {
      const channels = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channels.push(this.multipleSelection[i].gbId)
      }
      if (channels.length === 0) {
        this.$message.info({
          showClose: true,
          message: 'Please select channel'
        })
        return
      }

      this.linkPlan({
        channelIds: channels
      })
    },
    removeAll: function(row) {
      this.$confirm('Confirm to remove all channels？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.linkPlan({
          planId: this.planId,
          allLink: false
        })
      }).catch(() => {
      })
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    refresh: function() {
      this.initData()
    }
  }
}
</script>
