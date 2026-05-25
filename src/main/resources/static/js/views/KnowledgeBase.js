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
                         @click="selectedCategory = cat"
                         @mouseenter="hoveredCategory = cat"
                         @mouseleave="hoveredCategory = ''"
                         style="position: relative;">
                        <span>{{ getCategoryDisplayName(cat) }}</span>
                        <span style="display: flex; align-items: center; gap: 2px;">
                            <span v-if="hoveredCategory === cat" style="display: flex; gap: 2px; margin-right: 4px;">
                                <button class="cat-action-btn" title="编辑分类" @click.stop="openEditModal(cat)">&#9998;</button>
                                <button class="cat-action-btn" title="删除分类" @click.stop="openDeleteModal(cat)">&#10005;</button>
                            </span>
                            <span class="category-count">{{ items.length }}</span>
                        </span>
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

            <!-- 编辑分类弹窗 -->
            <div v-if="editModal.visible" class="modal-overlay" @click.self="editModal.visible = false">
                <div class="modal-box">
                    <h3 style="margin: 0 0 16px; font-size: 16px;">编辑分类</h3>

                    <div class="modal-tabs">
                        <span class="modal-tab" :class="{active: editModal.tab === 'info'}"
                              @click="editModal.tab = 'info'">基本信息</span>
                        <span class="modal-tab" :class="{active: editModal.tab === 'pages'}"
                              @click="loadEditPages(); editModal.tab = 'pages'">管理页面</span>
                    </div>

                    <div v-if="editModal.tab === 'info'">
                        <div class="form-group">
                            <label class="form-label">分类标识 (Key)</label>
                            <input class="form-input" v-model="editModal.newKey">
                        </div>
                        <div class="form-group">
                            <label class="form-label">分类名称</label>
                            <input class="form-input" v-model="editModal.name">
                        </div>
                        <div class="form-group">
                            <label class="form-label">描述</label>
                            <input class="form-input" v-model="editModal.description">
                        </div>
                        <div style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px;">
                            <button class="btn" @click="editModal.visible = false">取消</button>
                            <button class="btn btn-primary" :disabled="editModal.saving" @click="saveEditCategory">
                                {{ editModal.saving ? '保存中...' : '保存' }}
                            </button>
                        </div>
                    </div>

                    <div v-if="editModal.tab === 'pages'">
                        <div v-if="editModal.loadingPages" style="text-align: center; padding: 20px; color: #a0aec0;">
                            加载中...
                        </div>
                        <div v-else-if="editModal.pages.length === 0" style="text-align: center; padding: 20px; color: #a0aec0;">
                            该分类下暂无页面
                        </div>
                        <div v-else>
                            <div style="margin-bottom: 12px; font-size: 13px; color: #718096;">
                                选择要移动的页面，然后选择目标分类：
                            </div>
                            <div style="max-height: 200px; overflow-y: auto; border: 1px solid #e2e8f0; border-radius: 4px; padding: 8px;">
                                <label v-for="slug in editModal.pages" :key="slug"
                                       style="display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; cursor: pointer;">
                                    <input type="checkbox" v-model="editModal.selectedPages" :value="slug">
                                    <span>{{ slug }}</span>
                                </label>
                            </div>
                            <div v-if="editModal.selectedPages.length > 0" style="margin-top: 12px;">
                                <label class="form-label">移动到分类：</label>
                                <select class="form-select" v-model="editModal.moveTarget">
                                    <option value="">请选择...</option>
                                    <option v-for="cat in availableMoveTargets" :key="cat" :value="cat">
                                        {{ getCategoryDisplayName(cat) }}
                                    </option>
                                </select>
                                <div style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 12px;">
                                    <button class="btn" @click="editModal.selectedPages = []">取消选择</button>
                                    <button class="btn btn-primary"
                                            :disabled="!editModal.moveTarget || editModal.movingPages"
                                            @click="doMovePages">
                                        {{ editModal.movingPages ? '移动中...' : '移动 ' + editModal.selectedPages.length + ' 个页面' }}
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
                            <button class="btn" @click="editModal.visible = false">关闭</button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 删除分类弹窗 -->
            <div v-if="deleteModal.visible" class="modal-overlay" @click.self="deleteModal.visible = false">
                <div class="modal-box">
                    <h3 style="margin: 0 0 8px; font-size: 16px; color: #e53e3e;">删除分类</h3>
                    <p style="color: #718096; font-size: 14px; margin-bottom: 16px;">
                        确定要删除分类「{{ getCategoryDisplayName(deleteModal.categoryKey) }}」吗？
                        <span v-if="deleteModal.pageCount > 0">该分类下有 <strong>{{ deleteModal.pageCount }}</strong> 个页面。</span>
                        <span v-else>该分类下没有页面。</span>
                    </p>

                    <div v-if="deleteModal.pageCount > 0">
                        <label class="form-label">页面处理方式：</label>
                        <div style="display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px;">
                            <label class="radio-option" :class="{selected: deleteModal.action === 'DELETE_PAGES'}">
                                <input type="radio" v-model="deleteModal.action" value="DELETE_PAGES">
                                <span>同时删除所有页面</span>
                            </label>
                            <label class="radio-option" :class="{selected: deleteModal.action === 'MOVE_TO_EXISTING'}">
                                <input type="radio" v-model="deleteModal.action" value="MOVE_TO_EXISTING">
                                <span>移动到已有分类</span>
                            </label>
                            <label class="radio-option" :class="{selected: deleteModal.action === 'MOVE_TO_NEW'}">
                                <input type="radio" v-model="deleteModal.action" value="MOVE_TO_NEW">
                                <span>移动到新建分类</span>
                            </label>
                            <label class="radio-option" :class="{selected: deleteModal.action === 'MOVE_TO_UNCATEGORIZED'}">
                                <input type="radio" v-model="deleteModal.action" value="MOVE_TO_UNCATEGORIZED">
                                <span>移至「未分类」</span>
                            </label>
                        </div>

                        <div v-if="deleteModal.action === 'MOVE_TO_EXISTING'" style="margin-bottom: 12px;">
                            <label class="form-label">目标分类：</label>
                            <select class="form-select" v-model="deleteModal.targetCategoryKey">
                                <option value="">请选择...</option>
                                <option v-for="cat in deleteTargetCategories" :key="cat.key" :value="cat.key">
                                    {{ cat.name }}（{{ cat.key }}）
                                </option>
                            </select>
                        </div>

                        <div v-if="deleteModal.action === 'MOVE_TO_NEW'" style="margin-bottom: 12px;">
                            <div class="form-group">
                                <label class="form-label">新分类标识</label>
                                <input class="form-input" v-model="deleteModal.newCategoryKey" placeholder="kebab-case">
                            </div>
                            <div class="form-group">
                                <label class="form-label">新分类名称</label>
                                <input class="form-input" v-model="deleteModal.newCategoryName" placeholder="中文名称">
                            </div>
                            <div class="form-group">
                                <label class="form-label">描述</label>
                                <input class="form-input" v-model="deleteModal.newCategoryDescription" placeholder="描述">
                            </div>
                        </div>
                    </div>

                    <div style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px;">
                        <button class="btn" @click="deleteModal.visible = false">取消</button>
                        <button class="btn" style="background: #e53e3e; color: white;"
                                :disabled="deleteModal.deleting || !canDelete"
                                @click="doDeleteCategory">
                            {{ deleteModal.deleting ? '删除中...' : '确认删除' }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            pages: [], selectedCategory: '', loaded: false,
            showNewCatForm: false, newCatKey: '', newCatName: '', newCatDesc: '', creatingCat: false,
            hoveredCategory: '',
            categoryDefs: [],
            editModal: {
                visible: false, tab: 'info',
                categoryKey: '', newKey: '', name: '', description: '',
                saving: false,
                pages: [], selectedPages: [], loadingPages: false,
                moveTarget: '', movingPages: false
            },
            deleteModal: {
                visible: false, categoryKey: '', pageCount: 0,
                action: 'MOVE_TO_UNCATEGORIZED',
                targetCategoryKey: '',
                newCategoryKey: '', newCategoryName: '', newCategoryDescription: '',
                deleting: false
            }
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
        },
        availableMoveTargets() {
            return Object.keys(this.categories).filter(c => c !== this.editModal.categoryKey);
        },
        deleteTargetCategories() {
            return this.categoryDefs.filter(c => c.key !== this.deleteModal.categoryKey);
        },
        canDelete() {
            if (this.deleteModal.pageCount === 0) return true;
            if (!this.deleteModal.action) return false;
            if (this.deleteModal.action === 'MOVE_TO_EXISTING') {
                return !!this.deleteModal.targetCategoryKey;
            }
            if (this.deleteModal.action === 'MOVE_TO_NEW') {
                return !!this.deleteModal.newCategoryKey && !!this.deleteModal.newCategoryName;
            }
            return true;
        }
    },
    created() { this.loadPages(); this.loadCategoryDefs(); },
    methods: {
        async loadPages() {
            try {
                this.pages = await API.listPages();
            } catch (e) {
                console.error('加载知识页面失败', e);
            }
            this.loaded = true;
        },
        async loadCategoryDefs() {
            try {
                this.categoryDefs = await API.listCategoriesManage();
            } catch (e) {
                console.error('加载分类定义失败', e);
            }
        },
        getCategoryDisplayName(key) {
            const def = this.categoryDefs.find(c => c.key === key);
            return def ? def.name : key;
        },
        async createCategory() {
            this.creatingCat = true;
            try {
                await API.createCategory(this.newCatKey.trim(), this.newCatName.trim(), this.newCatDesc.trim());
                this.newCatKey = '';
                this.newCatName = '';
                this.newCatDesc = '';
                this.showNewCatForm = false;
                await this.loadCategoryDefs();
            } catch (e) {
                alert('创建类别失败: ' + e.message);
            }
            this.creatingCat = false;
        },

        // --- 编辑分类 ---
        openEditModal(catKey) {
            const def = this.categoryDefs.find(c => c.key === catKey);
            this.editModal = {
                visible: true, tab: 'info',
                categoryKey: catKey,
                newKey: catKey,
                name: def ? def.name : catKey,
                description: def ? (def.description || '') : '',
                saving: false,
                pages: [], selectedPages: [], loadingPages: false,
                moveTarget: '', movingPages: false
            };
        },
        async loadEditPages() {
            if (this.editModal.pages.length > 0) return;
            this.editModal.loadingPages = true;
            try {
                this.editModal.pages = await API.getCategoryPages(this.editModal.categoryKey);
            } catch (e) {
                console.error('加载分类页面失败', e);
            }
            this.editModal.loadingPages = false;
        },
        async saveEditCategory() {
            this.editModal.saving = true;
            try {
                await API.updateCategory(this.editModal.categoryKey, {
                    newKey: this.editModal.newKey !== this.editModal.categoryKey ? this.editModal.newKey : null,
                    name: this.editModal.name,
                    description: this.editModal.description
                });
                this.editModal.visible = false;
                await Promise.all([this.loadPages(), this.loadCategoryDefs()]);
                if (this.selectedCategory === this.editModal.categoryKey && this.editModal.newKey) {
                    this.selectedCategory = this.editModal.newKey;
                }
            } catch (e) {
                alert('编辑分类失败: ' + e.message);
            }
            this.editModal.saving = false;
        },
        async doMovePages() {
            this.editModal.movingPages = true;
            try {
                await API.movePages(this.editModal.selectedPages, this.editModal.moveTarget);
                this.editModal.visible = false;
                await Promise.all([this.loadPages(), this.loadCategoryDefs()]);
            } catch (e) {
                alert('移动页面失败: ' + e.message);
            }
            this.editModal.movingPages = false;
        },

        // --- 删除分类 ---
        async openDeleteModal(catKey) {
            const pagesInCat = this.categories[catKey] || [];
            this.deleteModal = {
                visible: true, categoryKey: catKey,
                pageCount: pagesInCat.length,
                action: pagesInCat.length > 0 ? 'MOVE_TO_UNCATEGORIZED' : 'DELETE_PAGES',
                targetCategoryKey: '',
                newCategoryKey: '', newCategoryName: '', newCategoryDescription: '',
                deleting: false
            };
        },
        async doDeleteCategory() {
            this.deleteModal.deleting = true;
            try {
                const options = {
                    action: this.deleteModal.pageCount === 0 ? 'DELETE_PAGES' : this.deleteModal.action
                };
                if (options.action === 'MOVE_TO_EXISTING') {
                    options.targetCategoryKey = this.deleteModal.targetCategoryKey;
                } else if (options.action === 'MOVE_TO_NEW') {
                    options.newCategoryKey = this.deleteModal.newCategoryKey;
                    options.newCategoryName = this.deleteModal.newCategoryName;
                    options.newCategoryDescription = this.deleteModal.newCategoryDescription;
                }
                await API.deleteCategory(this.deleteModal.categoryKey, options);
                this.deleteModal.visible = false;
                if (this.selectedCategory === this.deleteModal.categoryKey) {
                    this.selectedCategory = '';
                }
                await Promise.all([this.loadPages(), this.loadCategoryDefs()]);
            } catch (e) {
                alert('删除分类失败: ' + e.message);
            }
            this.deleteModal.deleting = false;
        }
    }
};
