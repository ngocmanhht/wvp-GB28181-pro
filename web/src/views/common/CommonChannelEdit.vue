<template>
  <div id="CommonChannelEdit" v-loading="loading" style="width: 100%; height: calc(-218px + 100vh); overflow: auto;">
    <el-form ref="channelForm" :model="form" :rules="rules" status-icon label-width="160px" class="channel-form" size="medium">
      <div class="form-box">
        <el-form-item label="Name" prop="gbName">
          <el-input v-model="form.gbName" placeholder="Please enter channel name" />
        </el-form-item>
        <el-form-item label="encoding" prop="gbDeviceId">
          <el-input v-model="form.gbDeviceId" placeholder="Please enter channel code">
            <template v-slot:append>
              <el-button @click="buildDeviceIdCode(form.gbDeviceId)">generate</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="Equipment manufacturer">
          <el-input v-model="form.gbManufacturer" placeholder="Please enter the device manufacturer" />
        </el-form-item>
        <el-form-item label="Device model">
          <el-autocomplete
            style="width: 100%;"
            v-model="form.gbModel"
            value-key="name"
            :fetch-suggestions="queryModel"
            placeholder="Please enter content"
          >
            <template slot-scope="{ item }">
              <span class="addr">{{ item.name }}（{{ item.alias }}）</span>
            </template>
          </el-autocomplete>
        </el-form-item>

        <el-form-item label="Administrative region">
          <el-input v-model="form.gbCivilCode" placeholder="Please enter administrative region" @change="getRegionPaths">
            <template v-slot:append>
              <el-button @click="chooseCivilCode()">Choose</el-button>
            </template>
          </el-input>
          <el-breadcrumb v-if="regionPath.length > 0" separator="/" style="display: block; margin-top: 8px; font-size: 14px;">
            <el-breadcrumb-item v-for="key in regionPath" :key="key">{{ key }}</el-breadcrumb-item>
          </el-breadcrumb>
        </el-form-item>

        <el-form-item label="Installation address">
          <el-input v-model="form.gbAddress" placeholder="Please enter the installation address" />
        </el-form-item>
        <el-form-item label="Monitoring position">
          <el-select v-model="form.gbDirectionType" style="width: 100%" placeholder="Please select a surveillance location">
            <el-option label="East(west to east)" :value="1" />
            <el-option label="west(east to west)" :value="2" />
            <el-option label="South(north to south)" :value="3" />
            <el-option label="north(south to north)" :value="4" />
            <el-option label="Southeast(northwest to southeast)" :value="5" />
            <el-option label="Northeast(Southwest to Northeast)" :value="6" />
            <el-option label="Southwest(Northeast to Southwest)" :value="7" />
            <el-option label="Northwest(southeast to northwest)" :value="8" />
            <el-option label="left(Non-standard)" :value="91" />
            <el-option label="after(Non-standard)" :value="92" />
            <el-option label="before(Non-standard)" :value="93" />
            <el-option label="right(Non-standard)" :value="94" />
            <el-option label="left front(Non-standard)" :value="95" />
            <el-option label="right front(Non-standard)" :value="96" />
            <el-option label="rear left(Non-standard)" :value="97" />
            <el-option label="right rear(Non-standard)" :value="98" />
          </el-select>
        </el-form-item>

        <el-form-item label="Parent node encoding">
          <el-input v-model="form.gbParentId" placeholder="Please enter the parent node code or select the virtual organization to which it belongs." @change="getPaths">
            <template v-slot:append>
              <el-button @click="chooseGroup()">Choose</el-button>
            </template>
          </el-input>
          <el-breadcrumb v-if="parentPath.length > 0" separator="/" style="display: block; margin-top: 8px; font-size: 14px;">
            <el-breadcrumb-item v-for="key in parentPath" :key="key">{{ key }}</el-breadcrumb-item>
          </el-breadcrumb>
        </el-form-item>
        <el-form-item label="Device status">
          <el-select v-model="form.gbStatus" style="width: 100%" placeholder="Please select device status">
            <el-option label="online" value="ON" />
            <el-option label="Offline" value="OFF" />
          </el-select>
        </el-form-item>
        <el-form-item label="longitude">
          <el-input v-model="form.gbLongitude" placeholder="Please enter longitude" />
        </el-form-item>
        <el-form-item label="Latitude">
          <el-input v-model="form.gbLatitude" placeholder="Please enter latitude" />
        </el-form-item>
        <el-form-item label="Camera type">
          <el-select v-model="form.gbPtzType" style="width: 100%" placeholder="Please select camera type">
            <el-option label="ball machine" :value="1" />
            <el-option label="hemisphere" :value="2" />
            <el-option label="Fixed bolt" :value="3" />
            <el-option label="remote control gun" :value="4" />
            <el-option label="remote controlled hemisphere" :value="5" />
            <el-option label="Panoramic view of multi-view equipment/Splicing channel" :value="6" />
            <el-option label="Split channels for multi-channel equipment" :value="7" />
            <el-option label="Mobile devices (non-standard）" :value="99" />
            <el-option label="Conference equipment (non-standard）" :value="98" />
          </el-select>
        </el-form-item>
      </div>
      <div>
        <el-form-item label="Business group number">
          <el-input v-model="form.gbBusinessGroupId" placeholder="Please enter the business group number" @change="getPaths"/>
        </el-form-item>
        <el-form-item label="police district">
          <el-input v-model="form.gbBlock" placeholder="Please enter police district" />
        </el-form-item>
        <el-form-item label="Signaling security mode">
          <el-select v-model="form.gbSafetyWay" style="width: 100%" placeholder="Please select signaling security mode">
            <el-option label="Not adopted" :value="0" />
            <el-option label="S/MIMEsignature" :value="2" />
            <el-option label="S/MIMEEncrypted signatures are also used" :value="3" />
            <el-option label="digital summary" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="Registration method">
          <el-select v-model="form.gbRegisterWay" style="width: 100%" placeholder="Please select registration method">
            <el-option label="IETFRFC3261Standard" :value="1" />
            <el-option label="Password-based two-way authentication" :value="2" />
            <el-option label="Two-way authentication registration based on digital certificate" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="Certificate serial number">
          <el-input v-model="form.gbCertNum" type="number" placeholder="Please enter the certificate serial number" />
        </el-form-item>
        <el-form-item label="Certificate valid identifier">
          <el-select v-model="form.gbCertifiable" style="width: 100%" placeholder="Please select a valid ID for the certificate">
            <el-option label="valid" :value="1" />
            <el-option label="Invalid" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="Invalid reason code">
          <el-input v-model="form.gbCertNum" type="errCode" placeholder="Please enter an invalid reason code" />
        </el-form-item>
        <el-form-item label="Certificate expiry date">
          <el-date-picker
            v-model="form.gbEndTime"
            type="datetime"
            placeholder="Select date time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Confidential attribute">
          <el-select v-model="form.gbSecrecy" style="width: 100%" placeholder="Please select confidentiality attribute">
            <el-option label="Not confidential" :value="0" />
            <el-option label="Confidential" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="IPaddress">
          <el-input v-model="form.gbIpAddress" placeholder="Please enter IP address" />
        </el-form-item>
        <el-form-item label="port">
          <el-input v-model="form.gbPort" type="number" placeholder="Please enter the port" />
        </el-form-item>
        <el-form-item label="Device password">
          <el-input v-model="form.gbPassword" placeholder="Please enter device password" />
        </el-form-item>
      </div>
      <div>
        <el-form-item label="Equipment ownership">
          <el-input v-model="form.gbOwner" placeholder="Please enter the device ownership" />
        </el-form-item>
        <el-form-item label="subdevice">
          <el-select v-model="form.gbParental" style="width: 100%" placeholder="Please select whether there are sub-devices">
            <el-option label="Yes" :value="1" />
            <el-option label="None" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="location type">
          <el-select v-model="form.gbPositionType" style="width: 100%" placeholder="Please select a location type">
            <el-option label="interprovincial checkpoint" :value="1" />
            <el-option label="Party and government organs" :value="2" />
            <el-option label="Station Pier" :value="3" />
            <el-option label="central square" :value="4" />
            <el-option label="sports venues" :value="5" />
            <el-option label="business center" :value="6" />
            <el-option label="religious place" :value="7" />
            <el-option label="Around campus" :value="8" />
            <el-option label="Complex security area" :value="9" />
            <el-option label="traffic arteries" :value="10" />
          </el-select>
        </el-form-item>
        <el-form-item label="outdoor/indoor">
          <el-select v-model="form.gbRoomType" style="width: 100%" placeholder="Please select a location type">
            <el-option label="outdoor" :value="1" />
            <el-option label="indoor" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="Purpose">
          <el-select v-model="form.gbUseType" style="width: 100%" placeholder="Please select usage type">
            <el-option label="law and order" :value="1" />
            <el-option label="transportation" :value="2" />
            <el-option label="focus" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="fill light">
          <el-select v-model="form.gbSupplyLightType" style="width: 100%" placeholder="Please select fill light type">
            <el-option label="No fill light" :value="1" />
            <el-option label="Infrared fill light" :value="2" />
            <el-option label="white light fill light" :value="3" />
            <el-option label="Laser fill light" :value="4" />
            <el-option label="Others" :value="9" />
          </el-select>
        </el-form-item>
        <el-form-item label="resolution">
          <el-input v-model="form.gbResolution" placeholder="Please enter resolution" />
        </el-form-item>
        <el-form-item label="Download twice as fast">
          <el-select v-model="form.gbDownloadSpeedArray" multiple style="width: 100%" placeholder="Please select download speed">
            <el-option label="1Double speed" value="1" />
            <el-option label="2Double speed" value="2" />
            <el-option label="4Double speed" value="4" />
            <el-option label="8Double speed" value="8" />
            <el-option label="16Double speed" value="16" />
          </el-select>
        </el-form-item>
        <el-form-item label="airspace coding capability">
          <el-select v-model="form.gbSvcSpaceSupportMod" style="width: 100%" placeholder="Please select airspace encoding capabilities">
            <el-option label="1level enhancement" value="1" />
            <el-option label="2level enhancement" value="2" />
            <el-option label="3level enhancement" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="Time domain coding capability">
          <el-select v-model="form.gbSvcTimeSupportMode" style="width: 100%" placeholder="Please select time domain encoding capabilities">
            <el-option label="1level enhancement" value="1" />
            <el-option label="2level enhancement" value="2" />
            <el-option label="3level enhancement" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item >
          <el-checkbox v-model="form.enableBroadcastForBool" >Voice intercom(Non-standard attributes)</el-checkbox>
        </el-form-item>
        <div style="text-align: right">
          <el-button type="primary" @click="onSubmit" >save</el-button>
          <el-button v-if="showCancel" @click="cancelSubmit" >Cancel</el-button>
          <el-button v-if="form.dataType === 1" @click="showReset">reset</el-button>
        </div>
      </div>

    </el-form>
    <channelCode ref="channelCode" />
    <chooseCivilCode ref="chooseCivilCode" />
    <chooseGroup ref="chooseGroup" />
    <resetChannel ref="resetChannel" @submit="reset"/>
  </div>
</template>

<script>
import channelCode from './../dialog/channelCode'
import ChooseCivilCode from '../dialog/chooseCivilCode.vue'
import ChooseGroup from '../dialog/chooseGroup.vue'
import diff from '../../utils/diff'
import ResetChannel from './../dialog/resetChannel.vue'

export default {
  name: 'CommonChannelEdit',
  components: {
    ResetChannel,
    ChooseCivilCode,
    ChooseGroup,
    channelCode
  },
  props: ['id', 'dataForm', 'showCancel'],
  data() {
    return {
      rules: {
        gbName: [
          { required: true, message: 'Please enter channel name', trigger: 'blur' }
        ],
        gbDeviceId: [
          { required: true, message: 'Please enter channel number', trigger: 'blur' }
        ]
      },
      loading: false,
      modelList: [],
      parentPath: [],
      regionPath: [],
      form: {}
    }
  },
  mounted() {
    this.$store.dispatch('server/getModelList')
      .then((data) => {
        console.log(data)
        this.modelList = data
      })
  },
  created() {
    // Get complete information
    if (this.id) {
      this.getCommonChannel(this.id)
    } else {
      if (!this.dataForm.gbDeviceId) {
        this.dataForm.gbDeviceId = ''
      }
      this.form = window.structuredClone(this.dataForm)
      this.getPaths()
    }
  },
  methods: {
    queryModel(queryString, callback) {
      // Filter options
      let modelList = this.modelList
      var results = queryString ? modelList.filter(((state) => {
        return (state.alias.toLowerCase().indexOf(queryString.toLowerCase()) === 0 || state.name.toLowerCase().indexOf(queryString.toLowerCase()) === 0)
      })) : modelList
      callback(results)
    },
    onSubmit: function() {
      this.$refs.channelForm.validate((valid) => {
        if (valid) {
          this.loading = true
          if (this.form.gbDownloadSpeedArray) {
            this.form.gbDownloadSpeed = this.form.gbDownloadSpeedArray.join('/')
          }
          this.form.enableBroadcast = this.form.enableBroadcastForBool ? 1 : 0
          // Determine which fields have changed
          let diffData = diff(this.dataForm, this.form)
          diffData['gbId'] = this.form.gbId

          console.log(diffData)
          console.log(this.dataForm)
          console.log(this.form)

          if (this.form.gbId) {
            this.$store.dispatch('commonChanel/update', diffData)
              .then(data => {
                this.$message.success({
                  showClose: true,
                  message: 'Saved successfully'
                })
                this.$emit('submitSuccess')
              })
              .catch((error) => {
                this.$message({
                  showClose: true,
                  message: error,
                  type: 'error'
                })
              })
              .finally(() => {
              this.loading = false
            })
          } else {
            this.$store.dispatch('commonChanel/add', this.form)
              .then(data => {
                this.$message.success({
                  showClose: true,
                  message: 'Saved successfully'
                })
                if (this.saveSuccess) {
                  this.saveSuccess()
                }
              })
              .catch((error) => {
                this.$message({
                  showClose: true,
                  message: error,
                  type: 'error'
                })
              })
              .finally(() => {
              this.loading = false
            })
          }
        }
      })
    },
    reset: function(fileIds) {
      this.$confirm('OK to reset to default content?', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.loading = true
        this.$store.dispatch('commonChanel/reset', {
          id: this.form.gbId,
          chanelFields: fileIds
        })
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: 'Reset successful saved'
            })
            this.getCommonChannel(this.form.gbId)
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
          .finally(() => {
            this.loading = false
          })
      }).catch(() => {

      })
    },
    getCommonChannel: function(id) {
      this.loading = true
      this.$store.dispatch('commonChanel/queryOne', id)
        .then(data => {
          if (data.gbDownloadSpeed) {
            data.gbDownloadSpeedArray = data.gbDownloadSpeed.split('/')
          }
          this.dataForm = window.structuredClone(data)
          this.form = data
          this.$set(this.form, 'enableBroadcastForBool', this.form.enableBroadcast === 1)
          this.getPaths()
          this.getRegionPaths()
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          this.loading = false
        })
    },
    buildDeviceIdCode: function(deviceId) {
      this.$refs.channelCode.openDialog(code => {
        this.form.gbDeviceId = code
      }, deviceId)
    },
    chooseCivilCode: function() {
      this.$refs.chooseCivilCode.openDialog(code => {
        this.form.gbCivilCode = code
        this.getRegionPaths()
      })
    },
    chooseGroup: function() {
      this.$refs.chooseGroup.openDialog((deviceId, businessGroupId) => {
        this.form.gbBusinessGroupId = businessGroupId
        this.form.gbParentId = deviceId
        this.getPaths()
      })
    },
    cancelSubmit: function() {
      this.$emit('cancel')
    },
    showReset: function() {
      this.$refs.resetChannel.openDialog()
    },
    getPaths: function() {
      this.parentPath = []
      if (this.form.gbParentId && this.form.gbBusinessGroupId) {
        this.$store.dispatch('group/getPath', {
          deviceId: this.form.gbParentId,
          businessGroup: this.form.gbBusinessGroupId
        })
          .then(data => {
            console.log(data)
            const path = []
            for (let i = 0; i < data.length; i++) {
              path.push(data[i].name)
            }
            this.parentPath = path
          })
      }
    },
    getRegionPaths: function() {
      this.regionPath = []
      if (this.form.gbCivilCode) {
        this.$store.dispatch('region/queryPath', this.form.gbCivilCode)
          .then(data => {
            console.log(data)
            const path = []
            for (let i = 0; i < data.length; i++) {
              path.push(data[i].name)
            }
            this.regionPath = path
          })
      }
    }
  }
}
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
