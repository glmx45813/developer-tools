package com.glmx.tools.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/ai-chat")
    public String aiChat() {
        return "ai-chat";
    }

    @GetMapping("/prompt-template")
    public String promptTemplate() {
        return "prompt-template";
    }
}
