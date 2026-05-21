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
        return { pages: [], selectedCategory: '', loaded: false };
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
        }
    }
};
