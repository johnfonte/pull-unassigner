# Pull Unassigner

Set up according to this [article](https://medium.com/babylon-engineering/bringing-some-order-to-pull-request-reviews-27ac55d181bb).

Add the Slack token and GitHub user token in the application.properties sample, add your GitHub org and team as well. Then, replace application.properties with your version.

Set up the map of users with the mapping users.slack_username = github_username. Replace any '.' characters in Slack usernames with underscores.

Change the Cron configuration in the properties file to your desired schedule.

There are examples of valid cURL calls that this functionality is based upon in curl_examples.
