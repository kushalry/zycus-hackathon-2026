package com.zycus.hackathon.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

	@GetMapping("/status")
	public Map<String, String> status() {
		return Map.of("status", "ready");
	}
}
