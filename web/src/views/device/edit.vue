<template>
  <div id="deviceEdit" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="Device editing"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 50px;">
        <el-form ref="form" :rules="rules" :model="form" label-width="100px">
          <el-form-item label="Device number" prop="deviceId">
            <el-input v-if="isEdit" v-model="form.deviceId" disabled />
            <el-input v-if="!isEdit" v-model="form.deviceId" clearable />
          </el-form-item>

          <el-form-item label="Device name" prop="name">
            <el-input v-model="form.name" clearable />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input v-model="form.password" clearable />
          </el-form-item>
          <el-form-item label="collect flowIP" prop="sdpIp">
            <el-input v-model="form.sdpIp" type="sdpIp" clearable />
          </el-form-item>
          <el-form-item label="streaming mediaID" prop="mediaServerId">
            <el-select v-model="form.mediaServerId" style="float: left; width: 100%">
              <el-option key="auto" label="Automatic load minimum" value="auto" />
              <el-option
                v-for="item in mediaServerList"
                :key="item.id"
                :label="item.id"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="character set" prop="charset">
            <el-select v-model="form.charset" style="float: left; width: 100%">
              <el-option key="GB2312" label="GB2312" value="gb2312" />
              <el-option key="UTF-8" label="UTF-8" value="utf-8" />
            </el-select>
          </el-form-item>
          <el-form-item label="coordinate system" prop="geoCoordSys">
            <el-select v-model="form.geoCoordSys" style="float: left; width: 100%">
              <el-option key="WGS84" label="WGS84" value="gb2312" />
              <el-option key="GCJ02" label="GCJ02" value="utf-8" />
            </el-select>
          </el-form-item>
          <el-form-item label="Other options">
            <el-checkbox v-model="form.ssrcCheck" label="SSRCVerification" style="float: left" />
            <el-checkbox v-model="form.asMessageChannel" label="as a message channel" style="float: left" />
            <el-checkbox v-model="form.broadcastPushAfterAck" label="Send stream after receiving ACK" style="float: left" />
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit">Confirm</el-button>
              <el-button @click="close">Cancel</el-button>
            </div>

          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'DeviceEdit',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      hostNames: [],
      mediaServerList: [], // List of dead nodes
      form: {},
      isEdit: false,
      rules: {
        deviceId: [{ required: true, message: 'Please enter the device number', trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(row, callback) {
      console.log(row)
      this.showDialog = true
      this.isEdit = false
      if (row) {
        this.isEdit = true
      }
      this.form = {}
      this.listChangeCallback = callback
      if (row != null) {
        this.form = row
      }
      this.getMediaServerList()
    },
    getMediaServerList: function() {
      this.$store.dispatch('server/getOnlineMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
    },
    onSubmit: function() {
      if (this.isEdit) {
        this.$store.dispatch('device/update', this.form)
          .then((data) => {
            this.listChangeCallback()
          })
      } else {
        this.$store.dispatch('device/add', this.form)
          .then((data) => {
            this.listChangeCallback()
          })
      }
    },
    close: function() {
      this.showDialog = false
      this.$refs.form.resetFields()
    }
  }
}
</script>
