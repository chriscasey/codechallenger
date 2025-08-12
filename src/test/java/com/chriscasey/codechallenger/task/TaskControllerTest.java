package com.chriscasey.codechallenger.task;

import com.chriscasey.codechallenger.task.dto.TaskRequest;
import com.chriscasey.codechallenger.task.dto.TaskResponse;
import com.chriscasey.codechallenger.task.command.UpdateTaskCommand;
import com.chriscasey.codechallenger.auth.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private final User mockUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .password("secret")
            .role(null)
            .build();

    private final TaskResponse taskResponse = new TaskResponse(
            1L,
            "Test Task",
            "Test Description",
            false
    );

    @Test
    @WithMockUser
    void shouldCreateTask() throws Exception {
        TaskRequest request = new TaskRequest("Test Task", "Test Description", false);

        Mockito.when(taskService.createTask(any(User.class), eq(request)))
                .thenReturn(taskResponse);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Test Task")))
                .andExpect(jsonPath("$.completed", is(false)));
    }

    @Test
    @WithMockUser
    void shouldReturnListOfTasks() throws Exception {
        Mockito.when(taskService.getTasks(any(User.class), any(), any()))
                .thenReturn(List.of(taskResponse));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Task")));
    }

    @Test
    @WithMockUser
    void shouldReturnSingleTask() throws Exception {
        Mockito.when(taskService.getTaskById(eq(mockUser), eq(1L)))
                .thenReturn(taskResponse);

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Task")));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenTaskNotFound() throws Exception {
        Mockito.when(taskService.getTaskById(eq(mockUser), eq(999L)))
                .thenReturn(null);

        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldUpdateTask() throws Exception {
        UpdateTaskCommand update = new UpdateTaskCommand("Updated", "Updated Desc", true);

        Mockito.when(taskService.updateTask(eq(mockUser), eq(1L), eq(update)))
                .thenReturn(taskResponse);

        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Test Task")));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenUpdatingNonexistentTask() throws Exception {
        UpdateTaskCommand update = new UpdateTaskCommand("Doesn't", "Exist", true);

        Mockito.when(taskService.updateTask(eq(mockUser), eq(999L), eq(update)))
                .thenReturn(null);

        mockMvc.perform(put("/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldDeleteTask() throws Exception {
        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isNoContent());
    }
}
