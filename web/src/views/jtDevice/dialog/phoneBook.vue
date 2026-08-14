<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="Set up phone book"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <el-form :inline="true" size="mini" @submit.native.prevent>
        <el-form-item>
          <el-button-group>
            <el-button v-if="!showUpload" icon="el-icon-upload2" size="mini" type="primary" @click="uploadData" :disabled="phoneBookList.length === 0">Import data</el-button>
            <el-button v-if="showUpload" icon="el-icon-close" size="mini" type="danger" @click="uploadData">end import</el-button>
            <el-button icon="el-icon-download">
              <a style="text-align: center; text-decoration: none"
                 href="/static/file/Set up phonebook template.xlsx"
                 download="Set up phonebook template.xlsx"
               >Download template</a>
            </el-button>
          </el-button-group>

        </el-form-item>
        <el-form-item style="float: right;">
          <el-button-group>
            <el-button icon="el-icon-delete" size="mini" @click="clearPhoneBook">Clear phone book</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="uploadPhoneBook">Update phone book</el-button>
            <el-button icon="el-icon-document-add" size="mini" @click="appendPhoneBook">Add phone book</el-button>
            <el-button icon="el-icon-edit-outline" size="mini" @click="editPhoneBook">Modify phone book</el-button>
          </el-button-group>

        </el-form-item>
      </el-form>

      <el-table :data="phoneBookList" v-if="!showUpload && phoneBookList.length > 0" :height="500" stripe style="width: 100%" empty-text="There is no data yet, click to select or drag in the file" @click.stop="()=>{}">
        <el-table-column label="logo">
          <template v-slot:default="scope">
            <span >{{ signLabel(scope.row.sign) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="Contact person" />
        <el-table-column prop="phoneNumber" label="phone number" />
        <el-table-column label="Operation" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              type="text"
              style="color: #f56c6c"
              icon="el-icon-delete"
              :loading="scope.row.addRegionLoading"
              @click="removeRow(scope.$index)"
            >
              Remove
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-upload
        v-if="showUpload || phoneBookList.length === 0"
        style="width: fit-content; height: 300px; margin: 86px auto 0 auto"
        drag
        accept=".xls,.xlsx"
        action=""
        :auto-upload="false"
        :show-file-list="false"
        :on-change="loadFiled">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">Drag files here, or<em>Click to upload</em></div>
        <div class="el-upload__tip" slot="tip">Can only uploadxls/xlsxFile</div>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>

import * as XLSX from 'xlsx'
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      phoneNumber: null,
      showDialog: false,
      showUpload: false,
      phoneBookList: []
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(phoneNumber) {
      this.showDialog = true
      this.phoneNumber = phoneNumber
      this.phoneBookList = []
    },
    close: function() {
      this.showDialog = false
      this.showUpload = false
      this.phoneBookList = []
    },
    signLabel: function(sign) {
      switch (sign){
        case 1:
          return 'Incoming call'
        case 2:
          return 'exhale'
        case 3:
          return 'Incoming call/exhale'
        default:
          return 'Error: Setting range (1: incoming call, 2: outgoing call, 3: incoming call/exhale）'
      }
    },
    uploadData: function() {
      this.showUpload = !this.showUpload
    },
    loadFiled: function(file) {
      if (!file.name.endsWith('.xls') && !file.name.endsWith('.xlsx')) {
        this.$message.error('File format error')
        return
      }
      const fileReader = new FileReader()
      fileReader.onload = (event) => {
        const data = new Uint8Array(event.target.result)
        const workbook = XLSX.read(data, { type: 'array' })
        const sheetName = workbook.SheetNames[0]
        const worksheet = workbook.Sheets[sheetName]
        const jsonData = XLSX.utils.sheet_to_json(worksheet)
        for (let i = 0; i < jsonData.length; i++) {
          let item = jsonData[i]
          this.phoneBookList.push({
            sign: item['logo'],
            phoneNumber: item['phone number'],
            contactName: item['Contact person']
          })
        }
        this.showUpload = false
      }
      fileReader.readAsArrayBuffer(file.raw)
    },
    removeRow: function(index) {
      this.phoneBookList.splice(index, 1)
    },
    clearPhoneBook: function() {
      this.$confirm('All existing contacts in the terminal will be cleared, confirm？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.submit({
          phoneNumber: this.phoneNumber,
          type: 0
        })
      })

    },
    uploadPhoneBook: function() {
      this.$confirm('All contacts in the terminal will be deleted and the current contacts will be added. Confirm？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.submit({
          phoneNumber: this.phoneNumber,
          type: 1,
          phoneBookContactList: this.phoneBookList
        })
      })

    },
    appendPhoneBook: function() {
      this.$confirm('The current contact will be appended to the terminal, confirm？', 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.submit({
          phoneNumber: this.phoneNumber,
          type: 2,
          phoneBookContactList: this.phoneBookList
        })
      })

    },
    editPhoneBook: function() {
      this.submit({
        phoneNumber: this.phoneNumber,
        type: 3,
        phoneBookContactList: this.phoneBookList
      })
    },
    submit: function(data) {
      this.$store.dispatch("jtDevice/setPhoneBook", data)
        .then(data => {
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
</style>
