<template>
  <div id="addUser" v-loading="getDeviceListLoading">
    <el-dialog
      v-el-drag-dialog
      title="Add national standard equipment channel"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      append-to-body
      @close="close()"
    ><el-form :inline="true" size="mini">
       <el-form-item label="Search">
         <el-input
           v-model="searchStr"
           style="margin-right: 1rem; width: auto;"
           size="mini"
           placeholder="Keywords"
           prefix-icon="el-icon-search"
           clearable
           @input="getDeviceList"
         />
       </el-form-item>
       <el-form-item label="online status">
         <el-select
           v-model="online"
           size="mini"
           style="width: 8rem; margin-right: 1rem;"
           placeholder="Please select"
           default-first-option
           @change="getDeviceList"
         >
           <el-option label="All" value="" />
           <el-option label="online" value="true" />
           <el-option label="Offline" value="false" />
         </el-select>
       </el-form-item>
       <el-form-item style="float: right;">
         <el-button icon="el-icon-refresh-right" circle @click="getDeviceList()" />
         <el-button type="primary" @click="onSubmit">OK</el-button>
       </el-form-item>
     </el-form>
      <!--Device list-->
      <el-table size="medium" :data="deviceList" style="width: 100%;font-size: 12px;" :height="winHeight" header-row-class-name="table-header" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="Name" min-width="160" />
        <el-table-column prop="deviceId" label="Device number" min-width="200" />
        <el-table-column prop="channelCount" label="Number of channels" min-width="120" />
        <el-table-column prop="manufacturer" label="Manufacturer" min-width="120" />
        <el-table-column label="address" min-width="160">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.hostAddress" size="medium">{{ scope.row.hostAddress }}</el-tag>
              <el-tag v-if="!scope.row.hostAddress" size="medium">unknown</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Status" min-width="120">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.onLine" size="medium">online</el-tag>
              <el-tag v-if="!scope.row.onLine" size="medium" type="info">Offline</el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[10, 25, 35, 50, 200, 1000, 50000]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'GbDeviceSelect',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      deviceList: [], // Device list
      currentDevice: {}, // Current operating device object
      searchStr: '',
      online: null,
      videoComponentList: [],
      updateLooper: 0, // Data refresh rotation training flag
      currentDeviceChannelsLenth: 0,
      winHeight: 580,
      currentPage: 1,
      count: 10,
      total: 0,
      getDeviceListLoading: false,
      multipleSelection: []
    }
  },
  computed: {},
  mounted() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.getDeviceList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getDeviceList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getDeviceList()
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    getDeviceList: function() {
      this.getDeviceListLoading = true
      this.$store.dispatch('device/queryDevices', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        status: this.online
      })
        .then(data => {
          this.total = data.total
          this.deviceList = data.list
        }).finally(() => {
          this.getDeviceListLoading = false
      })
    },
    openDialog: function(callback) {
      this.listChangeCallback = callback
      this.showDialog = true
    },
    onSubmit: function() {
      if (this.listChangeCallback) {
        this.listChangeCallback(this.multipleSelection)
      }
      this.showDialog = false
    },
    close: function() {
      this.showDialog = false
    }

  }
}
</script>
<style>
.el-dialog__body{
  padding: 20px;
}
</style>
