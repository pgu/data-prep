package org.talend.dataprep.maintenance.upgrade;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.task.TaskExecutor;
import org.talend.dataprep.security.Security;
import org.talend.dataprep.upgrade.UpgradeService;
import org.talend.tenancy.ForAll;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeTaskTest {

    @InjectMocks
    private UpgradeTask upgradeTask;

    @Mock
    private Security security;

    @Mock
    private TaskExecutor executor;

    @Mock
    private ForAll forAll;

    @Mock
    private UpgradeService upgradeService;

    @Test
    public void shouldPerformUpgrade() {
        // given
        when(upgradeService.needUpgrade()).thenReturn(true);

        final ForAll.ForAllConditionBuilder forAllConditionBuilder = mock(ForAll.ForAllConditionBuilder.class);
        when(forAllConditionBuilder.operational(any())).thenReturn(() -> true);
        when(forAll.condition()).thenReturn(forAllConditionBuilder);

        doAnswer((Answer<ForAll.ForAllConditionBuilder>) invocation -> {
            ((Runnable) invocation.getArguments()[1]).run();
            return null;
        }).when(forAll).execute(any(), any());

        doAnswer((Answer<Void>) invocation -> {
            ((Runnable) invocation.getArguments()[1]).run();
            return null;
        }).when(forAll).execute(any(), any());

        doAnswer((Answer<Void>) invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(executor).execute(any());

        // when
        upgradeTask.upgradeTask();

        // then
        verify(security, times(2)).getTenantId();
        verify(upgradeService, times(1)).upgradeVersion();
    }

    @Test
    public void shouldNotPerformUpgrade_noUpgradeToApply() {
        // given
        final ForAll.ForAllConditionBuilder forAllConditionBuilder = mock(ForAll.ForAllConditionBuilder.class);
        when(forAllConditionBuilder.operational(any())).thenReturn(() -> true);
        when(forAll.condition()).thenReturn(forAllConditionBuilder);

        doAnswer((Answer<ForAll.ForAllConditionBuilder>) invocation -> {
            ((Runnable) invocation.getArguments()[1]).run();
            return null;
        }).when(forAll).execute(any(), any());

        when(upgradeService.needUpgrade()).thenReturn(false);

        doAnswer((Answer<Void>) invocation -> {
            ((Runnable) invocation.getArguments()[1]).run();
            return null;
        }).when(forAll).execute(any(), any());

        // when
        upgradeTask.upgradeTask();

        // then
        verify(security, times(0)).getTenantId();
        verify(upgradeService, times(0)).upgradeVersion();
    }

    @Test
    public void shouldNotPerformUpgrade_repositoryNotSuited() {
        // given
        final ForAll.ForAllConditionBuilder forAllConditionBuilder = mock(ForAll.ForAllConditionBuilder.class);
        when(forAllConditionBuilder.operational(any())).thenReturn(() -> false);
        when(forAll.condition()).thenReturn(forAllConditionBuilder);

        doAnswer((Answer<ForAll.ForAllConditionBuilder>) invocation -> {
            ((Runnable) invocation.getArguments()[1]).run();
            return null;
        }).when(forAll).execute(any(), any());

        when(upgradeService.needUpgrade()).thenReturn(true);

        doAnswer((Answer<Void>) invocation -> {
            ((Runnable) invocation.getArguments()[1]).run();
            return null;
        }).when(forAll).execute(any(), any());

        // when
        upgradeTask.upgradeTask();

        // then
        verify(security, times(0)).getTenantId();
        verify(upgradeService, times(0)).upgradeVersion();
    }

}
