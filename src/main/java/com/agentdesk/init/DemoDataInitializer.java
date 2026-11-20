package com.agentdesk.init;

import com.agentdesk.knowledge.Document;
import com.agentdesk.knowledge.DocumentMapper;
import com.agentdesk.knowledge.DocumentService;
import com.agentdesk.user.User;
import com.agentdesk.user.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * 演示数据初始化：首次启动自动创建 demo 账号并入库 3 份示例文档，保证克隆后开箱即用。
 *
 * <p>可通过 {@code agentdesk.demo.enabled=false} 关闭。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agentdesk.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "123456";

    private static final String[] DEMO_DOCUMENTS = {
            "员工报销制度.md",
            "产品技术架构说明.md",
            "新员工入职指南.md"
    };

    private final UserMapper userMapper;
    private final DocumentMapper documentMapper;
    private final DocumentService documentService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        User demo = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, DEMO_USERNAME));
        if (demo == null) {
            demo = new User();
            demo.setUsername(DEMO_USERNAME);
            demo.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
            demo.setNickname("演示用户");
            demo.setRole("USER");
            demo.setCreatedAt(LocalDateTime.now());
            userMapper.insert(demo);
            log.info("已创建演示账号 {}/{}", DEMO_USERNAME, DEMO_PASSWORD);
        }

        Long existing = documentMapper.selectCount(Wrappers.<Document>lambdaQuery()
                .eq(Document::getUserId, demo.getId()));
        if (existing != null && existing > 0) {
            return;
        }

        for (String fileName : DEMO_DOCUMENTS) {
            try (InputStream inputStream = new ClassPathResource("demo-docs/" + fileName).getInputStream()) {
                documentService.ingest(demo.getId(), fileName, inputStream.readAllBytes());
                log.info("已导入演示文档：{}", fileName);
            } catch (Exception e) {
                log.warn("导入演示文档失败：{}，原因：{}", fileName, e.getMessage());
            }
        }
    }
}
