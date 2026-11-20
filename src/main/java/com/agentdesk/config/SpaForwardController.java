package com.agentdesk.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 单 Jar 部署时把前端路由转发到 index.html（前端资源位于 classpath:/static）。
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/chat", "/knowledge"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
