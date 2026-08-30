package com.staffs.leavebooking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires Firebase serviceAccountKey.json and RabbitMQ credentials — see Task 13 (integration tests)")
class LeavebookingApplicationTests {

    @Test
    void contextLoads() {
    }
}
