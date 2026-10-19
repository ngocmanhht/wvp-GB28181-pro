<template>
  <div id="channelMapInfobox" style="display: none">
    <div >
      <el-descriptions class="margin-top" title="channel.name" :column="4" direction="vertical">
        <el-descriptions-item label="Manufacturer">{{channel.manufacture}}</el-descriptions-item>
        <el-descriptions-item label="Model">{{channel.model}}</el-descriptions-item>
        <el-descriptions-item label="Equipment ownership" >{{channel.owner}}</el-descriptions-item>
        <el-descriptions-item label="Administrative region" >{{channel.civilCode}}</el-descriptions-item>
        <el-descriptions-item label="Installation address" >{{channel.address}}</el-descriptions-item>
        <el-descriptions-item label="Camera type" >{{channel.ptzTypeText}}</el-descriptions-item>
        <el-descriptions-item label="Latitude and longitude" >{{channel.longitude}},{{channel.latitude}}</el-descriptions-item>
        <el-descriptions-item label="Status">
          <el-tag size="small" v-if="channel.status === 1">online</el-tag>
          <el-tag size="small" v-if="channel.status === 0">Offline</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <devicePlayer ref="devicePlayer" v-loading="isLoging"></devicePlayer>
  </div>
</template>

<script>
import devicePlayer from '../device/dialog/devicePlayer.vue'

export default {
  name: "channelMapInfobox",
  props: ['channel'],
  computed: {devicePlayer},
  created() {},
  data() {
    return {
      showDialog: false,
      isLoging: false
    };
  },
  methods: {

    play: function (){
      let deviceId = this.channel.deviceId;
      this.isLoging = true;
      let channelId = this.channel.channelId;
      console.log("Notification device push1：" + deviceId + " : " + channelId);
      let that = this;
      this.$axios({
        method: 'get',
        url: '/api/play/start/' + deviceId + '/' + channelId
      }).then(function (res) {
        that.isLoging = false;
        if (res.data.code === 0) {
          that.$refs.devicePlayer.openDialog("media", deviceId, channelId, {
            streamInfo: res.data.data,
            hasAudio: this.channel.hasAudio
          });
        } else {
          that.$message.error(res.data.msg);
        }
      }).catch(function (e) {
      });
    },
    close: function () {
    },
  },
};
</script>
