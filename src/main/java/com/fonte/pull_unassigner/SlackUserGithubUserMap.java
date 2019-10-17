package com.fonte.pull_unassigner;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SlackUserGithubUserMap
{
	private Map<String, String> users = new HashMap<>();
}
