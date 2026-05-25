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
                    <div class="page-title-display">
                        <h1 class="page-title-text">{{ page.title }}</h1>
                        <button v-if="!editing" class="btn btn-edit-title" @click="startEdit">编辑</button>
                        <button v-else class="btn btn-edit-title" style="color: #2b6cb0; background: #ebf8ff;"
                                @click="cancelEdit">退出编辑</button>
                    </div>
                </div>

                <!-- 查看模式 -->
                <div v-if="!editing" class="card">
                    <div class="markdown-body" v-html="renderedHtml"></div>
                </div>

                <!-- 编辑模式 -->
                <div v-else class="card">
                    <div class="content-editor">
                        <div class="editor-toolbar">
                            <span style="font-size: 13px; color: #718096;">Markdown 编辑（标题即内容首行 # 标题）</span>
                            <div style="display: flex; gap: 8px;">
                                <button class="btn" style="font-size: 13px; padding: 4px 12px;"
                                        @click="cancelEdit">取消</button>
                                <button class="btn btn-primary" style="font-size: 13px; padding: 4px 12px;"
                                        :disabled="saving" @click="save">
                                    {{ saving ? '保存中...' : '保存' }}
                                </button>
                            </div>
                        </div>
                        <div class="editor-split">
                            <textarea class="editor-textarea" v-model="editContent"
                                      @input="updatePreview"></textarea>
                            <div class="editor-preview markdown-body" v-html="previewHtml"></div>
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
        return {
            page: null, notFound: false,
            editing: false, editContent: '', saving: false,
            previewHtml: ''
        };
    },
    computed: {
        renderedHtml() {
            if (!this.page) return '';
            return this.renderMarkdown(this.page.content || '');
        }
    },
    created() { this.loadPage(); },
    watch: {
        '$route.params': { handler() { this.loadPage(); }, deep: true }
    },
    methods: {
        renderMarkdown(content) {
            try {
                let html = marked.parse(content);
                html = html.replace(/\[\[([a-z0-9][a-z0-9\-\/]*)\|([^\]]+)\]\]/g,
                    '<a href="#/kb/$1" class="wiki-link">$2</a>');
                html = html.replace(/\[\[([a-z0-9][a-z0-9\-\/]*)\]\]/g,
                    '<a href="#/kb/$1" class="wiki-link">$1</a>');
                return html;
            } catch (e) {
                console.error('Markdown渲染失败', e);
                return '<pre>' + content.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>';
            }
        },
        async loadPage() {
            this.page = null;
            this.notFound = false;
            this.editing = false;
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
        startEdit() {
            this.editContent = this.page.content || '';
            this.previewHtml = this.renderMarkdown(this.editContent);
            this.editing = true;
        },
        cancelEdit() {
            this.editing = false;
        },
        updatePreview() {
            this.previewHtml = this.renderMarkdown(this.editContent);
        },
        async save() {
            this.saving = true;
            try {
                const updated = await API.updatePageContent(this.page.slug, this.editContent);
                this.page.content = updated.content;
                this.page.title = updated.title;
                this.editing = false;
            } catch (e) {
                alert('保存失败: ' + e.message);
            }
            this.saving = false;
        }
    }
};
