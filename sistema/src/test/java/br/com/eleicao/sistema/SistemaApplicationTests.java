package br.com.eleicao.sistema;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
class SistemaApplicationTests {

	@Test
	void contextLoads() {
	}
}