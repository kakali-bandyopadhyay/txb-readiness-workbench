package com.synthetic.readiness.summary;

import com.synthetic.readiness.readiness.WorkbenchService;
import com.synthetic.readiness.summary.ExecutiveSummary;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

class SummaryServiceTest {
    @Test void modelDraftUsesInjectedFakeAndIsMarkedDraft() throws Exception {
        WorkbenchService workbench=mock(WorkbenchService.class);
        when(workbench.dashboard(1,null)).thenReturn(new WorkbenchService.Dashboard(1L,null,50,1,2,0,1,List.of(),List.of(),List.of(),List.of(),"Not recorded",0,List.of()));
        SummaryTextClient fake=facts->{assertTrue(facts.contains("readiness=50%"));return "Synthetic fake summary.";};
        ExecutiveSummaryService service=new ExecutiveSummaryService(workbench,fake);EntityManager em=mock(EntityManager.class);service.em=em;
        ExecutiveSummary draft=service.generate(1,"MODEL","demo");
        assertEquals("DRAFT",draft.status);assertEquals("MODEL",draft.mode);assertTrue(draft.text.startsWith("DRAFT:"));verify(em).persist(draft);
    }
}

