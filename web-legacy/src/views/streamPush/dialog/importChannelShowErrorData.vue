<template>
  <div id="importChannelShowErrorData" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="Importing channel data was successful, but the data was duplicated"
      width="30rem"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        Repeat national standardID:
        <el-button v-clipboard="gbIds.join(',')" style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="Click to copy" @success="$message({type:'success', message:'Successfully copied to clipboard'})">Copy</el-button>
        <ul class="errDataBox">
          <li v-for="id in gbIds">
            {{ id }}
          </li>
        </ul>
      </div>

      <div>
        RepeatApp/stream:
        <el-button v-clipboard="streams.join(',')" style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="Click to copy" @success="$message({type:'success', message:'Successfully copied to clipboard'})">Copy</el-button>
        <ul class="errDataBox">
          <li v-for="id in streams">
            {{ id }}
          </li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ImportChannelShowErrorData',
  directives: { elDragDialog },
  props: ['gbIds', 'streams'],
  data() {
    return {
      isLoging: false,
      showDialog: false
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function() {
      this.showDialog = true
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>
<style>
.errDataBox{
  max-height: 15rem;
  overflow: auto;
}
</style>
