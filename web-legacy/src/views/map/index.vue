<template>
    <div id="devicePosition" style="height: calc(100vh - 84px);width: 100%;">
      <div style="height: 100%; display: grid; grid-template-columns: 360px auto">
        <DeviceTree ref="deviceTree" @clickEvent="treeChannelClickEvent" :showPosition="true" :contextmenu="getContextmenu()"/>
        <MapComponent ref="mapComponent" @loaded="initChannelLayer" @coordinateSystemChange="initChannelLayer" @zoomChange="zoomChange"></MapComponent>
      </div>
      <div class="map-tool-box-bottom-right">
        <div class="map-tool-btn-group" v-if="mapTileList.length > 0">
          <el-dropdown placement="top"  @command="changeLayerType">
            <div class="el-dropdown-link map-tool-btn">
              <i class="iconfont icon-mti-jutai"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item :command="0" >
                <span v-if="layerType !== 0">Layer off</span>
                <span v-if="layerType === 0" style="color: rgb(64, 158, 255);">Layer off</span>
              </el-dropdown-item>
              <el-dropdown-item :command="1" >
                <span v-if="layerType !== 1">Direct display</span>
                <span v-if="layerType === 1" style="color: rgb(64, 158, 255);">Direct display</span>
              </el-dropdown-item>
              <el-dropdown-item :command="2">
                <span v-if="layerType !== 2">thin layer</span>
                <span v-if="layerType === 2" style="color: rgb(64, 158, 255);">thin layer</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div class="map-tool-btn-group" v-if="mapTileList && mapTileList.length > 1">
          <el-dropdown placement="top"  @command="changeMapTile">
            <div class="el-dropdown-link map-tool-btn">
              <i class="iconfont icon-tuceng"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item  v-for="(item,index) in mapTileList" :key="index" :command="index">{{item.name}}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" @click="refreshLayer">
            <i class="iconfont icon-shuaxin3"></i>
          </div>
        </div>
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" @click="zoomIn">
            <i class="iconfont icon-plus1"></i>
          </div>
          <div class="map-tool-btn" @click="zoomOut">
            <i class="iconfont icon-minus1"></i>
          </div>
        </div>
      </div>
      <div class="map-tool-box-top-left">
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" title="Layer thinning" @click="showDrawThinBox(true)">
            <i class="iconfont icon-mti-sandian"></i> <span>Layer thinning</span>
          </div>
        </div>
      </div>
      <transition name="el-zoom-in-top">
        <div v-show="showDrawThin"  class="map-tool-draw-thin">
          <div class="map-tool-draw-thin-density">
            <span style="line-height: 36px; font-size: 15px">interval： </span>
            <el-slider v-model="diffPixels" show-input :min="1" :max="200" input-size="mini" ></el-slider>
            <div style="margin-left: 10px; line-height: 38px;">
              <el-button :loading="quicklyDrawThinLoading" @click="quicklyDrawThin" size="mini">Fast thinning</el-button>
              <el-button :loading="boxDrawThinLoading" size="mini" @click="boxDrawThin" >local thinning</el-button>
              <el-button size="mini" @click="resetDrawThinData()">Data restoration</el-button>
              <el-button :loading="saveDrawThinLoading" type="primary" :disabled="drawThinId === null" size="mini" @click="saveDrawThin()">save</el-button>
              <el-button type="warning" size="mini" @click="showDrawThinBox(false)">Cancel</el-button>
            </div>
          </div>
        </div>
      </transition>

<!--      <div class="map-tool-box-top-right">-->
<!--        <div class="map-tool-btn-group">-->
<!--          <div class="map-tool-btn" title="Dilute">-->
<!--            <i class="iconfont icon-mti-sandian"></i>-->
<!--          </div>-->
<!--          <div class="map-tool-btn" title="aggregation">-->
<!--            <i class="iconfont icon-mti-jutai"></i>-->
<!--          </div>-->

<!--        </div>-->
<!--      </div>-->
      <div ref="infobox">
        <transition name="el-zoom-in-center">
          <div class="infobox-content" v-if="channel">
            <el-descriptions class="margin-top" :title="channel.gbName" :column="1" :colon="true" size="mini" :labelStyle="labelStyle" >
              <el-descriptions-item label="No." >{{channel.gbDeviceId}}</el-descriptions-item>
              <el-descriptions-item label="Manufacturer">{{channel.gbManufacture}}</el-descriptions-item>
              <el-descriptions-item label="Installation address" >{{channel.gbAddress == null?'unknown': channel.gbAddress}}</el-descriptions-item>
            </el-descriptions>
            <div style="padding-top: 10px; margin: 0 auto; width: fit-content;">
              <el-button v-bind:disabled="channel.gbStatus !== 'ON'" type="primary" size="small" title="play" icon="el-icon-video-play" @click="play(channel)">play</el-button>
              <el-button type="primary" size="small" title="Edit" icon="el-icon-edit" @click="edit(channel)">Edit</el-button>
              <el-button type="primary" size="small" title="location" icon="el-icon-coordinate" @click="editPosition(channel)">location</el-button>
<!--              <el-button type="primary" size="small" title="Track query" icon="el-icon-map-location" @click="getTrace(channel)">trajectory</el-button>-->
            </div>
            <span class="infobox-close el-icon-close" @click="closeInfoBox"></span>
          </div>
        </transition>

      </div>

      <div ref="infoboxForEdit">
        <transition name="el-zoom-in-center">
          <div class="infobox-edit-content" v-if="dragChannel">
            <div style="width: 100%; line-height: 1.5rem; font-size: 14px">{{dragChannel.gbName}}  ({{dragChannel.gbDeviceId}})</div>
            <span style="font-size: 14px">longitude:</span> <el-input v-model="dragChannel.gbLongitude" placeholder="Please enter longitude" style="width: 7rem; margin-right: 10px"></el-input>
            <span style="font-size: 14px">Latitude: </span> <el-input v-model="dragChannel.gbLatitude" placeholder="Please enter latitude" style="width: 7rem; "></el-input>
            <el-button icon="el-icon-close" size="medium" type="text" @click="cancelEdit(dragChannel)" style="margin-left: 1rem; font-size: 18px; color: #2b2f3a"></el-button>
            <el-button icon="el-icon-check" size="medium" type="text" @click="submitEdit(dragChannel)" style="font-size: 18px; color: #0842e2"></el-button>
          </div>
        </transition>
      </div>
      <player ref="player" ></player>
      <queryTrace ref="queryTrace" ></queryTrace>
      <CommonChannelEditDialog ref="commonChannelEditDialog" ></CommonChannelEditDialog>
      <DrawThinProgress ref="drawThinProgress" ></DrawThinProgress>
    </div>
</template>

<script>
import DeviceTree from '../common/DeviceTree.vue'
import queryTrace from './queryTrace.vue'
import MapComponent from '../common/MapComponent.vue'
import player from '../channel/player.vue'
import CommonChannelEditDialog from '../dialog/commonChannelEditDialog.vue'
import DrawThinProgress from './dialog/drawThinProgress.vue'

let channelLayer, channelTileLayer = null
export default {
  name: 'Map',
  components: {
    DrawThinProgress,
    CommonChannelEditDialog,
    DeviceTree,
    player,
    queryTrace,
    MapComponent
  },
  data() {
    return {
      layer: null,
      channel: null,
      dragChannel: {},
      feature: null,
      device: null,
      infoBoxId: null,
      labelStyle: {
        width: '56px'
      },
      isLoging: false,
      longitudeStr: 'longitude',
      latitudeStr: 'latitude',
      mapTileList: [],
      diffPixels: 120,
      zoomValue: 10,
      showDrawThin: false,
      quicklyDrawThinLoading: false,
      boxDrawThinLoading: false,
      drawThinId: null,
      drawThinLayer: null,
      saveDrawThinLoading: false,
      layerType: 0
    }
  },
  created() {

  },
  destroyed() {

  },
  methods: {
    initChannelLayer: function () {
      this.mapTileList = this.$refs.mapComponent.mapTileList
      // Get all channels with positions
      this.closeInfoBox()

      let clientEvent = data => {
        this.closeInfoBox()
        this.$nextTick(() => {
          if (data[0].edit) {
            this.showEditInfo(data[0])
          }else {
            this.showChannelInfo(data[0])
          }
        })
      }

      channelLayer = this.$refs.mapComponent.addPointLayer([], clientEvent, null)
    },
    refreshLayer(){
      this.closeInfoBox()

      // Refresh tile layer
      if (channelLayer) {
        this.$refs.mapComponent.refreshLayer(channelLayer)
      }
      if (channelTileLayer) {
        this.$refs.mapComponent.refreshLayer(channelTileLayer)
      }
    },
    treeChannelClickEvent: function (id) {
      this.closeInfoBox()
      this.$store.dispatch('commonChanel/queryOne', id)
        .then(data => {
          if (!data.gbLongitude || data.gbLongitude < 0 || !data.gbLatitude || data.gbLatitude < 0) {
            this.$message.warning({
              showClose: true,
              message: 'No location information'
            })
            return
          }
          let zoomExtent = this.$refs.mapComponent.getZoomExtent()
          this.$refs.mapComponent.panTo([data.gbLongitude, data.gbLatitude], zoomExtent[1], () => {
            this.showChannelInfo(data)
          })
        })
    },
    zoomIn: function() {
      this.$refs.mapComponent.zoomIn()
    },
    zoomOut: function() {
      this.$refs.mapComponent.zoomOut()
    },
    getContextmenu: function (event) {
        return [
          {
            label: 'Playback channel',
            icon: 'el-icon-video-play',
            type: 1,
            onClick: (event, data, node) => {
              console.log(data)
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.play(data)
                })
                .catch((error) => {
                  this.$message({
                    showClose: true,
                    message: error,
                    type: 'error'
                  })
                })
            }
          },
          {
            label: 'Modify location',
            icon: 'el-icon-coordinate',
            type: 1,
            onClick: (event, data, node) => {
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.editPosition(data)
                })
                .catch((error) => {
                  this.$message({
                    showClose: true,
                    message: error,
                    type: 'error'
                  })
                })
            }
          },
          {
            label: 'Edit channel',
            icon: 'el-icon-edit',
            type: 1,
            onClick: (event, data, node) => {
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.edit(data)
                })
                .catch((error) => {
                  this.$message({
                    showClose: true,
                    message: error,
                    type: 'error'
                  })
                })
            }
          }
        ]
    },
    showChannelInfo: function(data) {
      this.channel = data
      // Add a temporary icon at this time
      let position = [data.gbLongitude, data.gbLatitude]
      let cameraData = {
        id: data.gbId,
        position: position,
        data: data,
        status: data.gbStatus
      }
      this.$refs.mapComponent.addFeature(channelLayer, cameraData)

      this.infoBoxId = this.$refs.mapComponent.openInfoBox(position, this.$refs.infobox, [0, -50])
    },
    zoomChange: function(zoom) {},

    changeMapTile: function (index) {
      if (this.showDrawThin) {
        this.$message.warning({
          showClose: true,
          message: 'The thinning operation is in progress, switching layers is prohibited.'
        })
        return
      }
      this.$refs.mapComponent.changeMapTile(index)
      this.changeLayerType(this.layerType)
    },
    clientEvent(data){
      this.closeInfoBox()
      this.$nextTick(() => {
        if (data[0].edit) {
          this.showEditInfo(data[0])
        }else {
          this.showChannelInfo(data[0])
        }
      })
    },
    changeLayerType: function (index) {
      this.layerType = index
      if (index === 0) {
        this.$refs.mapComponent.removeLayer(channelTileLayer)
        return
      }
      if (channelTileLayer) {
        this.$refs.mapComponent.removeLayer(channelTileLayer)
      }

      const baseUrl = window.baseUrl ? window.baseUrl : ''
      let baseApi = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl)
      let tileUrl = null
      if (index === 1) {
        tileUrl = baseApi + '/api/common/channel/map/tile/{z}/{x}/{y}'
      }else if (index === 2) {
        tileUrl = baseApi + '/api/common/channel/map/thin/tile/{z}/{x}/{y}'
      }
      channelTileLayer = this.$refs.mapComponent.addVectorTileLayer(tileUrl, this.clientEvent)
    },
    closeInfoBox: function () {
      if (this.infoBoxId !== null) {
        this.$refs.mapComponent.closeInfoBox(this.infoBoxId)
      }
      this.channel = null
      this.dragChannel = null
    },
    play: function (channel) {
      const loading = this.$loading({
        lock: true,
        text: 'Requesting video',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/playChannel', channel.gbId)
        .then((data) => {
          this.$refs.player.openDialog('media', channel.gbId, {
            streamInfo: data,
            hasAudio: channel.hasAudio
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          loading.close()
        })
    },
    edit: function (channel) {
      this.$refs.commonChannelEditDialog.openDialog(channel.gbId)
    },
    editPosition: function (channel) {
      this.closeInfoBox()
      // Enable the icon to be dragged
      this.$refs.mapComponent.dragInteraction.addFeatureId(channel.gbId,
        {
          startEvent: event => {
            this.closeInfoBox()
          },
          endEvent: event => {
            channel.gbLongitude = event.lonLat[0]
            channel.gbLatitude = event.lonLat[1]
            this.showEditInfo(channel)
          }
        }
      )

      let position = null
      if (!!channel.gbLongitude && !!channel.gbLatitude && channel.gbLongitude > 0 && channel.gbLatitude > 0) {
        position = [channel.gbLongitude, channel.gbLatitude]
        channel['oldLongitude'] = channel.gbLongitude
        channel['oldLatitude'] = channel.gbLatitude
      }else {
        position = this.$refs.mapComponent.getCenter()
        channel['oldLongitude'] = channel.gbLongitude
        channel['oldLatitude'] = channel.gbLatitude
        channel.gbLongitude = position[0]
        channel.gbLatitude = position[1]
      }

      channel['edit'] = true
      if (!this.$refs.mapComponent.coordinateInView(position)) {
        this.$refs.mapComponent.panTo(position, 16, () => {
          this.showEditInfo(channel)
        })
      }else {
        this.showEditInfo(channel)
      }

      // Mark editable icon red
      this.$refs.mapComponent.setFeaturePositionById(channelLayer, channel.gbId, {
        id: channel.gbId,
        position: position,
        data: channel,
        status: 'checked'
      })
      // If the tile layer is turned on, the tile layer should no longer display this at this time.feature
      if (channelTileLayer) {
        this.$refs.mapComponent.hideFeature(channelTileLayer, channel.gbId)
      }
    },
    showEditInfo: function(data) {
      this.dragChannel = data
      this.infoBoxId = this.$refs.mapComponent.openInfoBox([data.gbLongitude, data.gbLatitude], this.$refs.infoboxForEdit, [0, -50])
    },
    cancelEdit: function(channel) {
      this.closeInfoBox()
      this.$refs.mapComponent.dragInteraction.removeFeatureId(channel.gbId)
      channel.gbLongitude = channel.oldLongitude
      channel.gbLatitude = channel.oldLatitude
      channel['edit'] = false
      this.$refs.mapComponent.setFeaturePositionById(channelLayer, channel.gbId, {
        id: channel.gbId,
        position: [channel.gbLongitude, channel.gbLatitude],
        data: channel,
        status: channel.gbStatus
      })
      if (channelTileLayer) {
        this.$refs.mapComponent.cancelHideFeature(channelTileLayer, channel.gbId)
      }
    },
    submitEdit: function(channel) {
      let position = [channel.gbLongitude, channel.gbLatitude]
      this.$store.dispatch('commonChanel/update', channel)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: 'Saved successfully'
          })
          this.closeInfoBox()
          channel['edit'] = false
          this.$refs.mapComponent.dragInteraction.removeFeatureId(channel.gbId)

          this.$refs.mapComponent.setFeaturePositionById(channelLayer, channel.gbId, {
            id: channel.gbId,
            position: position,
            data: channel,
            status: channel.gbStatus
          })
          // Refresh the star tree menu
          this.$refs.deviceTree.refresh('channel' + channel.gbId)

        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    showDrawThinBox: function(show){
      this.showDrawThin = show
      if (!show) {
        setTimeout(() => {
          // Turn off thinning preview
          if (this.drawThinId !== null) {
            // Send message Clear thinning results
            this.$store.dispatch('commonChanel/clearThin', this.drawThinId)
              .catch((error) => {
                this.$message({
                  showClose: true,
                  message: error,
                  type: 'error'
                })
              })
            this.drawThinId = null
          }
          if (this.drawThinLayer !== null) {
            this.$refs.mapComponent.removeLayer(this.drawThinLayer)
            this.drawThinLayer = null
          }
          // Presentation layer
          if (this.layerType > 0) {
            this.changeLayerType(this.layerType)
          }
        }, 1)
      }

    },
    quicklyDrawThin: function (){
      if (channelLayer) {
        this.$refs.mapComponent.removeLayer(channelLayer)
      }
      if (channelTileLayer) {
        this.$refs.mapComponent.removeLayer(channelTileLayer)
      }
      if (this.drawThinLayer !== null) {
        this.$refs.mapComponent.removeLayer(this.drawThinLayer)
        this.drawThinLayer = null
      }
      this.quicklyDrawThinLoading = true
      // Get the thinning parameters of each layer
      this.$store.dispatch('commonChanel/drawThin', {
        zoomParam: this.getDrawThinParam()
      })
        .then(drawThinId => {
          // Show thinning progress
          this.drawThinId = drawThinId
          this.$refs.drawThinProgress.openDialog(drawThinId, () => {
            this.closeInfoBox()
            this.$message.success({
              showClose: true,
              message: 'The thinning is completed, please save the thinning results after previewing them correctly.'
            })
            // Show thinning results
            this.showDrawThinLayer(drawThinId)
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          this.quicklyDrawThinLoading = false
        })
    },
    showDrawThinLayer(thinId) {
      if (this.drawThinLayer) {
        this.$refs.mapComponent.removeLayer(this.drawThinLayer)
        this.drawThinLayer = null
      }
      // Show thinning results
      let geoCoordSys = this.$refs.mapComponent.getCoordSys()
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      let baseApi = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl)
      let tileUrl = baseApi + `/api/common/channel/map/thin/tile/{z}/{x}/{y}?geoCoordSys=${geoCoordSys}&thinId=${thinId}&accessToken=${this.$store.getters.token}`
      this.drawThinLayer = this.$refs.mapComponent.addVectorTileLayer(tileUrl, this.clientEvent)
    },
    boxDrawThin: function (){
      this.$message.warning({
        showClose: true,
        message: 'Click on the map to make a frame selection'
      })
      // draw box
      this.$refs.mapComponent.startDrawBox((extent) => {

        // Clean up the default camera layer
        if (channelLayer) {
          this.$refs.mapComponent.removeLayer(channelLayer)
        }
        if (channelTileLayer) {
          this.$refs.mapComponent.removeLayer(channelTileLayer)
        }
        if (this.drawThinLayer !== null) {
          this.$refs.mapComponent.removeLayer(this.drawThinLayer)
          this.drawThinLayer = null
        }
        this.boxDrawThinLoading = true
        // Get the thinning parameters of each layer
        this.$store.dispatch('commonChanel/drawThin', {
          zoomParam: this.getDrawThinParam(),
          extent: {
            minLng: extent[0],
            minLat: extent[1],
            maxLng: extent[2],
            maxLat: extent[3]
          },
          geoCoordSys: 'GCJ02'
        })
          .then(drawThinId => {
            // Show thinning progress
            this.drawThinId = drawThinId
            this.$refs.drawThinProgress.openDialog(drawThinId, () => {
              this.closeInfoBox()
              this.$message.success({
                showClose: true,
                message: 'The thinning is completed, please save the thinning results after previewing them correctly.'
              })
              // Show thinning results
              this.showDrawThinLayer(drawThinId)
            })
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
          .finally(() => {
            this.boxDrawThinLoading = false
          })
      })
    },
    getDrawThinParam() {
      // Get all levels
      let zoomExtent = this.$refs.mapComponent.getZoomExtent()
      let zoomMap = {}
      let zoom = zoomExtent[0]
      while (zoom <= zoomExtent[1]) {
        // Calculate the difference between latitude and longitude
        let diff = this.$refs.mapComponent.computeDiff(this.diffPixels, zoom)
        if (diff && diff > 0) {
          zoomMap[zoom] = diff
        }
        zoom += 1
      }
      return zoomMap
    },

    saveDrawThin: function(){
      if (!this.drawThinId) {
        return
      }
      this.saveDrawThinLoading = true
      this.$store.dispatch('commonChanel/saveThin', this.drawThinId)
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: 'Saved successfully'
          })
          this.showDrawThinBox(false)
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          this.saveDrawThinLoading = false
        })
    },
    resetDrawThinData(){
      this.$confirm('Confirm removal of thinning results?', 'Operation tips', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('commonChanel/resetLevel')
          .then(() => {
            this.$message.success({
              showClose: true,
              message: 'Data restored successfully'
            })
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      })
    }
  }

}
</script>

<style>
.map-tool-box-bottom-right {
  position: absolute;
  right: 20px;
  bottom: 20px;
}
.map-tool-box-top-right {
  position: absolute;
  right: 20px;
  top: 20px;
}
.map-tool-box-top-left {
  position: absolute;
  left: 380px;
  top: 20px;
}
.map-tool-btn-group {
  background-color: #FFFFFF;
  border-radius: 3px;
  user-select: none;
  box-shadow: 0 2px 2px rgba(0, 0, 0, .15);
  margin-bottom: 10px;
}
.map-tool-box-top-left .map-tool-btn-group {
  display: flex;
}
.map-tool-box-top-right .map-tool-btn-group {
  display: flex;
}
.map-tool-box-top-left .map-tool-btn {
  padding: 0 10px;
}
.map-tool-box-top-right .map-tool-btn {
  padding: 0 10px;
}
.map-tool-btn {
  border-bottom: 1px #dfdfdf solid;
  border-right: 1px #dfdfdf solid;
  width: fit-content;
  min-width: 33px;
  height: 36px;
  cursor: pointer;
  text-align: center;
  line-height: 36px;
  font-size: 14px;
}
.map-tool-btn i {
  font-size: 14px;
}
.map-tool-btn-group:last-child {
  border-bottom: none;
  border-right: none;
}
.map-tool-draw-thin {
  position: absolute;
  top: 63px;
  left: 380px;
  border: 1px solid #dfdfdf;
  background-color: #fff;
  border-radius: 4px;
  padding: 0 10px;
}
.map-tool-draw-thin-density {
  display: grid;
  grid-template-columns: 50px 400px auto;
  padding: 0;
  margin: 0;
}

.infobox-content{
  width: 270px;
  background-color: #FFFFFF;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #868686;
}

.infobox-content::after {
  position: absolute;
  bottom: -11px;
  left: calc(50% - 8px);
  display: block;
  content: "";
  width: 16px;
  height: 16px;
  background: url('/static/images/arrow.png') no-repeat center;
}

.infobox-edit-content{
  width: 400px;
  background-color: #FFFFFF;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #868686;
}

.infobox-edit-content::after {
  position: absolute;
  bottom: -11px;
  left: calc(50% - 8px);
  display: block;
  content: "";
  width: 16px;
  height: 16px;
  background: url('/static/images/arrow.png') no-repeat center;
}
.infobox-close {
  position: absolute;
  right: 1rem;
  top: 1rem;
  color: #000000;
  cursor:pointer
}
.el-descriptions__title {
  font-size: 1rem;
  font-weight: 700;
  padding: 20px 20px 0px 23px;
  text-align: center;
  width: 100%;
}
</style>
