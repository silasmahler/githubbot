import Vue from 'vue';
import Router from 'vue-router';
import Home from '../views/Home.vue';
import Login from '../views/Login.vue';

Vue.use(Router);

const router = new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {path: '/', name: 'home', component: Home},
    {path: '/login', name: 'Login', component: Login},
    {path: '*', redirect: '/'}
  ]
});

router.beforeEach((to: any, from: any, next: any) => {
  // redirect to login page if not logged in and trying to access a restricted page
  const publicPages = ['/login', '/'];
  const authRequired = !publicPages.includes(to.path);
  const loggedIn = localStorage.getItem('user');

  if (authRequired && !loggedIn) {
    return next('/login');
  }

  next();
});

export default router;
