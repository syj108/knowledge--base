const Dashboard = {
    template: `
        <div>
            <h2 style="margin-bottom: 20px; color: #2d3748;">仪表盘</h2>

            <div class="stats-grid" v-if="stats">
                <div class="stat-card">
                    <div class="stat-value">{{ stats.totalSources }}</div>
                    <div class="stat-label">源文档</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">{{ stats.totalPages }}</div>
                    <div class="stat-label">知识页面</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">{{ stats.totalLinks }}</div>
                    <div class="stat-label">内部链接</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">{{ stats.totalCategories }}</div>
                    <div class="stat-label">分类数</div>
                </div>
            </div>

            <div class="card">
                <div class="card-title">快捷操作</div>
                <div class="quick-actions">
                    <router-link to="/upload" class="btn btn-primary btn-lg">上传文档</router-link>
                    <router-link to="/kb" class="btn btn-secondary btn-lg">浏览知识库</router-link>
                </div>
            </div>

            <div class="card" v-if="stats && stats.recentSources.length > 0">
                <div class="card-title">最近处理的源文档</div>
                <table class="table">
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>类型</th>
                            <th>状态</th>
                            <th>时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="s in stats.recentSources" :key="s.sourceId">
                            <td>{{ s.title }}</td>
                            <td>{{ typeLabel(s.type) }}</td>
                            <td><span :class="'badge badge-' + s.status">{{ statusLabel(s.status) }}</span></td>
                            <td>{{ formatTime(s.lastIngestedAt) }}</td>
                            <td><router-link :to="'/sources/' + s.sourceId">查看</router-link></td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="!stats" class="loading"><div class="spinner"></div><p>加载中...</p></div>
        </div>
    `,
    data() {
        return { stats: null };
    },
    created() {
        this.loadStats();
    },
    methods: {
        async loadStats() {
            try {
                this.stats = await API.dashboard();
            } catch (e) {
                console.error('加载仪表盘失败', e);
            }
        },
        typeLabel(type) {
            const map = { DOCUMENT: '文档', CODE_REPO: '代码仓库', FREE_TEXT: '文本' };
            return map[type] || type;
        },
        statusLabel(status) {
            const map = { parsed: '已解析', extracting: '提取中', generating: '生成中',
                          reducing: '归约中', completed: '已完成', failed: '失败' };
            return map[status] || status;
        },
        formatTime(iso) {
            if (!iso) return '-';
            const d = new Date(iso);
            return d.toLocaleString('zh-CN');
        }
    }
};
