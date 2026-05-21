const { createApp } = Vue;
const { createRouter, createWebHashHistory } = VueRouter;

const routes = [
    { path: '/', component: Dashboard },
    { path: '/upload', component: SourceUpload },
    { path: '/sources', component: SourceList },
    { path: '/sources/:id', component: SourceDetail },
    { path: '/kb', component: KnowledgeBase },
    { path: '/kb/:slug(.*)*', component: PageDetail },
    { path: '/search', component: SearchView }
];

const router = createRouter({
    history: createWebHashHistory(),
    routes
});

const app = createApp({});
app.component('nav-bar', NavBar);
app.use(router);
app.mount('#app');
