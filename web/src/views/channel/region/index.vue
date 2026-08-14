<template>
  <div id="region" class="app-container" style="height: calc(100vh - 118px);display: grid; grid-template-columns: 400px auto">
    <RegionTree
      ref="regionTree"
      :show-header="true"
      :edit="true"
      @clickEvent="treeNodeClickEvent"
      @onChannelChange="onChannelChange"
      :enable-add-channel="true"
      :add-channel-to-civil-code="addChannelToCivilCode"
    />
    <div style="padding: 0 20px">
      <el-form :inline="true" size="mini">
        <el-form-item>
          <el-breadcrumb v-if="regionParents.length > 0" separator="/" style="display: ruby">
            <el-breadcrumb-item v-for="key in regionParents" :key="key">{{ key }}</el-breadcrumb-item>
          </el-breadcrumb>
          <div v-else style="color: #00c6ff">No administrative division selected</div>
        </el-form-item>
        <div style="float: right;">
          <el-form-item label="Search">
            <el-input
              v-model="searchStr"
              style="width: 10rem; margin-right: 1rem;"
              placeholder="Keywords"
              prefix-icon="el-icon-search"
              clearable
              @input="search"
            />
          </el-form-item>
          <el-form-item label="online status">
            <el-select
              v-model="online"
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
          <el-form-item label="Type" >
            <el-select
              v-model="channelType"
              style="width: 8rem; margin-right: 1rem;"
              placeholder="Please select"
              default-first-option
              @change="getChannelList"
            >
              <el-option label="All" value="" />
              <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="add()">
              add channel
            </el-button>
            <el-button :disabled="multipleSelection.length === 0" type="danger" @click="remove()">
              Remove channel
            </el-button>
            <el-button plain type="warning" @click="showUnusualChanel()">
              Abnormal mount channel
            </el-button>
          </el-form-item>
          <el-form-item >
            <el-button icon="el-icon-refresh-right" circle @click="getChannelList()" />
          </el-form-item>
        </div>
      </el-form>
      <el-table
        ref="channelListTable"
        size="medium"
        :data="channelList"
        :height="tableHeight"
        style="width: 100%"
        header-row-class-name="table-header"
        @selection-change="handleSelectionChange"
        @row-dblclick="rowDblclick"
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
        <el-table-column label="location information" min-width="150">
          <template v-slot:default="scope">
            <span v-if="scope.row.gbLongitude && scope.row.gbLatitude">{{ scope.row.gbLongitude }}<br>{{ scope.row.gbLatitude }}</span>
            <span v-if="!scope.row.gbLongitude || !scope.row.gbLatitude">None</span>
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
    </div>
    <GbChannelSelect ref="gbChannelSelect" data-type="civilCode" />
    <UnusualRegionChannelSelect ref="unusualRegionChannelSelect" />
  </div>
</template>

<script>
import RegionTree from '../..//common/RegionTree.vue'
import GbChannelSelect from '../../dialog/GbChannelSelect.vue'
import UnusualRegionChannelSelect from './UnusualRegionChannelSelect.vue'

export default {
  name: 'Region',
  components: {
    GbChannelSelect,
    RegionTree,
    UnusualRegionChannelSelect
  },
  data() {
    return {
      channelList: [],
      tableHeight: 'calc(100vh - 190px)',
      searchStr: '',
      channelType: '',
      online: '',
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
      regionId: '',
      regionDeviceId: '',
      regionParents: [],
      multipleSelection: []
    }
  },

  created() {
    this.initData()
    this.tableHeight = `calc(100vh - ${this.$refs.queryForm.offsetHeight}px)`
  },
  destroyed() {
  },
  methods: {
    initData: function() {
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
      this.$store.dispatch('commonChanel/getCivilCodeList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online,
        channelType: this.channelType,
        civilCode: this.regionDeviceId
      })
        .then(data => {
          this.total = data.total
          this.channelList = data.list
          // Prevent form misalignment
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    rowDblclick: function(row, rowIndex) {
      // if (row.gbCivilCode) {
      //   this.$refs.regionTree.refresh(row.gbCivilCode)
      // }
    },
    add: function(row) {
      if (this.regionDeviceId === '') {
        this.$message.info({
          showClose: true,
          message: 'Please select the administrative division on the left'
        })
        return
      }
      this.$refs.gbChannelSelect.openDialog((data) => {
        console.log('selected data')
        console.log(data)
        this.addChannelToCivilCode(this.regionDeviceId, data)
      })
    },
    addChannelToCivilCode: function(regionDeviceId, data) {
      if (data.length === 0) {
        return
      }
      const channels = []
      for (let i = 0; i < data.length; i++) {
        channels.push(data[i].gbId)
      }
      this.loading = true
      this.$store.dispatch('commonChanel/addToRegion', {
        civilCode: regionDeviceId,
        channelIds: channels
      })
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
      this.loading = true
      this.$store.dispatch('commonChanel/deleteFromRegion', channels)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: 'Saved successfully'
          })
          this.getChannelList()
          // Refresh tree nodes
          this.$refs.regionTree.refresh(this.regionDeviceId)
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
    showUnusualChanel: function() {
      this.$refs.unusualRegionChannelSelect.openDialog()
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    refresh: function() {
      this.initData()
    },
    treeNodeClickEvent: function(region) {
      this.regionDeviceId = region.deviceId
      if (region.deviceId === '') {
        this.channelList = []
        this.regionParents = []
      }
      this.initData()
      this.$store.dispatch('region/queryPath', this.regionDeviceId)
        .then(data => {
          const path = []
          for (let i = 0; i < data.length; i++) {
            path.push(data[i].name)
          }
          this.regionParents = path
        })
    },
    gbChannelSelectEnd: function(selectedData) {
      console.log(selectedData)
    },
    onChannelChange: function(deviceId) {
      if (this.regionDeviceId === deviceId) {
        this.getChannelList()
      }
    }
  }
}
</script>
