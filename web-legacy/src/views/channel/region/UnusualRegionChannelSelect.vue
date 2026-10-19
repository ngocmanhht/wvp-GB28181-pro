<template>
  <div id="gbChannelSelect" v-loading="getChannelListLoading">
    <el-dialog
      v-el-drag-dialog
      title="Abnormal mount channel"
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
            size="mini"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="getChannelList"
          />
        </el-form-item>
        <el-form-item label="online status">
          <el-select
            v-model="online"
            size="mini"
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
            size="mini"
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
          <el-button
            size="mini"
            type="primary"
            :loading="getChannelListLoading"
            :disabled="multipleSelection.length ===0"
            @click="clearUnusualRegion()"
          >Clear</el-button>
          <el-button
            size="mini"
            :loading="getChannelListLoading"
            @click="clearUnusualRegion(true)"
          >clear all</el-button>
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
        <el-table-column prop="gbCivilCode" label="Administrative division" min-width="100" />
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
        <el-table-column label="Operation" min-width="140" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              type="text"
              icon="el-icon-plus"
              :loading="scope.row.addRegionLoading"
              @click="addRegion(scope.row)"
            >
              add
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: grid; grid-template-columns: 1fr 1fr">
        <div style="text-align: left; line-height: 32px">
          <i class="el-icon-info" /> After clearing, the channel can be added to the administrative division normally, and the corresponding administrative division node can be added automatically.。
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
  name: 'UnusualRegionChannelSelect',
  directives: { elDragDialog },
  props: [],
  data() {
    return {
      showDialog: false,
      channelList: [], // Device list
      searchStr: '',
      online: null,
      channelType: '',
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
      this.$store.dispatch('commonChanel/getUnusualCivilCodeList', {
        page: this.currentPage,
        count: this.count,
        channelType: this.channelType,
        query: this.searchStr,
        online: this.online
      })
        .then((data) => {
          this.total = data.total
          for (let i = 0; i < data.list.length; i++) {
            data.list[i]['addRegionLoading'] = false
          }
          this.channelList = data.list
        })
        .catch((error) => {
          console.error(error)
        })
        .finally(() => {
          this.getChannelListLoading = false
        })
    },
    openDialog: function() {
      this.showDialog = true
      this.initData()
    },
    close: function() {
      this.showDialog = false
    },
    clearUnusualRegion: function(all) {
      let channels = null
      if (all || this.multipleSelection.length > 0) {
        channels = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          channels.push(this.multipleSelection[i].gbId)
        }
      }
      this.$store.dispatch('commonChanel/clearUnusualCivilCodeList', {
        all: all,
        channelIds: channels
      })
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: 'Clear successfully'
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
    addRegion: function(row) {
      row.addRegionLoading = true
      this.$store.dispatch('region/description', row.gbCivilCode)
        .then((data) => {
          this.$confirm(`Confirm to add： ${data}`, 'Tips', {
            confirmButtonText: 'OK',
            cancelButtonText: 'Cancel',
            type: 'info'
          }).then(() => {
            this.$store.dispatch('region/addByCivilCode', row.gbCivilCode)
              .then((data) => {
                this.$message.success({
                  showClose: true,
                  message: 'Added successfully'
                })
                this.initData()
              })
          }).catch(() => {

          })
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          row.addRegionLoading = false
        })
    }

  }
}
</script>
