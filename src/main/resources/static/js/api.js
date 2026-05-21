/**
 * API 调用封装
 */
const API = {
    // 仪表盘
    dashboard() {
        return fetch('/api/dashboard').then(r => r.json());
    },

    // 源文档
    listSources() {
        return fetch('/api/sources').then(r => r.json());
    },

    sourceStatus(id) {
        return fetch(`/api/sources/${id}/status`).then(r => r.json());
    },

    sourceContent(id) {
        return fetch(`/api/sources/${id}/content`).then(r => r.json());
    },

    uploadDocument(file, category) {
        const form = new FormData();
        form.append('file', file);
        if (category) form.append('category', category);
        return fetch('/api/sources/document', { method: 'POST', body: form }).then(r => r.json());
    },

    submitText(title, content, language, category) {
        return fetch('/api/sources/text', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, content, language, category: category || null })
        }).then(r => r.json());
    },

    submitCode(gitUrl, branch, category) {
        return fetch('/api/sources/code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ gitUrl, branch, category: category || null })
        }).then(r => r.json());
    },

    listCategories() {
        return fetch('/api/sources/categories').then(r => r.json());
    },

    createCategory(key, name, description) {
        return fetch('/api/sources/categories', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key, name, description })
        }).then(r => r.json());
    },

    // 知识页面
    listPages() {
        return fetch('/api/pages').then(r => r.json());
    },

    getPage(slug) {
        return fetch(`/api/pages/${slug}`).then(r => r.json());
    },

    updatePageTitle(slug, title) {
        return fetch(`/api/pages/${slug}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title })
        }).then(r => r.json());
    },

    search(query) {
        return fetch(`/api/pages/search?q=${encodeURIComponent(query)}`).then(r => r.json());
    },

    // 图谱
    getGraph() {
        return fetch('/api/graph').then(r => r.json());
    }
};
