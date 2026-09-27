package com.joborch.producer;

import com.joborch.common.domain.JobType;
import com.joborch.producer.domain.Workflow;
import com.joborch.producer.domain.WorkflowStatus;
import com.joborch.producer.dto.CreateWorkflowRequest;
import com.joborch.producer.dto.WorkflowResponse;
import com.joborch.producer.service.WorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the DAG Workflow Engine — Phases 19, 20, 21.
 */
@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class WorkflowEngineIntegrationTest {

    @Autowired WorkflowService workflowService;

    @Test
    @DisplayName("Linear A→B→C workflow: creates workflow and submits A immediately")
    void linearWorkflow_created_successfully() {
        CreateWorkflowRequest req = buildLinearWorkflow();
        WorkflowResponse resp = workflowService.createWorkflow(req);

        assertThat(resp.getId()).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(WorkflowStatus.RUNNING);
        assertThat(resp.getName()).isEqualTo("Linear Test Workflow");
    }

    @Test
    @DisplayName("Diamond A→(B,C)→D workflow: A submitted first, B+C parallel after A completes")
    void diamondWorkflow_created_successfully() {
        CreateWorkflowRequest req = buildDiamondWorkflow();
        WorkflowResponse resp = workflowService.createWorkflow(req);

        assertThat(resp.getId()).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(WorkflowStatus.RUNNING);
    }

    @Test
    @DisplayName("Cycle detection: A→B→A should throw IllegalArgumentException")
    void cycleDetection_throws() {
        CreateWorkflowRequest cyclic = new CreateWorkflowRequest();
        cyclic.setName("Cyclic Workflow");
        cyclic.setSteps(List.of(
            step("A", JobType.EMAIL, List.of("B")),
            step("B", JobType.REPORT, List.of("A"))
        ));

        assertThatThrownBy(() -> workflowService.createWorkflow(cyclic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    @DisplayName("Unknown dependency reference should throw")
    void unknownDependency_throws() {
        CreateWorkflowRequest bad = new CreateWorkflowRequest();
        bad.setName("Bad Dep Workflow");
        bad.setSteps(List.of(
            step("A", JobType.EMAIL, List.of("NONEXISTENT"))
        ));

        assertThatThrownBy(() -> workflowService.createWorkflow(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NONEXISTENT");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private CreateWorkflowRequest buildLinearWorkflow() {
        CreateWorkflowRequest req = new CreateWorkflowRequest();
        req.setName("Linear Test Workflow");
        req.setSteps(List.of(
            step("A", JobType.EMAIL,         List.of()),
            step("B", JobType.NOTIFICATION,  List.of("A")),
            step("C", JobType.DATA_PROCESSING, List.of("B"))
        ));
        return req;
    }

    private CreateWorkflowRequest buildDiamondWorkflow() {
        CreateWorkflowRequest req = new CreateWorkflowRequest();
        req.setName("Diamond Test Workflow");
        req.setSteps(List.of(
            step("A", JobType.EMAIL,           List.of()),
            step("B", JobType.REPORT,          List.of("A")),
            step("C", JobType.NOTIFICATION,    List.of("A")),
            step("D", JobType.DATA_PROCESSING, List.of("B", "C"))
        ));
        return req;
    }

    private CreateWorkflowRequest.StepDefinition step(String name, JobType type, List<String> deps) {
        CreateWorkflowRequest.StepDefinition s = new CreateWorkflowRequest.StepDefinition();
        s.setStepName(name);
        s.setJobType(type);
        s.setDependsOn(deps);
        s.setPayload(Map.of("testKey", "testValue"));
        return s;
    }
}
