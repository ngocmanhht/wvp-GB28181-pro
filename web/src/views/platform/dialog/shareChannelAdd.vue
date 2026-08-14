<template>
  <div id="shareChannelAdd" style="background-color: #FFFFFF; display: grid; grid-template-columns: 83px minmax(0, 1fr);">
    <el-tabs v-model="hasShare" tab-position="left" style="" @tab-click="search">
      <el-tab-pane label="Not shared" name="false" />
      <el-tab-pane label="Shared" name="true" />
    </el-tabs>
    <div style="padding: 0 2rem">
      <el-form :inline="true" size="mini">
        <el-form-item label="Search">
          <el-input
            v-model="searchStr"
            style="margin-right: 1rem; width: auto;"
            size="mini"
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
            size="mini"
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
          <el-button v-if="hasShare !=='true'" size="mini" type="primary" :loading="addLoading" @click="add()">
            add
          </el-button>
          <el-button v-if="hasShare ==='true'" size="mini" type="danger" :loading="removeLoading" @click="remove()">
            Remove
          </el-button>
          <el-button v-if="hasShare !=='true'" size="mini" :loading="addByDeviceLoading" @click="addByDevice()">Add by device</el-button>
          <el-button v-if="hasShare ==='true'" size="mini" :loading="removeByDeviceLoading" @click="removeByDevice()">Remove by device</el-button>
          <el-button v-if="hasShare !=='true'" size="mini" :loading="addAllLoading" @click="addAll()">add all</el-button>
          <el-button v-if="hasShare ==='true'" size="mini" :loading="removeAllLoading" @click="removeAll()">Remove all</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="getChannelList()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="channelListTable"
        size="small"
        :data="channelList"
        :height="winHeight"
        header-row-class-name="table-header"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" :selectable="selectable" />
        <el-table-column prop="gbName" label="Name" min-width="180" />
        <el-table-column prop="gbDeviceId" label="No." min-width="180" />
        <el-table-column v-if="hasShare ==='true'" label="custom name" min-width="180">
          <template v-slot:default="scope">
            <div slot="—" class="name-wrapper">
              <el-input v-model:value="scope.row.customName" size="mini" placeholder="Leave blank and press original name" />
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="hasShare ==='true'" label="Custom number" min-width="180">
          <template v-slot:default="scope">
            <div slot="—" class="name-wrapper">
              <el-input v-model:value="scope.row.customDeviceId" size="mini" placeholder="If left blank, press the original number" />
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="hasShare ==='true'" label="" min-width="80">
          <template v-slot:default="scope">
            <el-button size="mini" type="primary" @click="saveCustom(scope.row)">save
            </el-button>
          </template>
        </el-table-column>
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
</template>

<script>

import gbDeviceSelect from '../../dialog/GbDeviceSelect.vue'

export default {
  name: 'ShareChannelAdd',
  components: { gbDeviceSelect },
  props: ['platformId'],
  data() {
    return {
      channelList: [],
      searchStr: '',
      channelType: '',
      online: '',
      hasShare: 'false',
      winHeight: window.innerHeight - 300,
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
      multipleSelection: [],
      addLoading: false,
      addByDeviceLoading: false,
      addAllLoading: false,
      removeLoading: false,
      removeByDeviceLoading: false,
      removeAllLoading: false
    }
  },

  created() {
    this.initData()
  },
  destroyed() {},
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
      this.$store.dispatch('platform/getChannelList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online,
        channelType: this.channelType,
        platformId: this.platformId,
        hasShare: this.hasShare
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
    selectable: function(row, rowIndex) {
      if (this.hasShare === '') {
        if (row.platformId) {
          return false
        } else {
          return true
        }
      } else {
        return true
      }
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
      this.addLoading = true
      this.$store.dispatch('platform/addChannel', {
        platformId: this.platformId,
        channelIds: channels
      })
        .then(() => {
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
          this.addLoading = false
        })
    },
    addAll: function(row) {
      this.$confirm('Confirm to add all？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.addAllLoading = true
        this.$store.dispatch('platform/addChannel', {
          platformId: this.platformId,
          all: true
        })
          .then(() => {
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
            this.addAllLoading = false
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
        this.addByDeviceLoading = true
        this.$store.dispatch('platform/addChannelByDevice', {
          platformId: this.platformId,
          deviceIds: deviceIds
        })
          .then(() => {
            this.$message.success({
              showClose: true,
              message: 'Saved successfully'
            })
            this.initData()
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
          })
          .finally(() => {
            this.addByDeviceLoading = false
          })
      })
    },

    removeByDevice: function(row) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.removeByDeviceLoading = true
        this.$store.dispatch('platform/removeChannelByDevice', {
          platformId: this.platformId,
          deviceIds: deviceIds
        })
          .then(() => {
            this.$message.success({
              showClose: true,
              message: 'Saved successfully'
            })
            this.initData()
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
          })
          .finally(() => {
            this.removeByDeviceLoading = false
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
      this.removeLoading = true
      this.$store.dispatch('platform/removeChannel', {
        platformId: this.platformId,
        channelIds: channels
      })
        .then(() => {
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
          this.removeLoading = false
        })
    },
    removeAll: function(row) {
      this.$confirm('Confirm to remove all？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.removeAllLoading = true
        this.$store.dispatch('platform/removeChannel', {
          platformId: this.platformId,
          all: true
        })
          .then(() => {
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
            this.removeAllLoading = false
          })
      }).catch(() => {
      })
    },
    saveCustom: function(row) {
      this.$store.dispatch('platform/updateCustomChannel', row)
        .then(() => {
          this.$message.success({
            showClose: true,
            message: 'Saved successfully'
          })
          this.initData()
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
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
