const SearchView = {
    template: `
        <div>
            <h2 style="margin-bottom: 20px; color: #2d3748;">搜索结果</h2>

            <div class="card" style="margin-bottom: 20px; padding: 12px 16px;">
                <div style="display: flex; gap: 8px;">
                    <input class="form-input" v-model="query" placeholder="输入关键词搜索..."
                           @keyup.enter="doSearch" style="flex: 1;">
                    <button class="btn btn-primary" @click="doSearch">搜索</button>
                </div>
            </div>

            <div class="card" v-if="searched && results.length > 0">
                <div class="card-title">找到 {{ results.length }} 条结果</div>
                <div class="search-result" v-for="r in results" :key="r.slug">
                    <div>
                        <router-link :to="'/kb/' + r.slug" class="result-title">{{ r.title }}</router-link>
                        <span class="result-category">{{ r.category }}</span>
                    </div>
                    <div class="result-snippet" v-html="highlightSnippet(r.snippet)"></div>
                </div>
            </div>

            <div class="card empty-state" v-if="searched && results.length === 0">
                <div class="empty-icon">&#128269;</div>
                <div class="empty-text">未找到匹配内容</div>
                <p style="color: #a0aec0; margin-top: 8px;">请尝试使用其他关键词</p>
            </div>

            <div v-if="searching" class="loading"><div class="spinner"></div></div>
        </div>
    `,
    data() {
        return { query: '', results: [], searched: false, searching: false };
    },
    created() {
        this.query = this.$route.query.q || '';
        if (this.query) this.doSearch();
    },
    watch: {
        '$route.query.q'(val) {
            if (val && val !== this.query) {
                this.query = val;
                this.doSearch();
            }
        }
    },
    methods: {
        async doSearch() {
            if (!this.query.trim()) return;
            this.searching = true;
            this.searched = false;
            try {
                this.results = await API.search(this.query.trim());
            } catch (e) {
                console.error('搜索失败', e);
                this.results = [];
            }
            this.searched = true;
            this.searching = false;

            // 更新 URL
            if (this.$route.query.q !== this.query) {
                this.$router.replace({ path: '/search', query: { q: this.query } });
            }
        },
        highlightSnippet(snippet) {
            if (!snippet || !this.query) return snippet || '';
            const escaped = this.query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const re = new RegExp('(' + escaped + ')', 'gi');
            return snippet.replace(/</g, '&lt;').replace(/>/g, '&gt;')
                          .replace(re, '<mark>$1</mark>');
        }
    }
};
