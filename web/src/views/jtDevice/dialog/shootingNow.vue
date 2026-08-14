<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="Shoot now"
      width="65%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <el-form size="small" @submit.native.prevent>
        <el-form-item>
          <el-form inline  @submit.native.prevent>
            <el-form-item style="margin-right: 14.5rem">
              <el-radio-group v-model="commandType">
                <el-radio :label="true" border>shooting</el-radio>
                <el-radio :label="false" border>Video</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-form-item>
        <el-form-item>
          <el-form inline size="small" @submit.native.prevent>
            <el-form-item label="Recording duration" v-if="!commandType">
              <el-input type="number" v-model="time" placeholder="Keep recording" style="width: 8rem"></el-input>
            </el-form-item>
            <el-form-item label="Continuous shooting" v-if="commandType">
              <el-input type="number" v-model="commandNumber" placeholder="Number of continuous shots" style="width: 4rem"></el-input>
            </el-form-item>
            <el-form-item label="Photo interval" v-if="commandType">
              <el-input type="number" v-model="time" placeholder="Minimum interval for taking photos" style="width: 8rem"></el-input>
            </el-form-item>
            <el-form-item label="Storage method">
              <el-select
                v-model="save"
                style="width: 8rem"
                placeholder="Please select storage method"
              >
                <el-option label="real time upload" :value="0" />
                <el-option label="save" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="channel">
              <el-select
                v-model="chanelId"
                style="width: 8rem"
                placeholder="Please select channel"
              >
                <el-option v-for="item in channelList" :key="item.id" :label="item.name" :value="item.channelId" />
              </el-select>
            </el-form-item>
            <el-form-item label="resolution">
              <el-select
                v-model="resolvingPower"
                style="width: 8rem"
                placeholder="Please select resolution"
              >
                <el-option label="lowest resolution" :value="0x00" />
                <el-option label="320×240" :value="0x01" />
                <el-option label="640×480" :value="0x02" />
                <el-option label="800×600" :value="0x03" />
                <el-option label="1024×768" :value="0x04" />
                <el-option label="176×144" :value="0x05" />
                <el-option label="352×288" :value="0x06" />
                <el-option label="704×288" :value="0x07" />
                <el-option label="704×576" :value="0x08" />
                <el-option label="highest resolution" :value="0xff" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="showImageConfig">Advanced configuration</el-checkbox>
            </el-form-item>
            <el-form-item style="margin-left: 2rem; text-align: right" >
              <el-button v-if="!commandType" type="danger" icon="el-icon-video-camera" @click="stopRecord()" >
                Stop recording
              </el-button>
              <el-button v-if="!commandType" type="primary" icon="el-icon-video-camera" @click="shooting()" >
                Start recording
              </el-button>
              <el-button v-if="commandType" type="primary" icon="el-icon-camera" @click="shooting()" >
                take pictures
              </el-button>
            </el-form-item>
          </el-form>
        </el-form-item>
        <el-form-item v-if="showImageConfig">
          <el-form size="small" label-width="80px" style="display: grid; grid-template-columns: 1fr 1fr; grid-gap: 0.5rem">
            <el-form-item label="quality" prop="topSpeed" >
              <el-slider v-model="quality" show-input :height="1" :marks="qualityMarks" :min="1" :max="10" :step="1"/>
            </el-form-item>
            <el-form-item label="brightness" prop="brightness">
              <el-slider v-model="brightness" show-input :height="1" :min="0" :max="255" :step="1" />
            </el-form-item>
            <el-form-item label="Contrast" prop="contrastRatio">
              <el-slider v-model="contrastRatio" show-input :height="1" :min="0" :max="127" :step="1"/>
            </el-form-item>
            <el-form-item label="saturation" prop="saturation">
              <el-slider v-model="saturation" show-input :height="1" :min="0" :max="127" :step="1"/>
            </el-form-item>
            <el-form-item label="Chroma" prop="chroma">
              <el-slider v-model="chroma" show-input :height="1" :min="0" :max="255" :step="1"/>
            </el-form-item>
          </el-form>
        </el-form-item>
      </el-form>
      <queryMediaList :phoneNumber="phoneNumber" :deviceId="deviceId" :channelList="channelList"></queryMediaList>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import queryMediaList from './queryMediaList.vue'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  components: { queryMediaList },
  props: {},
  data() {
    return {
      deviceId: null,
      phoneNumber: null,
      showDialog: false,
      queryLoading: false,
      showImageConfig: false,
      commandType: true,
      commandNumber: 1,
      time: null,
      save: 1,
      chanelId: null,
      resolvingPower: 0xff,
      qualityMarks: {
        1: 'optimal',
        10: 'worst'
      },
      quality: 1,
      brightness: 125,
      contrastRatio: 63,
      saturation: 63,
      chroma: 125,
      channelList: [],
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(phoneNumber, deviceId) {
      console.log(phoneNumber)
      this.showDialog = true
      this.phoneNumber = phoneNumber
      this.deviceId = deviceId
      this.$store.dispatch('jtDevice/queryChannels', {
        page: 1,
        count: 1000,
        deviceId: this.deviceId
      })
        .then(data => {
          this.channelList = data.list
          this.chanelId = data.list[0].channelId
        })

    },
    close: function() {
      this.showDialog = false
      this.channelList = []
      this.type = 0
      this.chanelId = null
    },
    shooting: function() {
      this.$store.dispatch('jtDevice/shooting', {
        phoneNumber: this.phoneNumber,
        shootingCommand: {
          chanelId: this.chanelId,
          command: !this.commandType? 0xFFFF : this.commandNumber,
          time: this.time,
          save: this.save,
          resolvingPower: this.resolvingPower,
          quality: this.quality,
          brightness: this.brightness,
          contrastRatio: this.contrastRatio,
          saturation: this.saturation,
          chroma: this.chroma
        }
      })
        .then( data => {
          this.$message.success({
            showClose: true,
            message: 'The message has been sent'
          })
        })
    },
    stopRecord: function() {
      this.$store.dispatch('jtDevice/shooting', {
        phoneNumber: this.phoneNumber,
        shootingCommand: {
          chanelId: this.chanelId,
          command: 0,
          time: 0,
          save: 1,
          resolvingPower: 0xff,
          quality: 0,
          brightness: 0,
          contrastRatio: 0,
          saturation: 0,
          chroma: 0
        }
      })
        .then( data => {
          this.$message.success({
            showClose: true,
            message: 'The message has been sent'
          })
        })
    }
  }
}
</script>

<style scoped>
>>> .el-upload {
  width: 100% !important;
}
>>> .el-slider__marks-text {
  margin-top: -36px;
  font-size: 12px;
  width: 2rem !important;
}
</style>
