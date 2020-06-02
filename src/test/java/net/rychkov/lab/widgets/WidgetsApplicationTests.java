package net.rychkov.lab.widgets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.rychkov.lab.widgets.api.controllers.WidgetController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WidgetsApplicationTests {

	@Autowired
	WidgetController controller;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
		assertThat(controller).isNotNull();
	}

	@Test
	@DirtiesContext
	public void addTest() throws Exception {

		this.mockMvc.perform(post("/widget")
				.contentType(MediaType.APPLICATION_JSON).content("{\"x\":10,\"y\":10,\"z\":10,\"width\":10,\"height\":10}"))
				.andDo(print()).andExpect(status().isCreated());
	}

	@Test
	@DirtiesContext
	public void addAndUpdateTest() throws Exception {

		this.mockMvc.perform(post("/widget")
				.contentType(MediaType.APPLICATION_JSON).content("{\"x\":10,\"y\":10,\"z\":10,\"width\":10,\"height\":10}"))
				.andDo(print()).andExpect(status().isCreated());

		this.mockMvc.perform(patch("/widget/1")
				.contentType(MediaType.APPLICATION_JSON).content("{\"x\":11,\"y\":11,\"z\":11,\"width\":11,\"height\":11}"))
				.andDo(print()).andExpect(status().isOk());
	}

	@Test
	@DirtiesContext
	public void filterTest() throws Exception {

		this.mockMvc.perform(
				post("/widget")
				.contentType(MediaType.APPLICATION_JSON).content("{\"x\":10,\"y\":10,\"z\":3,\"width\":11,\"height\":12}")
		).andExpect(status().isCreated());

		this.mockMvc.perform(
				post("/widget")
				.contentType(MediaType.APPLICATION_JSON).content("{\"x\":200,\"y\":20,\"z\":4,\"width\":21,\"height\":20}")
		).andExpect(status().isCreated());

		this.mockMvc.perform(
				post("/widget")
				.contentType(MediaType.APPLICATION_JSON).content("{\"x\":25,\"y\":25,\"z\":1,\"width\":50,\"height\":50}")
		).andExpect(status().isCreated());


		this.mockMvc.perform(
				get("/widget/filter?x1=0&y1=0&x2=50&y2=100")
		).andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$[0].id").value(1))
		.andExpect(jsonPath("$[1].id").value(3));
	}
}
