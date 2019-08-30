<template>
    <div>
        <h1>Hi {{account.user.firstName}}!</h1>
        <p>
            <router-link to="/login">Logout</router-link>
        </p>
    </div>



    <form class="form-horizontal">
        <fieldset>
            <!-- Form Name -->
            <legend>Repository Registration Form</legend>

            <!-- Text input-->
            <div class="form-group">
                <label class="col-md-4 control-label">Team and Repository Name</label>
                <div class="col-md-4">
                    <input v-model="repoName" type="text" placeholder="Team and Repository Name" class="form-control input-md" required="">
                </div>
            </div>

            <!-- Textarea -->
            <div class="form-group">
                <label class="col-md-4 control-label">Description</label>
                <div class="col-md-4">
                    <textarea v-model="description" class="form-control">Say somethong about your project</textarea>
                </div>
            </div>

            <!-- Text input-->
            <div class="form-group">
                <label class="col-md-4 control-label">You</label>
                <div class="col-md-4">
                    <input v-model="creator" type="text" placeholder="Your GitHub Name" class="form-control input-md" required="">
                </div>
            </div>

            <!-- Text input-->
            <div class="form-group">
                <label class="col-md-4 control-label">Teammate 1</label>
                <div class="col-md-4">
                    <input v-model="teammateOne" type="text" placeholder="GitHub Name" class="form-control input-md">
                </div>
            </div>

            <!-- Text input-->
            <div class="form-group">
                <label class="col-md-4 control-label">Teammate 2</label>
                <div class="col-md-4">
                    <input v-model="teammateTwo" type="text" placeholder="GitHub Name" class="form-control input-md">
                </div>
            </div>

            <!-- Text input-->
            <div class="form-group">
                <label class="col-md-4 control-label">Teammate 3</label>
                <div class="col-md-4">
                    <input v-model="teammateThree" type="text" placeholder="GitHub Name" class="form-control input-md">
                </div>
            </div>

            <!-- Text input-->
            <div class="form-group">
                <label class="col-md-4 control-label">Teammate 4</label>
                <div class="col-md-4">
                    <input v-model="teammateFour" type="text" placeholder="GitHub Name" class="form-control input-md">
                </div>
            </div>

            <!-- Button (Double) -->
            <div class="form-group">
                <label class="col-md-4 control-label"></label>
                <div class="col-md-8">
                    <button v-on:click="confirm()" class="btn btn-success">Confirm</button>
                    <button v-on:click="cancel()" class="btn btn-danger">Cancel</button>
                </div>
            </div>

        </fieldset>
    </form>
</template>

<script>
    import { mapState, mapActions } from 'vuex';

    export default {
        name: 'RegisterProject',
        data() {
            return {
                repoName: '',
                creator: '',
                teammateOne: '',
                teammateTwo: '',
                teammateThree: '',
                teammateFour: '',
                description: ''
            };
        },
        methods: {
            confirm() {
                // TODO call backend and create repo from template
            },
            cancel() {
                this.repoName = '';
                this.creator = '';
                this.teammateOne = '';
                this.teammateTwo = '';
                this.teammateThree = '';
                this.teammateFour = '';
                this.description = '';
            },
            ...mapActions('users', {
                getAllUsers: 'getAll',
                deleteUser: 'delete'
            },)
        },
        computed: {
            ...mapState({
                account: (state) => state.account,
                users: (state) => state.users.all
            })
        },
        created() {
            this.getAllUsers();
        }
    };
</script>