<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 60%; margin: 0 auto">
        <el-form-item label="Live streaming encoding mode" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.liveStreamCodeRateType"
            style="width: 100%"
            placeholder="Please select live streaming encoding mode"
          >
            <el-option label="CBR( Fixed code rate)" :value="0" />
            <el-option label="BR( variable code rate)" :value="1" />
            <el-option label="ABR( average code rate)" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="Live stream resolution" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.liveStreamResolving"
            style="width: 100%"
            placeholder="Please select a live stream resolution"
          >
            <el-option label="QCIF( 164×144 )" :value="0" />
            <el-option label="CIF( 360×288 )" :value="1" />
            <el-option label="WCIF( 480×288 )" :value="2" />
            <el-option label="D1( 720x576 )" :value="3" />
            <el-option label="WD1( 960×576 )" :value="4" />
            <el-option label="720P( 1280×720 )" :value="5" />
            <el-option label="1080P( 1920×1080 )" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="Live stream keyframe interval" prop="chroma">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.liveStreamIInterval" show-input :min="1" :max="1000" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="Live streaming target bitrate" prop="liveStreamFrameRate">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.liveStreamFrameRate" show-input :min="1" :max="120" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="Live streaming target bitrate( kbps)" prop="liveStreamCodeRate">
          <el-input type="number" v-model="form.videoParam.liveStreamCodeRate" />
        </el-form-item>


        <el-form-item label="Storage stream encoding mode" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.storageStreamCodeRateType"
            style="width: 100%"
            placeholder="Please select storage stream encoding mode"
          >
            <el-option label="CBR( Fixed code rate)" :value="0" />
            <el-option label="BR( variable code rate)" :value="1" />
            <el-option label="ABR( average code rate)" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="Storage stream resolution" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.storageStreamResolving"
            style="width: 100%"
            placeholder="Please select storage stream resolution"
          >
            <el-option label="QCIF( 164×144 )" :value="0" />
            <el-option label="CIF( 360×288 )" :value="1" />
            <el-option label="WCIF( 480×288 )" :value="2" />
            <el-option label="D1( 720x576 )" :value="2" />
            <el-option label="WD1( 960×576 )" :value="2" />
            <el-option label="720P( 1280×720 )" :value="2" />
            <el-option label="1080P( 1920×1080 )" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="Storage stream keyframe interval" prop="chroma">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.storageStreamIInterval" show-input :min="1" :max="1000" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="Storage stream target frame rate" prop="liveStreamFrameRate">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.storageStreamFrameRate" show-input :min="1" :max="120" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="Storage stream target bitrate(kbps)" prop="liveStreamCodeRate">
          <el-input type="number" v-model="form.videoParam.storageStreamCodeRate" />
        </el-form-item>
        <el-form-item label="Special alarm video storage threshold(Percentage)" prop="storageLimit">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.alarmRecordingParam.storageLimit" show-input :min="1" :max="99" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="Special alarm recording duration(minutes)" prop="duration">
          <el-input type="number" v-model="form.videoParam.duration" />
        </el-form-item>
        <el-form-item label="Special alarm identification start time(minutes)" prop="startTime">
          <el-input type="number" v-model="form.videoParam.startTime" />
        </el-form-item>
        <el-form-item label="audio output" prop="startTime">
          <el-checkbox label="enable" v-model="form.videoParam.audioEnable" ></el-checkbox>
        </el-form-item>
        <el-form-item label="OSDSubtitle overlay settings" prop="osd">
          <el-checkbox label="date and time" v-model="form.videoParam.osd.time" ></el-checkbox>
          <el-checkbox label="license plate number" v-model="form.videoParam.osd.licensePlate" ></el-checkbox>
          <el-checkbox label="Logical channel number" v-model="form.videoParam.osd.channelId" ></el-checkbox>
          <el-checkbox label="Latitude and longitude" v-model="form.videoParam.osd.position" ></el-checkbox>
          <el-checkbox label="Driving record speed" v-model="form.videoParam.osd.speed" ></el-checkbox>
          <el-checkbox label="Satellite positioning speed" v-model="form.videoParam.osd.speedForGPS" ></el-checkbox>
          <el-checkbox label="continuous driving time" v-model="form.videoParam.osd.drivingTime" ></el-checkbox>
        </el-form-item>
      </el-form>
    </div>
    <p style="text-align: right">
      <el-button type="primary" @click="onSubmit">Confirm</el-button>
      <el-button @click="showDevice">Cancel</el-button>
    </p>

  </div>
</template>

<script>

export default {
  name: 'communication',
  components: {
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {},
      qualityMarks: {
        1: 'optimal',
        10: 'worst'
      },
      isLoading: false
    }
  },

  mounted() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.isLoading = true
      this.$store.dispatch('jtDevice/queryConfig', this.phoneNumber)
        .then((data) => {
          if (!data.videoParam) {
            data.videoParam = {
              osd: {}
            }
          }
          if (!data.alarmRecordingParam) {
            data.alarmRecordingParam = {}
          }
          this.form = data
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    onSubmit: function() {
      this.$emit('submit', this.form)
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
<style scoped>
  >>> .el-slider__marks-text {
    margin-top: 6px;
    font-size: 12px;
    width: 2rem !important;
}
</style>
