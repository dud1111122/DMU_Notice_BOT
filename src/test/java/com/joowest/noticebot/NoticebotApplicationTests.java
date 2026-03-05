package com.joowest.noticebot;

import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class NoticebotApplicationTests {

	@MockBean
	private JDA jda;

	@Test
	void contextLoads() {
	}

}
