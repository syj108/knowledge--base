const SourceList = {
    template: `
        <div>
            <h2 style="margin-bottom: 20px; color: #2d3748;">源文档列表</h2>

            <div class="card" style="margin-bottom: 16px; padding: 12px 16px;">
                <div style="display: flex; gap: 8px; align-items: center;">
                    <span style="font-size: 14px; color: #718096;">筛选：</span>
                    <span class="btn btn-secondary" :class="{'btn-primary': filter === ''}"
                          style="padding: 4px 12px; font-size: 13px;" @click="filter=''">全部</span>
                    <span class="btn btn-secondary" :class="{'btn-primary': filter === 'completed'}"
                          style="padding: 4px 12px; font-size: 13px;" @click="filter='completed'">已完成</span>
                    <span class="btn btn-secondary" :class="{'btn-primary': filter === 'processing'}"
                          style="padding: 4px 12px; font-size: 13px;" @click="filter='processing'">处理中</span>
                    <span class="btn btn-secondary" :class="{'btn-primary': filter === 'failed'}"
                          style="padding: 4px 12px; font-size: 13px;" @click="filter='failed'">失败</span>
                </div>
            </div>

            <div class="card" v-if="filteredSources.length > 0">
                <table class="table">
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>类型</th>
                            <th>状态</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="s in filteredSources" :key="s.sourceId">
                            <td>{{ s.title }}</td>
                            <td>{{ typeLabel(s.type) }}</td>
                            <td><span :class="'badge badge-' + s.status">{{ statusLabel(s.status) }}</span></td>
                            <td><router-link :to="'/sources/' + s.sourceId">查看详情</router-link></td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-else-if="loaded" class="card empty-state">
                <div class="empty-icon">&#128195;</div>
                <div class="empty-text">暂无源文档</div>
                <router-link to="/upload" class="btn btn-primary" style="margin-top: 16px;">上传文档</router-link>
            </div>

            <div v-else class="loading"><div class="spinner"></div></div>
        </div>
    `,
    data() {
        return { sources: [], filter: '', loaded: false };
    },
    computed: {
        filteredSources() {
            if (!this.filter) return this.sources;
            if (this.filter === 'processing') {
                return this.sources.filter(s =>
                    ['parsed', 'extracting', 'generating', 'reducing'].includes(s.status));
            }
            return this.sources.filter(s => s.status === this.filter);
        }
    },
    created() { this.loadSources(); },
    methods: {
        async loadSources() {
            try {
                this.sources = await API.listSources();
            } catch (e) {
                console.error('加载源文档列表失败', e);
            }
            this.loaded = true;
        },
        typeLabel(type) {
            const map = { DOCUMENT: '文档', CODE_REPO: '代码仓库', FREE_TEXT: '文本' };
            return map[type] || type;
        },
        statusLabel(status) {
            const map = { parsed: '已解析', extracting: '提取中', generating: '生成中',
                          reducing: '归约中', completed: '已完成', failed: '失败' };
            return map[status] || status;
        }
    }
};
