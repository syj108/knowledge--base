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

            <div v-if="page">
                <div class="page-title-bar">
                    <div v-if="!editingTitle" class="page-title-display">
                        <h1 class="page-title-text">{{ page.title }}</h1>
                        <button class="btn btn-edit-title" @click="startEditTitle">编辑标题</button>
                    </div>
                    <div v-else class="page-title-edit">
                        <input class="form-input" v-model="newTitle"
                               @keyup.enter="saveTitle" @keyup.escape="editingTitle = false">
                        <button class="btn btn-primary" style="white-space: nowrap;"
                                :disabled="!newTitle.trim() || savingTitle" @click="saveTitle">
                            {{ savingTitle ? '...' : '保存' }}
                        </button>
                        <button class="btn" style="white-space: nowrap;"
                                @click="editingTitle = false">取消</button>
                    </div>
                </div>
                <div class="card">
                    <div class="markdown-body" v-html="renderedHtml"></div>
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
        return { page: null, notFound: false, editingTitle: false, newTitle: '', savingTitle: false };
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
            this.editingTitle = false;
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
        },
        startEditTitle() {
            this.newTitle = this.page.title;
            this.editingTitle = true;
        },
        async saveTitle() {
            if (!this.newTitle.trim() || this.newTitle.trim() === this.page.title) {
                this.editingTitle = false;
                return;
            }
            this.savingTitle = true;
            try {
                const updated = await API.updatePageTitle(this.page.slug, this.newTitle.trim());
                this.page.title = updated.title;
                this.page.content = updated.content;
                this.editingTitle = false;
            } catch (e) {
                alert('保存标题失败: ' + e.message);
            }
            this.savingTitle = false;
        }
    }
};
