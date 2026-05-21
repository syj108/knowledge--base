const NavBar = {
    template: `
        <nav class="navbar">
            <router-link to="/" class="navbar-brand">知识沉淀智能体</router-link>
            <div class="navbar-links">
                <router-link to="/">首页</router-link>
                <router-link to="/upload">上传文档</router-link>
                <router-link to="/sources">源文档</router-link>
                <router-link to="/kb">知识库</router-link>
            </div>
            <div class="navbar-search">
                <input type="text"
                       v-model="searchQuery"
                       placeholder="搜索知识库..."
                       @keyup.enter="doSearch">
            </div>
        </nav>
    `,
    data() {
        return { searchQuery: '' };
    },
    methods: {
        doSearch() {
            if (this.searchQuery.trim()) {
                this.$router.push({ path: '/search', query: { q: this.searchQuery.trim() } });
            }
        }
    }
};
