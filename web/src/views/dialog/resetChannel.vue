<template>
  <el-dialog
    v-el-drag-dialog
    title="Select the field to be reset"
    width="45rem"
    top="10rem"
    center
    :append-to-body="true"
    :close-on-click-modal="false"
    :visible.sync="showVideoDialog"
    v-if="showVideoDialog"
    :destroy-on-close="true"
  >
    <div style="padding: 0 1rem">
      <el-checkbox v-for="(item,index) in allVal" v-bind:key="item.field" v-model="item.checked" :label="item.name" ></el-checkbox>
    </div>

    <div slot="footer">
      <el-form size="small">
        <el-form-item style="text-align: left">
          <el-button @click="checkedSome" size="mini" >Commonly used</el-button>
          <el-button @click="checkedAll" size="mini" >Select all</el-button>
          <el-button @click="clearChecked" size="mini" >Clear</el-button>
        </el-form-item>
        <el-form-item style="text-align: right">
          <el-button type="primary" @click="handleOk">save</el-button>
          <el-button @click="closeModel" >Cancel</el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showVideoDialog: false,
      allVal: null
    }
  },
  beforeMount() {
    this.initData()
  },
  methods: {
    openDialog: function() {
      this.showVideoDialog = true
      this.initData()
    },
    closeModel: function() {
      this.showVideoDialog = false
    },
    initData: function() {
      this.allVal = [
        {
          name: 'Name',
          field: 'gbName',
          checked: true,
          disable: false
        },
        {
          name: 'encoding',
          field: 'gbDeviceId',
          checked: true,
          disable: false
        },
        {
          name: 'Equipment manufacturer',
          field: 'gbManufacturer',
          checked: true,
          disable: false
        },
        {
          name: 'Device model',
          field: 'gbModel',
          checked: true,
          disable: false
        },
        {
          name: 'Administrative region',
          field: 'gbCivilCode',
          checked: true,
          disable: false
        },
        {
          name: 'Installation address',
          field: 'gbAddress',
          checked: true,
          disable: false
        },
        {
          name: 'Monitoring position',
          field: 'gbDirectionType',
          checked: true,
          disable: false
        },
        {
          name: 'Parent node encoding',
          field: 'gbParentId',
          checked: true,
          disable: false
        },
        {
          name: 'Device status',
          field: 'gbStatus',
          checked: true,
          disable: false
        },
        {
          name: 'longitude',
          field: 'gbLongitude',
          checked: true,
          disable: false
        },
        {
          name: 'Latitude',
          field: 'gbLatitude',
          checked: true,
          disable: false
        },
        {
          name: 'Camera type',
          field: 'gbPtzType',
          checked: true,
          disable: false
        },
        {
          name: 'business grouping',
          field: 'gbBusinessGroupId',
          checked: true,
          disable: false
        },
        {
          name: 'police district',
          field: 'gbBlock',
          checked: true,
          disable: false
        },
        {
          name: 'Confidential attribute',
          field: 'gbSecrecy',
          checked: true,
          disable: false
        },
        {
          name: 'IPaddress',
          field: 'gbIpAddress',
          checked: true,
          disable: false
        },
        {
          name: 'port',
          field: 'gbPort',
          checked: true,
          disable: false
        },
        {
          name: 'Equipment ownership',
          field: 'gbOwner',
          checked: true,
          disable: false
        },
        {
          name: 'Is there a sub-device?',
          field: 'gbParental',
          checked: true,
          disable: false
        },
        {
          name: 'location type',
          field: 'gbPositionType',
          checked: true,
          disable: false
        },
        {
          name: 'indoor/outdoor',
          field: 'gbRoomType',
          checked: true,
          disable: false
        },
        {
          name: 'Purpose',
          field: 'gbUseType',
          checked: true,
          disable: false
        },
        {
          name: 'fill light',
          field: 'gbSupplyLightType',
          checked: true,
          disable: false
        },
        {
          name: 'resolution',
          field: 'gbResolution',
          checked: true,
          disable: false
        },
        {
          name: 'Download twice as fast',
          field: 'gbDownloadSpeed',
          checked: true,
          disable: false
        }
      ]
    },
    clearChecked: function() {
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        item.checked = false
      }
    },
    checkedAll: function() {
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        item.checked = true
      }
    },
    checkedSome: function() {
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        item.checked = (item.field === 'gbName' || item.field === 'gbStatus'
          || item.field === 'gbLongitude' || item.field === 'gbLatitude'
          || item.field === 'gbBusinessGroupId' || item.field === 'gbParentId')
      }
    },
    handleOk: function() {
      this.showVideoDialog = false
      let fileArray = []
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        if (item.checked) {
          fileArray.push(item.field)
        }
      }
      this.$emit('submit', fileArray)
    }
  }
}
</script>
