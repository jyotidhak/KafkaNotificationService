package com.notification.service.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.json.JSONObject;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

@Service
public class RedisService {
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	private static String Steps_prefix="_Step";
	private static String Category_prefix="_Category";


	public void saveRule(String key, String value) {
		redisTemplate.opsForValue().set(key, value);
	}
	public void saveUserRoleAndRulesKeys(String role, List<String> ruleKeys) {
		redisTemplate.opsForValue().set(role+Category_prefix, ruleKeys);
	}

	public void savePipelineSteps(String role, List<String> steps) {
		redisTemplate.opsForValue().set(role+Steps_prefix, steps);
	}

	public List<String> getPipelineSteps(String category) {
		Object object=redisTemplate.opsForValue().get(category+Steps_prefix);
		if(object==null){
			return Collections.EMPTY_LIST;
		}

		List<String> steps = (List<String>) object;
		return steps;
	}

	public List<String> getRuleKeysByUserRole(String userCategory) {
		Object object=redisTemplate.opsForValue().get(userCategory+Category_prefix);
		if(object==null){
			return Collections.EMPTY_LIST;
		}

		List<String> keys = (List<String>) object;
		return keys;
	}

	public JSONObject evaluateAndUpdate(JSONObject payload) {
		ObjectMapper objectMapper = new ObjectMapper();
		HashMap<String, Object> map = null;

		List<String> keys=new ArrayList<>();
		List<String> steps=new ArrayList<>();
		try {
			map = objectMapper.readValue(payload.toString(), HashMap.class);
			steps=getPipelineSteps(map.get("category").toString());

			keys =getRuleKeysByUserRole(map.get("category").toString());

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		Map<String, String> rules = new HashMap<>();

		for(String key: keys){
			String rule= redisTemplate.opsForValue().get(key).toString();
			if(rule!=null) {
				rules.put(key, rule);
			}
		}
		JSONObject response = new JSONObject(map);

		for (String step : steps) {
			String validationFailure = executePipelineSteps(step, rules, map);
			if (!validationFailure.equals("success")){
				response.put("status", "failure");
				response.put("message", validationFailure);
				return response;
			}
		}
		return response;
	}

	private String executePipelineSteps(String step, Map<String, String> rules, Map<String, Object> payload) {
		for (Map.Entry<String, String> entry : rules.entrySet()) {
			if (entry.getKey().contains(step)) {
				try {
					Map<String, Object> context = new HashMap<>(payload);
					context.put("payload", payload);
					Object result = MVEL.eval(entry.getValue(), context);
					if (result == null || (result instanceof Boolean && !(Boolean) result)) {
						String failureMessage = "Failure in step: " + step + " - Rule: " + entry.getKey();
						//logger.error(failureMessage);
						return failureMessage;
					}
				} catch (Exception e) {
					String failureMessage = "Exception in step: " + step + " - Rule: " + entry.getKey() + " - " + e.getMessage();
					//logger.error(failureMessage, e);
					return failureMessage;
				}
			}
		}
		return "success";
	}
}