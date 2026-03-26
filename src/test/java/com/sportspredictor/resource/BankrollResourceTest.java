package com.sportspredictor.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.BankrollService;
import com.sportspredictor.service.HistoryService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BankrollResource}. */
@ExtendWith(MockitoExtension.class)
class BankrollResourceTest {

    @Mock
    private BankrollService bankrollService;

    @Mock
    private HistoryService historyService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BankrollResource bankrollResource;

    @Test
    void registersFourResources() {
        List<SyncResourceSpecification> resources = bankrollResource.bankrollResources();

        assertThat(resources).hasSize(4);
    }
}
