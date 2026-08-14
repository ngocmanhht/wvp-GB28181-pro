<template>
  <div id="addStreamProxy" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title=" Join"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="streamProxy" :rules="rules" :model="proxyParam" label-width="140px">
          <el-form-item label="Name" prop="name">
            <el-input v-model="proxyParam.name" clearable />
          </el-form-item>
          <el-form-item label="Streaming application name" prop="app">
            <el-input v-model="proxyParam.app" clearable :disabled="edit" />
          </el-form-item>
          <el-form-item label="flowID" prop="stream">
            <el-input v-model="proxyParam.stream" clearable :disabled="edit" />
          </el-form-item>
          <el-form-item label="National standard code" prop="gbId">
            <el-input v-model="proxyParam.gbId" placeholder="Setting the national standard code can be pushed to the national standard" clearable />
          </el-form-item>
          <el-form-item v-if="proxyParam.gbId" label="longitude" prop="longitude">
            <el-input v-model="proxyParam.longitude" placeholder="longitude" clearable />
          </el-form-item>
          <el-form-item v-if="proxyParam.gbId" label="Latitude" prop="latitude">
            <el-input v-model="proxyParam.latitude" placeholder="longitude" clearable />
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit">save</el-button>
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
  name: 'PushStreamEdit',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      edit: false,
      proxyParam: {
        name: null,
        app: null,
        stream: null,
        gbId: null,
        longitude: null,
        latitude: null
      },
      rules: {
        name: [{ required: true, message: 'Please enter name', trigger: 'blur' }],
        app: [{ required: true, message: 'Please enter application name', trigger: 'blur' }],
        stream: [{ required: true, message: 'Please enter the streamID', trigger: 'blur' }],
        gbId: [{ required: true, message: 'Please enter the national standard code', trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(proxyParam, callback) {
      this.showDialog = true
      this.listChangeCallback = callback
      if (proxyParam != null) {
        this.proxyParam = proxyParam
        this.edit = true
      } else {
        this.proxyParam = {
          name: null,
          app: null,
          stream: null,
          gbId: null,
          longitude: null,
          latitude: null
        }
        this.edit = false
      }
    },
    onSubmit: function() {
      console.log('onSubmit')
      if (this.edit) {
        this.$store.dispatch('streamPush/saveToGb', this.proxyParam)
          .then((data) => {
            this.$message({
              showClose: true,
              message: 'Saved successfully',
              type: 'success'
            })
            this.showDialog = false
            if (this.listChangeCallback != null) {
              this.listChangeCallback()
            }
          })
      } else {
        this.$store.dispatch('streamPush/add', this.proxyParam)
          .then((data) => {
            this.$message({
              showClose: true,
              message: 'Saved successfully',
              type: 'success'
            })
            this.showDialog = false
            if (this.listChangeCallback != null) {
              this.listChangeCallback()
            }
          })
      }
    },
    close: function() {
      console.log('Close joinGB')
      this.showDialog = false
      this.$refs.streamProxy.resetFields()
    }
  }
}
</script>
