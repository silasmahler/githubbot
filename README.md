# GitHub Bot
This is a bot for GitHub to manage invitations to organizations for events like hackathons. 

## How to use
To use this bot set up an OAuth app for your organizations GitHub account. Next clone the 
repository. Create a `secretConfig.properties` file from the `secretConfig.properties.example`
from the `resources` directory by filling in your organizations `client_id` and `client_secret` 
and an access token belonging to one of the organization owners. Best use an account which's only
purpose is to provide this access and share it with the other admin members of your organization.

After that change to the root directory and do `gradle bootRun` to start GitHub Bot.

## Misc
Do not commit the `secretConfig.properties` to any public repository or share it with someone. 
It contains your organizations `client_id` and `client_secret` and an access token of an account
with admin rights to your organization. Someone could use it to impersonate or to damge you or
your organization.