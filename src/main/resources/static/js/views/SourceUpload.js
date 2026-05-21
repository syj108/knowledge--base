const SourceUpload = {
    template: `
        <div>
            <h2 style="margin-bottom: 20px; color: #2d3748;">上传源文档</h2>

            <div class="card">
                <div class="tabs">
                    <div class="tab" :class="{active: tab === 'document'}" @click="tab='document'">文档上传</div>
                    <div class="tab" :class="{active: tab === 'text'}" @click="tab='text'">文本输入</div>
                    <div class="tab" :class="{active: tab === 'code'}" @click="tab='code'">代码仓库</div>
                </div>

                <!-- 文档上传 -->
                <div v-if="tab === 'document'">
                    <div class="upload-zone"
                         :class="{'drag-over': dragging}"
                         @dragover.prevent="dragging=true"
                         @dragleave="dragging=false"
                         @drop.prevent="onDrop"
                         @click="$refs.fileInput.click()">
                        <div class="upload-icon">&#128196;</div>
                        <div class="upload-text">点击或拖拽文件到此处</div>
                        <div class="upload-hint">支持 PDF、Word、Markdown、HTML 等格式，最大 100MB</div>
                        <input ref="fileInput" type="file" style="display:none"
                               accept=".pdf,.doc,.docx,.md,.html,.txt"
                               @change="onFileSelect">
                    </div>
                    <div v-if="selectedFile" style="margin-top: 12px; font-size: 14px; color: #4a5568;">
                        已选择：{{ selectedFile.name }}（{{ formatSize(selectedFile.size) }}）
                    </div>
                    <button v-if="selectedFile" class="btn btn-primary" style="margin-top: 12px;"
                            :disabled="submitting" @click="uploadDoc">
                        {{ submitting ? '上传中...' : '开始上传' }}
                    </button>
                </div>

                <!-- 文本输入 -->
                <div v-if="tab === 'text'">
                    <div class="form-group">
                        <label class="form-label">标题</label>
                        <input class="form-input" v-model="textTitle" placeholder="请输入文档标题">
                    </div>
                    <div class="form-group">
                        <label class="form-label">语言</label>
                        <select class="form-select" v-model="textLang">
                            <option value="zh">中文</option>
                            <option value="en">英文</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">内容</label>
                        <textarea class="form-textarea" v-model="textContent"
                                  placeholder="请粘贴或输入文本内容..."></textarea>
                    </div>
                    <button class="btn btn-primary" :disabled="submitting || !textContent.trim()"
                            @click="submitText">
                        {{ submitting ? '提交中...' : '提交文本' }}
                    </button>
                </div>

                <!-- 代码仓库 -->
                <div v-if="tab === 'code'">
                    <div class="form-group">
                        <label class="form-label">Git 仓库地址</label>
                        <input class="form-input" v-model="gitUrl" placeholder="https://github.com/user/repo.git">
                    </div>
                    <div class="form-group">
                        <label class="form-label">分支</label>
                        <input class="form-input" v-model="gitBranch" placeholder="main">
                    </div>
                    <button class="btn btn-primary" :disabled="submitting || !gitUrl.trim()"
                            @click="submitCode">
                        {{ submitting ? '克隆中...' : '开始分析' }}
                    </button>
                </div>
            </div>

            <!-- 提交结果 -->
            <div v-if="result" class="alert" :class="result.status === 'error' ? 'alert-error' : 'alert-success'"
                 style="margin-top: 16px;">
                {{ result.message }}
                <router-link v-if="result.sourceId" :to="'/sources/' + result.sourceId"
                             style="margin-left: 12px;">查看处理状态 &rarr;</router-link>
            </div>
        </div>
    `,
    data() {
        return {
            tab: 'document',
            dragging: false,
            selectedFile: null,
            textTitle: '',
            textContent: '',
            textLang: 'zh',
            gitUrl: '',
            gitBranch: 'main',
            submitting: false,
            result: null
        };
    },
    methods: {
        onDrop(e) {
            this.dragging = false;
            const files = e.dataTransfer.files;
            if (files.length > 0) this.selectedFile = files[0];
        },
        onFileSelect(e) {
            if (e.target.files.length > 0) this.selectedFile = e.target.files[0];
        },
        async uploadDoc() {
            this.submitting = true;
            this.result = null;
            try {
                this.result = await API.uploadDocument(this.selectedFile);
            } catch (e) {
                this.result = { status: 'error', message: '上传失败: ' + e.message };
            }
            this.submitting = false;
        },
        async submitText() {
            this.submitting = true;
            this.result = null;
            try {
                this.result = await API.submitText(this.textTitle, this.textContent, this.textLang);
            } catch (e) {
                this.result = { status: 'error', message: '提交失败: ' + e.message };
            }
            this.submitting = false;
        },
        async submitCode() {
            this.submitting = true;
            this.result = null;
            try {
                this.result = await API.submitCode(this.gitUrl, this.gitBranch);
            } catch (e) {
                this.result = { status: 'error', message: '提交失败: ' + e.message };
            }
            this.submitting = false;
        },
        formatSize(bytes) {
            if (bytes < 1024) return bytes + ' B';
            if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
            return (bytes / 1048576).toFixed(1) + ' MB';
        }
    }
};
