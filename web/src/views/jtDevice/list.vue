<template>
  <div id="app" style="height: calc(100vh - 124px);">
    <el-form :inline="true" size="mini">
      <el-form-item label="Search">
        <el-input
          v-model="searchStr"
          style="margin-right: 1rem; width: auto;"
          placeholder="Keywords"
          prefix-icon="el-icon-search"
          clearable
          @input="initData"
        />
      </el-form-item>
      <el-form-item label="online status">
        <el-select
          v-model="online"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="Please select"
          default-first-option
          @change="initData"
        >
          <el-option label="All" value="" />
          <el-option label="online" value="true" />
          <el-option label="Offline" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">new equipment</el-button>
        <el-button icon="el-icon-info" style="margin-right: 1rem;" @click="showInfo()">access information</el-button>
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button
          icon="el-icon-refresh-right"
          circle
          :loading="getListLoading"
          @click="getList()"
        />
      </el-form-item>
    </el-form>
    <!--Device list-->
    <el-table
      size="small"
      :data="deviceList"
      height="calc(100% - 64px)"
      header-row-class-name="table-header"
    >
      <el-table-column prop="phoneNumber" label="Terminal mobile phone number" min-width="120" />
      <el-table-column prop="terminalId" label="terminalID" min-width="120" />
      <el-table-column label="Provincial area" min-width="120" >
        <template slot-scope="scope">
          {{scope.row.provinceText || scope.row.provinceId}}
        </template>
      </el-table-column>
      <el-table-column label="City and county" min-width="120" >
        <template slot-scope="scope">
          {{scope.row.cityText || scope.row.cityId}}
        </template>
      </el-table-column>
      <el-table-column prop="makerId" label="manufacturer" min-width="120" />
      <el-table-column prop="model" label="Model" min-width="120" />
      <el-table-column label="license plate color" min-width="120">
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <span v-if="scope.row.plateColor === 1">blue</span>
            <span v-else-if="scope.row.plateColor === 2">yellow</span>
            <span v-else-if="scope.row.plateColor === 3">black</span>
            <span v-else-if="scope.row.plateColor === 4">white</span>
            <span v-else-if="scope.row.plateColor === 5">green</span>
            <span v-else-if="scope.row.plateColor === 91">Farm yellow</span>
            <span v-else-if="scope.row.plateColor === 92">Farm green</span>
            <span v-else-if="scope.row.plateColor === 93">Yellow-green</span>
            <span v-else-if="scope.row.plateColor === 94">gradient green</span>
            <span v-else>Not listed</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="plateNo" label="license plate" min-width="120" />
      <el-table-column prop="registerTime" label="Registration time" min-width="160" />
      <el-table-column label="Status" min-width="120">
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag v-if="scope.row.status" size="medium">online</el-tag>
            <el-tag v-if="!scope.row.status" size="medium" type="info">Offline</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Operation" min-width="340" fixed="right">
        <template slot-scope="scope">
          <el-divider direction="vertical" />
          <el-button
            type="text"
            size="medium"
            icon="el-icon-video-camera"
            @click="showChannelList(scope.row)"
          >channel
          </el-button>
          <el-divider direction="vertical" />
          <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">Edit</el-button>
          <el-divider direction="vertical" />
          <el-button
            size="medium"
            icon="el-icon-delete"
            type="text"
            style="color: #f56c6c"
            @click="deleteDevice(scope.row)"
          >Delete
          </el-button>
          <el-divider direction="vertical" />
          <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
            <el-button size="medium" type="text">
              More features<i class="el-icon-arrow-down el-icon--right" />
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="params" :disabled="!scope.row.status">
                Terminal parameters</el-dropdown-item>
              <el-dropdown-item command="attribute" v-bind:disabled="!scope.row.status">
                terminal properties</el-dropdown-item>
              <el-dropdown-item command="mediaAttribute" v-bind:disabled="!scope.row.status">
                Audio and video properties</el-dropdown-item>
              <el-dropdown-item command="linkDetection" v-bind:disabled="!scope.row.status" >
                Link detection</el-dropdown-item>
              <el-dropdown-item command="position" v-bind:disabled="!scope.row.status" >
                location information</el-dropdown-item>
              <el-dropdown-item command="textMsg" v-bind:disabled="!scope.row.status" >
                Text delivery</el-dropdown-item>
              <el-dropdown-item command="telephoneCallback" v-bind:disabled="!scope.row.status" >
                Call back</el-dropdown-item>
              <el-dropdown-item command="setPhoneBook" v-bind:disabled="!scope.row.status" >
                Set up phone book</el-dropdown-item>
              <el-dropdown-item command="driverInfo" v-bind:disabled="!scope.row.status" >
                driver information</el-dropdown-item>
              <el-dropdown-item command="reset" v-bind:disabled="!scope.row.status" >
                terminal reset</el-dropdown-item>
              <el-dropdown-item command="factoryReset" v-bind:disabled="!scope.row.status" >
                Factory reset</el-dropdown-item>
              <el-dropdown-item command="connection" v-bind:disabled="!scope.row.status" >
                Connect to the specified server</el-dropdown-item>
              <el-dropdown-item command="door" v-bind:disabled="!scope.row.status" >
                door control</el-dropdown-item>
              <el-dropdown-item command="shooting" v-bind:disabled="!scope.row.status" >
                Shoot now</el-dropdown-item>
              <el-dropdown-item command="queryMediaList" v-bind:disabled="!scope.row.status" >
                multimedia search</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="float: right"
      :current-page="currentPage"
      :page-size="count"
      :page-sizes="[15, 25, 35, 50]"
      layout="total, sizes, prev, pager, next"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="currentChange"
    />
    <deviceEdit ref="deviceEdit" />
    <configInfo ref="configInfo" />
    <attribute ref="attribute" />
    <position ref="position" />
    <textMsg ref="textMsg" />
    <telephoneCallback ref="telephoneCallback" />
    <driverInfo ref="driverInfo" />
    <connectionServer ref="connectionServer" />
    <controlDoor ref="controlDoor" />
    <mediaAttribute ref="mediaAttribute" />
    <phoneBook ref="phoneBook" />
    <queryMediaList ref="queryMediaList" />
    <shootingNow ref="shootingNow" />
  </div>
</template>

<script>
import deviceEdit from './edit.vue'
import configInfo from '../dialog/configInfo.vue'
import attribute from './dialog/attribute.vue'
import position from './dialog/position.vue'
import textMsg from './dialog/textMsg.vue'
import telephoneCallback from './dialog/telephoneCallback.vue'
import driverInfo from './dialog/driverInfo.vue'
import connectionServer from './dialog/connectionServer.vue'
import controlDoor from './dialog/controlDoor.vue'
import mediaAttribute from './dialog/mediaAttribute.vue'
import phoneBook from './dialog/phoneBook.vue'
import queryMediaList from './dialog/queryMediaListDialog.vue'
import shootingNow from './dialog/shootingNow.vue'

export default {
  name: 'App',
  components: {
    deviceEdit, configInfo, attribute, position, textMsg, telephoneCallback, driverInfo, connectionServer, controlDoor
    , mediaAttribute, phoneBook, queryMediaList, shootingNow
  },
  data() {
    return {
      deviceList: [], // Device list
      updateLooper: 0, // Data refresh rotation training flag
      winHeight: window.innerHeight - 200,
      searchStr: '',
      online: '',
      currentPage: 1,
      count: 15,
      total: 0,
      getListLoading: false
    }
  },
  mounted() {
    this.initData()
    this.updateLooper = setInterval(this.getList, 10000)
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.currentPage = 1
      this.total = 0
      this.getList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getList()
    },
    getList: function() {
      this.getListLoading = true
      this.$store.dispatch('jtDevice/queryDevices', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online
      })
        .then(data => {
          this.total = data.total
          this.deviceList = data.list
        })
        .finally(() => {
          this.getListLoading = false
        })
    },
    deleteDevice: function(row) {
      this.$confirm('Confirm to delete this device？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('jtDevice/deleteDevice', row.phoneNumber)
          .then(data => {
            this.getList()
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
      this.$refs.deviceEdit.openDialog(row, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: 'The device modification is successful and the channel character set will take effect in the next update.',
          type: 'success'
        })
        setTimeout(this.getList, 200)
      })
    },
    showChannelList: function(row) {
      console.log(row)
      this.$emit('show-channel', row.id)
    },
    showParam: function(row) {
      this.$emit('show-param', row.phoneNumber)
    },
    add: function() {
      this.$refs.deviceEdit.openDialog(null, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: 'Added successfully',
          type: 'success'
        })
        setTimeout(this.getList, 200)
      })
    },
    moreClick: function(command, itemData) {
      if (command === 'params') {
        this.showParam(itemData)
      } else if (command === 'attribute') {
         this.queryAttribute(itemData)
      } else if (command === 'linkDetection') {
         this.linkDetection(itemData)
      } else if (command === 'position') {
         this.queryPosition(itemData)
      } else if (command === 'textMsg') {
         this.sendTextMsg(itemData)
      } else if (command === 'telephoneCallback') {
         this.telephoneCallback(itemData)
      } else if (command === 'driverInfo') {
         this.queryDriverInfo(itemData)
      } else if (command === 'factoryReset') {
         this.factoryReset(itemData)
      } else if (command === 'reset') {
         this.reset(itemData)
      } else if (command === 'door') {
         this.controlDoor(itemData)
      } else if (command === 'connection') {
         this.connection(itemData)
      } else if (command === 'mediaAttribute') {
         this.queryMediaAttribute(itemData)
      } else if (command === 'setPhoneBook') {
         this.setPhoneBook(itemData)
      } else if (command === 'queryMediaList') {
         this.queryMediaList(itemData)
      } else if (command === 'shooting') {
         this.shootingNow(itemData)
      } else {
        this.$message.info('Not supported yet')
      }
    },
    showInfo: function() {
      this.$store.dispatch('server/getSystemConfig')
        .then((data) => {
          this.serverId = data.addOn.serverId
          this.$refs.configInfo.openDialog(data, 'jt1078Config')
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    queryAttribute: function(itemData) {
      this.$store.dispatch('jtDevice/queryAttribute', itemData.phoneNumber)
        .then((data) => {
          this.$refs.attribute.openDialog(data)
        })
    },
    queryPosition: function(itemData) {
      this.$store.dispatch('jtDevice/queryPosition', itemData.phoneNumber)
        .then((data) => {
          this.$refs.position.openDialog(data)
        })
    },
    sendTextMsg: function(itemData) {
      this.$refs.textMsg.openDialog(itemData)
    },
    telephoneCallback: function(itemData) {
      this.$refs.telephoneCallback.openDialog(itemData)
    },
    queryDriverInfo: function(itemData) {
      this.$store.dispatch('jtDevice/queryDriverInfo', itemData.phoneNumber)
          .then(data => {
            this.$refs.driverInfo.openDialog(data)
          })
    },
    factoryReset: function(itemData) {
      this.$confirm('Confirm to restore factory settings', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('jtDevice/factoryReset', itemData.phoneNumber)
            .then(data => {
              this.$message.success({
                showClose: true,
                message: 'The message has been sent'
              })
            })
      }).catch(() => {

      })
    },
    reset: function(itemData) {
      this.$confirm('OK to start terminal reset', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('jtDevice/reset', itemData.phoneNumber)
            .then(data => {
              this.$message.success({
                showClose: true,
                message: 'The message has been sent'
              })
            })
      }).catch(() => {

      })
    },
    connection: function(itemData) {
      this.$refs.connectionServer.openDialog(itemData.phoneNumber)
    },
    setPhoneBook: function(itemData) {
      this.$refs.phoneBook.openDialog(itemData.phoneNumber)
    },
    queryMediaList: function(itemData) {
      this.$refs.queryMediaList.openDialog(itemData.phoneNumber, itemData.id)
    },
    queryMediaAttribute: function(itemData) {
      this.$store.dispatch('jtDevice/queryMediaAttribute', itemData.phoneNumber)
        .then((data) => {
          this.$refs.mediaAttribute.openDialog(data)
        })
    },
    controlDoor: function(itemData) {
      this.$refs.controlDoor.openDialog(itemData.phoneNumber)
    },
    shootingNow: function(itemData) {
      this.$refs.shootingNow.openDialog(itemData.phoneNumber, itemData.id)
    },
    linkDetection: function(itemData) {
      this.$store.dispatch('jtDevice/linkDetection', itemData.phoneNumber)
        .then((data) => {
          if (data === 0) {
            this.$message.success({
              showClose: true,
              message: 'success'
            })
          }else if (data === 1) {
            this.$message.error({
              showClose: true,
              message: 'failed'
            })
          }else if (data === 2) {
            this.$message.error({
              showClose: true,
              message: 'The message is wrong'
            })
          }else if (data === 3) {
            this.$message.error({
              showClose: true,
              message: 'This message is not supported'
            })
          }
        })
    }
  }
}
</script>
