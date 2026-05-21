const PageDetail = {
    template: `
        <div>
            <div class="breadcrumb">
                <router-link to="/kb">知识库</router-link>
                <span class="separator">/</span>
                <span v-if="page">{{ page.category }}</span>
                <span class="separator" v-if="page">/</span>
                <span>{{ page ? page.title : '...' }}</span>
            </div>

            <div class="page-detail-layout" v-if="page">
                <div class="card">
                    <div class="markdown-body" v-html="renderedHtml"></div>
                </div>
                <div class="page-meta">
                    <div class="meta-title">页面信息</div>
                    <div class="meta-item">
                        <div class="meta-label">分类</div>
                        <div>{{ page.category }}</div>
                    </div>
                    <div class="meta-item">
                        <div class="meta-label">标识</div>
                        <div style="font-family: monospace; font-size: 12px; word-break: break-all;">
                            {{ page.slug }}
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="notFound" class="card empty-state">
                <div class="empty-icon">&#128533;</div>
                <div class="empty-text">页面不存在</div>
                <router-link to="/kb" class="btn btn-primary" style="margin-top: 16px;">返回知识库</router-link>
            </div>

            <div v-if="!page && !notFound" class="loading"><div class="spinner"></div></div>
        </div>
    `,
    data() {
        return { page: null, notFound: false };
    },
    computed: {
        renderedHtml() {
            if (!this.page) return '';
            try {
                let html = marked.parse(this.page.content || '');
                html = html.replace(/\[\[([a-z0-9][a-z0-9\-\/]*)\|([^\]]+)\]\]/g,
                    '<a href="#/kb/$1" class="wiki-link">$2</a>');
                html = html.replace(/\[\[([a-z0-9][a-z0-9\-\/]*)\]\]/g,
                    '<a href="#/kb/$1" class="wiki-link">$1</a>');
                return html;
            } catch (e) {
                console.error('Markdown渲染失败', e);
                return '<pre>' + (this.page.content || '').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>';
            }
        }
    },
    created() { this.loadPage(); },
    watch: {
        '$route.params': { handler() { this.loadPage(); }, deep: true }
    },
    methods: {
        async loadPage() {
            this.page = null;
            this.notFound = false;
            const slugParts = this.$route.params.slug;
            const slug = Array.isArray(slugParts) ? slugParts.join('/') : (slugParts || '');
            if (!slug) { this.notFound = true; return; }
            try {
                const resp = await fetch(`/api/pages/${slug}`);
                if (resp.status === 404) {
                    this.notFound = true;
                    return;
                }
                this.page = await resp.json();
            } catch (e) {
                console.error('加载页面失败', e);
                this.notFound = true;
            }
        }
    }
};
