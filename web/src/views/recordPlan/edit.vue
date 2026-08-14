<template>
  <div id="editRecordPlan" v-loading="loading" style="text-align: left;">
    <el-dialog
      v-el-drag-dialog
      title="Recording plan"
      width="900px"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" class="edit-record-plan">
        <el-form style="padding: 0 20px" size="small">
          <el-form-item>
            <el-input v-model="planName" type="text" placeholder="Please enter plan name" />
          </el-form-item>
          <el-form-item>
            <div class="content">
              <weekTimePicker ref="weekTimePicker" :plan-array="planArray" />
            </div>
          </el-form-item>
          <el-form-item>
            <div style="float: right; margin-top: 20px">
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
import weekTimePicker from '../common/weekTimePicker.vue'

export default {
  name: 'EditRecordPlan',
  directives: { elDragDialog },
  components: { weekTimePicker },
  props: {},
  data() {
    return {
      options: [],
      loading: false,
      edit: false,
      planName: null,
      id: null,
      showDialog: false,
      endCallback: '',
      planArray: [
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        }
      ]
    }
  },
  created() {
  },
  methods: {
    openDialog: function(recordPlan, endCallback) {
      this.endCallback = endCallback
      this.showDialog = true
      this.edit = false
      this.planArray = [
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        },
        {
          data: []
        }
      ]
      if (recordPlan) {
        console.log(recordPlan)
        this.edit = true
        this.planName = recordPlan.name
        this.id = recordPlan.id
        this.$store.dispatch('recordPlan/getPlan', recordPlan.id)
          .then(data => {
            if (data && data.planItemList) {
              this.handPlanData(data.planItemList)
            }
          })
          .catch(err => {
            console.log(err)
          })
      }
    },
    onSubmit: function() {
      const planList = this.handPlanArray()
      console.log(planList)
      if (!this.edit) {
        this.$store.dispatch('recordPlan/addPlan', {
          name: this.planName,
          planList: planList
        })
          .then(data => {
            this.$message({
              showClose: true,
              message: 'Added successfully',
              type: 'success'
            })
            this.endCallback()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
          .finally(() => {
            this.showDialog = false
          })
      } else {
        this.$store.dispatch('recordPlan/update', {
          id: this.id,
          name: this.planName,
          planList: planList
        })
          .then(data => {
            this.$message({
              showClose: true,
              message: 'Update successful',
              type: 'success'
            })
            this.endCallback()
          })
          .catch((error) => {
            console.error(error)
          })
          .finally(() => {
            this.showDialog = false
          })
      }
    },
    handPlanData: function(planList) {
      // Convert database data to component data format
      for (let i = 0; i < planList.length; i++) {
        const item = planList[i]
        console.log(item)
        this.planArray[item.weekDay - 1].data.push({
          start: item.start,
          end: item.stop
        })
      }
    },
    handPlanArray: function() {
      // Convert component data format to database data
      const dataArray = []
      for (let i = 0; i < this.planArray.length; i++) {
        const item = this.planArray[i]
        for (let j = 0; j < item.data.length; j++) {
          const itemData = item.data[j]
          dataArray.push({
            start: Math.floor(itemData.start),
            stop: Math.floor(itemData.end),
            weekDay: i + 1,
            planId: this.id
          })
        }
      }
      console.log(dataArray)
      return dataArray
    },
    close: function() {
      this.showDialog = false
      this.id = null
      this.planName = null
      this.byteTime = ''
      this.endCallback = ''
      if (this.endCallback) {
        this.endCallback()
      }
    }
  }
}
</script>
<style scoped>
.edit-record-plan {
  user-select: none;
}
.content-header {
  border-bottom: 1px solid #d9d9d9;
}
</style>
