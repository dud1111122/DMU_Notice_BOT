package com.joowest.noticebot;

import com.joowest.noticebot.service.NoticeCrawlerService;
import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:noticebot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.task.scheduling.enabled=false"
})
class NoticebotApplicationTests {

	@MockBean
	private JDA jda;

	@MockBean
	private NoticeCrawlerService noticeCrawlerService;

	@Test
	void contextLoads() {
	}

}
