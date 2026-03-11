package com.travel.travelapp;

import com.travel.travelapp.repository.TripScheduleRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TravelappApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TripScheduleRepository tripScheduleRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void readinessProbeShouldBeUpAfterStartup() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void seededChapraSchedulesShouldBeThreeHoursApart() {
		List<LocalTime> departures = tripScheduleRepository
				.findByTravelDateAndRouteSourceIgnoreCaseAndRouteDestinationIgnoreCaseAndActiveTrue(
						LocalDate.now().plusDays(1), "Patna", "Chapra")
				.stream()
				.map(schedule -> schedule.getDepartureTime())
				.sorted()
				.toList();

		assertThat(departures).containsExactly(
				LocalTime.of(9, 30),
				LocalTime.of(12, 30),
				LocalTime.of(15, 30));
	}

}
