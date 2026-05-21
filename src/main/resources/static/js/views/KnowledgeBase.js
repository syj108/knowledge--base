const KnowledgeBase = {
    template: `
        <div>
            <h2 style="margin-bottom: 20px; color: #2d3748;">知识库</h2>

            <div v-if="loaded && pages.length === 0" class="card empty-state">
                <div class="empty-icon">&#128218;</div>
                <div class="empty-text">知识库暂无内容</div>
                <p style="color: #a0aec0; margin-top: 8px;">上传文档后，系统将自动生成知识页面</p>
                <router-link to="/upload" class="btn btn-primary" style="margin-top: 16px;">上传文档</router-link>
            </div>

            <div class="kb-layout" v-if="pages.length > 0">
                <div class="kb-sidebar">
                    <div style="font-size: 13px; font-weight: 600; color: #718096; margin-bottom: 12px; text-transform: uppercase;">
                        分类目录
                    </div>
                    <div class="category-item" :class="{active: selectedCategory === ''}"
                         @click="selectedCategory = ''">
                        <span>全部</span>
                        <span class="category-count">{{ pages.length }}</span>
                    </div>
                    <div class="category-item"
                         v-for="(items, cat) in categories" :key="cat"
                         :class="{active: selectedCategory === cat}"
                         @click="selectedCategory = cat">
                        <span>{{ cat }}</span>
                        <span class="category-count">{{ items.length }}</span>
                    </div>
                    <div style="margin-top: 16px; border-top: 1px solid #e2e8f0; padding-top: 12px;">
                        <button v-if="!showNewCatForm" class="btn"
                                style="width: 100%; font-size: 13px; background: #edf2f7; color: #4a5568; border: 1px dashed #cbd5e0;"
                                @click="showNewCatForm = true">+ 新增类别</button>
                        <div v-if="showNewCatForm" style="font-size: 13px;">
                            <input class="form-input" v-model="newCatKey" placeholder="标识 (kebab-case)"
                                   style="font-size: 12px; padding: 6px 8px; margin-bottom: 6px;">
                            <input class="form-input" v-model="newCatName" placeholder="名称 (中文)"
                                   style="font-size: 12px; padding: 6px 8px; margin-bottom: 6px;">
                            <input class="form-input" v-model="newCatDesc" placeholder="描述"
                                   style="font-size: 12px; padding: 6px 8px; margin-bottom: 8px;">
                            <div style="display: flex; gap: 6px;">
                                <button class="btn btn-primary" style="flex:1; font-size: 12px; padding: 4px 8px;"
                                        :disabled="!newCatKey.trim() || !newCatName.trim() || creatingCat"
                                        @click="createCategory">
                                    {{ creatingCat ? '...' : '创建' }}
                                </button>
                                <button class="btn" style="flex:1; font-size: 12px; padding: 4px 8px;"
                                        @click="showNewCatForm = false">取消</button>
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <div class="page-grid">
                        <div class="page-card" v-for="p in filteredPages" :key="p.slug"
                             @click="$router.push('/kb/' + p.slug)">
                            <div class="page-title">{{ p.title }}</div>
                            <div class="page-slug">{{ p.slug }}</div>
                            <div class="page-summary">{{ p.summary }}</div>
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="!loaded" class="loading"><div class="spinner"></div></div>
        </div>
    `,
    data() {
        return {
            pages: [], selectedCategory: '', loaded: false,
            showNewCatForm: false, newCatKey: '', newCatName: '', newCatDesc: '', creatingCat: false
        };
    },
    computed: {
        categories() {
            const cats = {};
            for (const p of this.pages) {
                if (!cats[p.category]) cats[p.category] = [];
                cats[p.category].push(p);
            }
            return cats;
        },
        filteredPages() {
            if (!this.selectedCategory) return this.pages;
            return this.pages.filter(p => p.category === this.selectedCategory);
        }
    },
    created() { this.loadPages(); },
    methods: {
        async loadPages() {
            try {
                this.pages = await API.listPages();
            } catch (e) {
                console.error('加载知识页面失败', e);
            }
            this.loaded = true;
        },
        async createCategory() {
            this.creatingCat = true;
            try {
                await API.createCategory(this.newCatKey.trim(), this.newCatName.trim(), this.newCatDesc.trim());
                this.newCatKey = '';
                this.newCatName = '';
                this.newCatDesc = '';
                this.showNewCatForm = false;
            } catch (e) {
                alert('创建类别失败: ' + e.message);
            }
            this.creatingCat = false;
        }
    }
};
