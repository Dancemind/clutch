package com.clutch.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
// test/resources/application-test.yml
@ActiveProfiles("test")
@Transactional
public abstract class BaseServiceIntegrationTest {
}

