const SourceDetail = {
    template: `
        <div>
            <div class="breadcrumb">
                <router-link to="/sources">源文档</router-link>
                <span class="separator">/</span>
                <span>{{ detail ? detail.title : '...' }}</span>
            </div>

            <div class="card" v-if="detail">
                <div class="card-title">基本信息</div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <div class="detail-label">标题</div>
                        <div class="detail-value">{{ detail.title }}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">类型</div>
                        <div class="detail-value">{{ typeLabel(detail.type) }}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">状态</div>
                        <div class="detail-value">
                            <span :class="'badge badge-' + status.status">{{ statusLabel(status.status) }}</span>
                            <span v-if="polling" style="margin-left: 8px; font-size: 12px; color: #a0aec0;">
                                自动刷新中...
                            </span>
                        </div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">内容哈希</div>
                        <div class="detail-value" style="font-family: monospace; font-size: 12px;">
                            {{ detail.contentHash || '-' }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- 生成的知识页面 -->
            <div class="card" v-if="status && status.generatedPages && status.generatedPages.length > 0">
                <div class="card-title">生成的知识页面</div>
                <div v-for="slug in status.generatedPages" :key="slug" style="margin-bottom: 8px;">
                    <router-link :to="'/kb/' + slug" style="color: #2b6cb0;">{{ slug }}</router-link>
                </div>
            </div>

            <!-- 拆分建议 -->
            <div class="alert alert-info" v-if="status && status.splitSuggested">
                该源文档生成的页面内容较长，建议进行拆分。
            </div>

            <!-- 源文档内容 -->
            <div class="card" v-if="sourceContent !== null">
                <div class="card-title">源文档内容
                    <span v-if="contentFileName" style="font-weight: 400; font-size: 12px; color: #a0aec0; margin-left: 8px;">
                        {{ contentFileName }}
                    </span>
                </div>
                <div class="source-content-box">{{ sourceContent || '(空)' }}</div>
            </div>

            <div v-if="!detail" class="loading"><div class="spinner"></div></div>
        </div>
    `,
    data() {
        return {
            detail: null,
            status: { status: 'parsed', generatedPages: [], splitSuggested: false },
            sourceContent: null,
            contentFileName: '',
            polling: false,
            pollTimer: null
        };
    },
    created() { this.load(); },
    beforeUnmount() { this.stopPolling(); },
    methods: {
        async load() {
            const id = this.$route.params.id;
            try {
                const [sources, statusData, content] = await Promise.all([
                    API.listSources(),
                    API.sourceStatus(id),
                    API.sourceContent(id).catch(() => ({ content: '', fileName: '' }))
                ]);
                this.detail = sources.find(s => s.sourceId === id) || { title: id, type: '' };
                this.status = statusData;
                this.sourceContent = content.content;
                this.contentFileName = content.fileName;

                if (this.isProcessing(statusData.status)) {
                    this.startPolling();
                }
            } catch (e) {
                console.error('加载源文档详情失败', e);
            }
        },
        isProcessing(s) {
            return ['parsed', 'extracting', 'generating', 'reducing'].includes(s);
        },
        startPolling() {
            this.polling = true;
            this.pollTimer = setInterval(async () => {
                try {
                    const id = this.$route.params.id;
                    this.status = await API.sourceStatus(id);
                    if (!this.isProcessing(this.status.status)) {
                        this.stopPolling();
                    }
                } catch (e) {
                    this.stopPolling();
                }
            }, 3000);
        },
        stopPolling() {
            this.polling = false;
            if (this.pollTimer) {
                clearInterval(this.pollTimer);
                this.pollTimer = null;
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
        }
    }
};
