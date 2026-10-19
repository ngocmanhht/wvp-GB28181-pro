<template>
  <div id="gbChannelSelect" v-loading="getChannelListLoading">
    <el-dialog
      v-el-drag-dialog
      title="Add national standard channel"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      append-to-body
      @close="close()"
    >
      <el-form :inline="true" size="mini">
        <el-form-item label="Search">
          <el-input
            v-model="searchStr"
            style="margin-right: 1rem; width: auto;"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="getChannelList"
          />
        </el-form-item>
        <el-form-item label="online status">
          <el-select
            v-model="online"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="getChannelList"
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
            @change="getChannelList"
          >
            <el-option label="All" value="" />
            <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="float: right" @click="onSubmit">OK</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle :loading="getChannelListLoading" @click="getChannelList()" />
        </el-form-item>
      </el-form>
      <!--Channel list-->
      <el-table
        ref="channelListTable"
        size="small"
        :data="channelList"
        :height="winHeight"
        style="width: 100%;"
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
      <div style="display: grid; grid-template-columns: 1fr 1fr">
        <div style="text-align: left; line-height: 32px">
          <i class="el-icon-info" /> Channel not found, can be found in the national standard equipment/Select the edit button in the channel and select{{ dataType === 'civilCode'?'Administrative division':'Parent node encoding' }}
        </div>
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
      </div>

    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'GbChannelSelect',
  directives: { elDragDialog },
  props: ['dataType', 'selected'],
  data() {
    return {
      showDialog: false,
      channelList: [], // Device list
      currentDevice: {}, // Current operating device object
      searchStr: '',
      online: null,
      channelType: '',
      videoComponentList: [],
      updateLooper: 0, // Data refresh rotation training flag
      currentDeviceChannelsLenth: 0,
      winHeight: 580,
      currentPage: 1,
      count: 10,
      total: 0,
      getChannelListLoading: false,
      multipleSelection: []
    }
  },
  computed: {},
  methods: {
    initData: function() {
      this.getChannelList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getChannelList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getChannelList()
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    getChannelList: function() {
      this.getChannelListLoading = true
      if (this.dataType === 'civilCode') {
        this.$store.dispatch('commonChanel/getCivilCodeList', {
          page: this.currentPage,
          count: this.count,
          channelType: this.channelType,
          query: this.searchStr,
          online: this.online
        })
          .then(data => {
            this.total = data.total
            this.channelList = data.list
          }).finally(() => {
            this.getChannelListLoading = false
          })
      } else {
        this.$store.dispatch('commonChanel/getParentList', {
          page: this.currentPage,
          count: this.count,
          query: this.searchStr,
          channelType: this.channelType,
          online: this.online
        })
          .then(data => {
            this.total = data.total
            this.channelList = data.list
          }).finally(() => {
            this.getChannelListLoading = false
          })
      }
    },
    openDialog: function(callback) {
      this.listChangeCallback = callback
      this.showDialog = true
      this.initData()
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
