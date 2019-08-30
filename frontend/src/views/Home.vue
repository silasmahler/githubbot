<template>
  <h3>Home Page</h3>
</template>

<script>
  import { mapState, mapActions } from 'vuex';

  export default {
    name: 'app',
    computed: {
      ...mapState({
        alert: (state) => state.alert,
      })
    },
    methods: {
      ...mapActions({
        clearAlert: 'alert/clear',
      })
    }
  };
</script>
