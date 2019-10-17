package com.fonte.pull_unassigner;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.catalina.connector.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class Unassigner
{
	String slackURL = "https://slack.com/api/users.list";
	String githubBaseURL = "https://api.github.com";
	String githubAPIAcceptHeader = "application/vnd.github.hellcat-preview+json";

	@Value("${slack_token}")
	String slackToken;
	@Value("${github_maintainer}")
	String githubMaintainer;
	@Value("${github_token}")
	String githubMaintainerToken;
	@Value("${github_org}")
	String githubOrg;
	@Value("${github_team_id}")
	String githubTeamId;
	@Value("${github_team_name}")
	String githubTeamName;

	@Autowired
	SlackUserGithubUserMap slackUserGithubUserMap;

	Set<String> slackVacationStatusSet = new HashSet<>(Arrays.asList("palm_tree", "red_circle", "face_with_thermometer"));

	@Scheduled(cron = "${cron.expression}")
//	@Scheduled(fixedRate = 3600000, initialDelay = 1000)
	public void updateGithubTeam() throws UnirestException, IllegalStateException
	{
		Map<String, Boolean> githubUsersActiveMap = new HashMap<>();

		UnirestUtil.setUnirestObjectMapper();

		if (Objects.isNull(githubMaintainer) || Objects.isNull(githubMaintainerToken) || Objects.isNull(githubOrg) || (Objects.isNull(githubTeamId) && Objects.isNull(githubTeamName)))
		{
			throw new IllegalStateException("Missing Setup Information");
		}

		HttpResponse<JsonNode> jsonResponse = Unirest.post(slackURL)
													 .header("Content-Type", "application/x-www-form-urlencoded")
													 .queryString("token", slackToken)
													 .asJson();
		if (jsonResponse.getStatus() == Response.SC_OK)
		{

			JsonNode jsonNode = jsonResponse.getBody();
			JSONObject responseObject = jsonNode.getObject();
			if (responseObject.getBoolean("ok"))
			{
				JSONArray dataList = responseObject.getJSONArray("members");
				for (int i=0; i<dataList.length(); i++)
				{
					JSONObject memberItem = dataList.getJSONObject(i);
					String slackName = memberItem.getString("name").replaceAll("\\.", "_");
					String githubName = slackUserGithubUserMap.getUsers().get(slackName);
					if (Objects.nonNull(githubName))
					{
						JSONObject memberProfileItem = memberItem.getJSONObject("profile");
						String slackStatusEmoji = memberProfileItem.getString("status_emoji").replaceAll(":", "");

						githubUsersActiveMap.put(githubName, !slackVacationStatusSet.contains(slackStatusEmoji));
					}
				}
			}
		}

		if (Objects.isNull(githubTeamId))
		{
			String githubURL = githubBaseURL + "/orgs/" + githubOrg + "/teams";
			jsonResponse = Unirest.get(githubURL)
								  .basicAuth(githubMaintainer, githubMaintainerToken)
								  .header("Accept", githubAPIAcceptHeader)
								  .asJson();

			if (jsonResponse.getStatus() == Response.SC_OK)
			{

				JsonNode jsonNode = jsonResponse.getBody();
				JSONArray dataList = jsonNode.getArray();
				for (int i=0; i<dataList.length(); i++)
				{
					JSONObject orgItem = dataList.getJSONObject(i);
					String orgSlug = orgItem.getString("slug").replaceAll("\\.", "_");
					if (Objects.equals(orgSlug, githubTeamName))
					{
						githubTeamId = String.valueOf(orgItem.getInt("id"));
						break;
					}
				}
			}
		}

		for (Map.Entry<String, Boolean> githubUserActive : githubUsersActiveMap.entrySet())
		{
			String githubUser = githubUserActive.getKey();
			Boolean active = githubUserActive.getValue();
			String githubURL = githubBaseURL + "/teams/" + githubTeamId + "/memberships/" + githubUser;
			if (Objects.equals(githubMaintainer, githubUser))
			{
				// don't remove maintainer from team
				// preferably this maintainer is not a real user, and is excluded from assignment rotation
				continue;
			}
			if (active)
			{
				jsonResponse = Unirest.put(githubURL)
									  .basicAuth(githubMaintainer, githubMaintainerToken)
									  .header("Accept", githubAPIAcceptHeader)
									  .queryString("role", "member")
									  .asJson();
				if (jsonResponse.getStatus() == Response.SC_OK)
				{
					JsonNode jsonNode = jsonResponse.getBody();
					JSONObject responseObject = jsonNode.getObject();
				}
			}
			else
			{
				jsonResponse = Unirest.delete(githubURL)
									  .basicAuth(githubMaintainer, githubMaintainerToken)
									  .asJson();
				if (jsonResponse.getStatus() == Response.SC_NO_CONTENT)
				{
					// DELETE successful
				}
			}
		}


	}
}
