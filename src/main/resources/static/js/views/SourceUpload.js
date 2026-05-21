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

                <!-- 类别选择（通用） -->
                <div class="form-group" style="margin-bottom: 16px; padding: 12px; background: #f7fafc; border-radius: 6px;">
                    <label class="form-label" style="font-weight: 600;">所属类别</label>
                    <div style="display: flex; gap: 8px; align-items: center;">
                        <select class="form-select" v-model="selectedCategory" style="flex: 1;">
                            <option value="">不指定（由 AI 自动分类）</option>
                            <option v-for="cat in categories" :key="cat.key" :value="cat.key">
                                {{ cat.name }}（{{ cat.key }}）
                            </option>
                            <option value="__new__">+ 新建类别...</option>
                        </select>
                    </div>
                    <!-- 新建类别表单 -->
                    <div v-if="selectedCategory === '__new__'" style="margin-top: 12px; padding: 12px; background: #fff; border: 1px solid #e2e8f0; border-radius: 4px;">
                        <div class="form-group">
                            <label class="form-label">类别标识（英文 kebab-case）</label>
                            <input class="form-input" v-model="newCatKey" placeholder="例如: identity-service">
                        </div>
                        <div class="form-group">
                            <label class="form-label">类别名称（中文）</label>
                            <input class="form-input" v-model="newCatName" placeholder="例如: 应用身份服务">
                        </div>
                        <div class="form-group">
                            <label class="form-label">描述</label>
                            <input class="form-input" v-model="newCatDesc" placeholder="例如: EIAM、CIAM 等身份认证相关产品">
                        </div>
                        <button class="btn btn-primary" style="margin-top: 8px;"
                                :disabled="!newCatKey.trim() || !newCatName.trim() || creatingCat"
                                @click="createCategory">
                            {{ creatingCat ? '创建中...' : '创建类别' }}
                        </button>
                    </div>
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
            result: null,
            categories: [],
            selectedCategory: '',
            newCatKey: '',
            newCatName: '',
            newCatDesc: '',
            creatingCat: false
        };
    },
    created() {
        this.loadCategories();
    },
    methods: {
        async loadCategories() {
            try {
                this.categories = await API.listCategories();
            } catch (e) {
                console.error('加载类别列表失败', e);
            }
        },
        async createCategory() {
            this.creatingCat = true;
            try {
                const cat = await API.createCategory(
                    this.newCatKey.trim(), this.newCatName.trim(), this.newCatDesc.trim());
                this.categories.push(cat);
                this.selectedCategory = cat.key;
                this.newCatKey = '';
                this.newCatName = '';
                this.newCatDesc = '';
            } catch (e) {
                alert('创建类别失败: ' + e.message);
            }
            this.creatingCat = false;
        },
        getEffectiveCategory() {
            if (this.selectedCategory && this.selectedCategory !== '__new__') {
                return this.selectedCategory;
            }
            return null;
        },
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
                this.result = await API.uploadDocument(this.selectedFile, this.getEffectiveCategory());
            } catch (e) {
                this.result = { status: 'error', message: '上传失败: ' + e.message };
            }
            this.submitting = false;
        },
        async submitText() {
            this.submitting = true;
            this.result = null;
            try {
                this.result = await API.submitText(
                    this.textTitle, this.textContent, this.textLang, this.getEffectiveCategory());
            } catch (e) {
                this.result = { status: 'error', message: '提交失败: ' + e.message };
            }
            this.submitting = false;
        },
        async submitCode() {
            this.submitting = true;
            this.result = null;
            try {
                this.result = await API.submitCode(
                    this.gitUrl, this.gitBranch, this.getEffectiveCategory());
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
