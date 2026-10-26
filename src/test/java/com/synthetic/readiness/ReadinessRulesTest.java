package com.synthetic.readiness;

import com.synthetic.readiness.release.*;
import com.synthetic.readiness.readiness.*;
import com.synthetic.readiness.uat.*;
import com.synthetic.readiness.control.*;
import com.synthetic.readiness.decision.*;
import com.synthetic.readiness.audit.*;
import com.synthetic.readiness.stabilisation.*;
import com.synthetic.readiness.summary.*;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class ReadinessRulesTest {
    private final WorkbenchService service = new WorkbenchService();
    private ReadinessAction base() { ReadinessAction a=new ReadinessAction();a.mandatory=true;a.dueDate=LocalDate.now().minusDays(30);a.status="IN_PROGRESS";return a; }

    @Test void completeNeedsOwnerRecorded() { var a=base();a.completedAt=LocalDateTime.now();a.evidenceReference="SYNTH-1";assertFalse(service.actionComplete(a)); }
    @Test void completeNeedsTimestamp() { var a=base();a.completedBy="Owner";a.evidenceReference="SYNTH-1";assertFalse(service.actionComplete(a)); }
    @Test void completeNeedsEvidence() { var a=base();a.completedBy="Owner";a.completedAt=LocalDateTime.now();assertFalse(service.actionComplete(a)); }
    @Test void dueDateDoesNotCompleteAction() { var a=base();a.dueDate=LocalDate.now().minusDays(100);assertFalse(service.actionComplete(a)); }
    @Test void attachedEvidenceWithoutOwnerDoesNotComplete() { var a=base();a.evidenceReference="SYNTH-2";assertFalse(service.actionComplete(a)); }
    @Test void completeWhenOwnerTimestampAndEvidenceExist() { var a=base();a.completedBy="Owner";a.completedAt=LocalDateTime.now();a.evidenceReference="SYNTH-3";assertTrue(service.actionComplete(a)); }
    @Test void acceptedExceptionNeedsApprover() { var a=base();a.exceptionAccepted=true;a.exceptionRationale="Illustrative reason";assertFalse(service.actionComplete(a)); }
    @Test void acceptedExceptionNeedsRationale() { var a=base();a.exceptionAccepted=true;a.exceptionApprover="Lead";assertFalse(service.actionComplete(a)); }
    @Test void unacceptedExceptionDoesNotComplete() { var a=base();a.exceptionApprover="Lead";a.exceptionRationale="Illustrative reason";assertFalse(service.actionComplete(a)); }
    @Test void acceptedExceptionCompletesWithApproverAndRationale() { var a=base();a.exceptionAccepted=true;a.exceptionApprover="Lead";a.exceptionRationale="Illustrative reason";assertTrue(service.actionComplete(a)); }
    @Test void ordinaryActionCanStillBeReportedCompleteByEvidenceRule() { var a=base();a.mandatory=false;a.completedBy="Owner";a.completedAt=LocalDateTime.now();a.evidenceReference="SYNTH-4";assertTrue(service.actionComplete(a)); }
    @Test void completionPercentageUsesRoundedNumeratorOverDenominator() { assertEquals(67,WorkbenchService.completionPercent(2,3)); }
    @Test void zeroMandatoryActionsAreFullyReady() { assertEquals(100,WorkbenchService.completionPercent(0,0)); }
    @Test void percentageDoesNotExceedOneHundred() { assertEquals(100,WorkbenchService.completionPercent(11,10)); }
    @Test void percentageDoesNotGoBelowZero() { assertEquals(0,WorkbenchService.completionPercent(-1,10)); }
    @Test void criticalBlockerSuggestsNoGo() { assertEquals("No-Go",WorkbenchService.suggestedStatus(List.of(new WorkbenchService.Blocker("Critical defect gate","Synthetic defect","Open")))); }
    @Test void highBlockerSuggestsConditionalGo() { assertEquals("Conditional Go",WorkbenchService.suggestedStatus(List.of(new WorkbenchService.Blocker("High defect condition","Synthetic defect","Open")))); }
    @Test void missingActionEvidenceSuggestsConditionalGo() { assertEquals("Conditional Go",WorkbenchService.suggestedStatus(List.of(new WorkbenchService.Blocker("Mandatory action evidence rule","Synthetic action","Missing")))); }
    @Test void noBlockersSuggestGo() { assertEquals("Go",WorkbenchService.suggestedStatus(List.of())); }
    @Test void criticalBlockerTakesPriorityOverOtherBlockers() { assertEquals("No-Go",WorkbenchService.suggestedStatus(List.of(new WorkbenchService.Blocker("High defect condition","High","Open"),new WorkbenchService.Blocker("Critical defect gate","Critical","Open")))); }
    @TestFactory Stream<DynamicTest> dueDatesNeverCompleteAnUnrecordedAction() {
        return IntStream.rangeClosed(1,12).mapToObj(days -> DynamicTest.dynamicTest("elapsed due date does not complete item " + days, () -> { var a=base();a.dueDate=LocalDate.now().minusDays(days);assertFalse(service.actionComplete(a)); }));
    }
}

