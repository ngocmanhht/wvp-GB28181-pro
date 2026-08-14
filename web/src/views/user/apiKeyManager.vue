<template>
  <div id="app" style="width: 100%">
    <el-dialog
      title="ApiKeylist"
      width="80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <el-form :inline="true" size="mini">
        <el-form-item>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addUserApiKey">
            addApiKey
          </el-button>
        </el-form-item>
      </el-form>
      <!--ApiKeylist-->
      <el-table
        size="small"
        :data="userList"
        style="width: 100%;font-size: 12px;"
        :height="winHeight"
        header-row-class-name="table-header"
      >
        <el-table-column prop="user.username" label="Username" min-width="120" />
        <el-table-column prop="app" label="Application name" min-width="160" />
        <el-table-column label="ApiKey" :show-overflow-tooltip="true" min-width="300">
          <template #default="scope">
            <i v-clipboard="scope.row.apiKey" class="cpoy-btn el-icon-document-copy" title="Click to copy" @success="$message({type:'success', message:'Successfully copied to clipboard'})" />
            <span>{{ scope.row.apiKey }}</span>

          </template>
        </el-table-column>
        <el-table-column prop="enable" label="enable" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.enable">
              enable
            </el-tag>
            <el-tag v-else type="info">
              deactivate
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Expiration time" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.expiredAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="Remarks" min-width="160" />
        <el-table-column label="Operation" min-width="260" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.enable"
              size="medium"
              icon="el-icon-circle-close"
              type="text"
              @click="disableUserApiKey(scope.row)"
            >
              deactivate
            </el-button>
            <el-button
              v-else
              size="medium"
              icon="el-icon-circle-check"
              type="text"
              @click="enableUserApiKey(scope.row)"
            >
              enable
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-refresh" type="text" @click="resetUserApiKey(scope.row)">
              reset
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-edit" type="text" @click="remarkUserApiKey(scope.row)">
              Remarks
            </el-button>
            <el-divider direction="vertical" />
            <el-button
              size="medium"
              icon="el-icon-delete"
              type="text"
              style="color: #f56c6c"
              @click="deleteUserApiKey(scope.row)"
            >
              Delete
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </el-dialog>
    <addUserApiKey ref="addUserApiKey" />
    <remarkUserApiKey ref="remarkUserApiKey" />
  </div>
</template>

<script>
import addUserApiKey from './dialog/addUserApiKey.vue'
import remarkUserApiKey from './dialog/remarkUserApiKey.vue'
import moment from 'moment'

export default {
  name: 'UserApiKeyManager',
  components: {
    addUserApiKey,
    remarkUserApiKey
  },
  data() {
    return {
      userList: [], // Device list
      currentUser: {}, // Current operating device object
      winHeight: window.innerHeight - 300,
      currentPage: 1,
      count: 15,
      total: 0,
      getUserApiKeyListLoading: false,
      showDialog: false
    }
  },
  mounted() {},
  methods: {
    openDialog: function(userId) {
      this.userId = userId
      this.showDialog = true
      this.initData()
    },
    initData() {
      this.getUserApiKeyList()
    },
    currentChange(val) {
      this.currentPage = val
      this.getUserApiKeyList()
    },
    handleSizeChange(val) {
      this.count = val
      this.getUserApiKeyList()
    },
    getUserApiKeyList() {
      this.getUserApiKeyListLoading = true
      this.$store.dispatch('userApiKeys/queryList', {
        page: this.currentPage,
        count: this.count,
        userId: this.userId
      })
        .then(data => {
          this.total = data.total
          this.userList = data.list
        })
        .finally(() => {
          this.getUserApiKeyListLoading = false
        })
    },
    addUserApiKey() {
      this.$refs.addUserApiKey.openDialog(this.userId, () => {
        this.$refs.addUserApiKey.close()
        this.$message({
          showClose: true,
          message: 'ApiKeyAdded successfully',
          type: 'success'
        })
        setTimeout(this.getUserApiKeyList, 200)
      })
    },
    remarkUserApiKey(row) {
      this.$refs.remarkUserApiKey.openDialog(row.id, () => {
        this.$refs.remarkUserApiKey.close()
        this.$message({
          showClose: true,
          message: 'Remarks modified successfully',
          type: 'success'
        })
        setTimeout(this.getUserApiKeyList, 200)
      })
    },
    enableUserApiKey(row) {
      let msg = 'Confirm to enable thisApiKey？'
      if (row.online !== 0) {
        msg = '<strong>Confirm to enable thisApiKey？</strong>'
      }
      this.$confirm(msg, 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/enable', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: 'Activated successfully',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }).catch(() => {
      })
    },
    disableUserApiKey(row) {
      let msg = 'Confirm to disable thisApiKey？'
      if (row.online !== 0) {
        msg = '<strong>Confirm to disable thisApiKey？</strong>'
      }
      this.$confirm(msg, 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/disable', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: 'Deactivation successful',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: 'Deactivation failed',
              type: 'error'
            })
            console.error(error)
          })
      }).catch(() => {
      })
    },
    resetUserApiKey(row) {
      let msg = 'OK to reset thisApiKey？'
      if (row.online !== 0) {
        msg = '<strong>OK to reset thisApiKey？</strong>'
      }
      this.$confirm(msg, 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/reset', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: 'Reset successful',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: 'Reset failed',
              type: 'error'
            })
            console.error(error)
          })
      }).catch(() => {
      })
    },
    deleteUserApiKey(row) {
      let msg = 'Confirm to delete thisApiKey？'
      if (row.online !== 0) {
        msg = '<strong>Confirm to delete thisApiKey？</strong>'
      }
      this.$confirm(msg, 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/remove', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: 'Delete successfully',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: 'Delete failed',
              type: 'error'
            })
            console.error(error)
          })
      }).catch(() => {
      })
    },
    close() {
      this.showDialog = false
    },
    formatTime(timestamp) {
      return moment(timestamp).format('YYYY-MM-DD HH:mm:ss')
    }
  }
}
</script>
<style>

</style>
