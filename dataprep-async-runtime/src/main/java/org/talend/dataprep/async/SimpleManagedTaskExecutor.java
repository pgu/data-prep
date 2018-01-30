// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.async;

import static java.util.Optional.ofNullable;
import static org.talend.dataprep.async.AsyncExecution.Status.DONE;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.fullrun.AsyncExecutionResult;
import org.talend.dataprep.async.progress.ExecutionContext;
import org.talend.dataprep.async.repository.ManagedTaskRepository;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.TransformationErrorCodes;
import org.talend.dataprep.security.Security;
import org.talend.dataprep.transformation.pipeline.Signal;

/**
 * Managed task executor based on a local thread pool.
 *
 * @param <T> the type of managed tasks.
 */
@Component
@ConditionalOnProperty(name = "execution.executor.local", matchIfMissing = true)
public class SimpleManagedTaskExecutor<T extends AsyncExecutionResult> implements ManagedTaskExecutor<T> {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleManagedTaskExecutor.class);

    /** Where the tasks are stored. */
    @Autowired
    private ManagedTaskRepository repository;

    /** The thread pool that execute the tasks. */
    @Autowired
    @Qualifier("managedTaskEngine")
    private AsyncListenableTaskExecutor delegate;

    @Autowired
    private Security security;

    /** List of tasks to run. */
    private final Map<String, ListenableFuture<T>> futures = new ConcurrentHashMap<>();

    @Override
    public AsyncExecution resume(ManagedTaskCallable<T> task, String executionId) {
        LOGGER.debug("Resuming execution '{}' from repository '{}'", executionId, repository);
        final AsyncExecution execution = repository.get(executionId);
        if (execution == null) {
            LOGGER.error("Execution #{} can be resumed (not found).", executionId);
            throw new TDPException(TransformationErrorCodes.UNABLE_TO_RESUME_EXECUTION, ExceptionContext.withBuilder().put("id", executionId).build());
        } else if (execution.getStatus() != AsyncExecution.Status.RUNNING) {
            // Execution is expected to be created as "RUNNING" before the dispatcher resumes it.
            LOGGER.error("Execution #{} can be resumed (status is {}).", execution.getStatus());
            throw new TDPException(TransformationErrorCodes.UNABLE_TO_RESUME_EXECUTION, ExceptionContext.withBuilder().put("id", executionId).build());
        }

        // Wrap callable to get the running status.
        final Callable<T> wrapper = wrapTaskWithProgressInformation(task, execution);

        ListenableFuture<T> future = delegate.submitListenable(wrapper);
        future.addCallback(new AsyncListenableFutureCallback(execution));
        futures.put(execution.getId(), future);

        LOGGER.debug("Execution {} resumed for execution.", execution.getId());
        return execution;
    }

    /**
     * @see ManagedTaskExecutor#queue(ManagedTaskCallable, String)
     */
    @Override
    public synchronized AsyncExecution queue(final ManagedTaskCallable<T> task, String groupId) {

        // Create async execution
        final AsyncExecution asyncExecution = ofNullable(groupId).map(s -> new AsyncExecution(groupId)).orElseGet(AsyncExecution::new);
        asyncExecution.setUserId(security.getUserId());
        asyncExecution.setTenantId(security.getTenantId());
        repository.save(asyncExecution);

        // Wrap callable to get the running status.
        final Callable<T> wrapper = wrapTaskWithProgressInformation(task, asyncExecution);

        ListenableFuture<T> future = delegate.submitListenable(wrapper);
        future.addCallback(new AsyncListenableFutureCallback(asyncExecution));
        futures.put(asyncExecution.getId(), future);

        LOGGER.debug("Execution {} queued for execution.", asyncExecution.getId());
        return asyncExecution;
    }

    /**
     * Wrap the given task with progress information and update execution in the repository.
     *
     * @param task the task to wrap.
     * @param asyncExecution the matching async execution monitor.
     * @return the tasks wrapped with progress information.
     */
    private Callable<T> wrapTaskWithProgressInformation(Callable<T> task, AsyncExecution asyncExecution) {
        return () -> {
            asyncExecution.updateExecutionState(AsyncExecution.Status.RUNNING);
            repository.save(asyncExecution);
            try {
                ExecutionContext.get().link(asyncExecution, Thread.currentThread(), repository);
                return task.call();
            } finally {
                ExecutionContext.get().unlink(Thread.currentThread());
            }
        };
    }

    /**
     * @see ManagedTaskRepository#get(String)
     */
    public AsyncExecution find(final String id) {
        LOGGER.debug("Request for execution #{}", id);
        return repository.get(id);
    }

    /**
     * @see ManagedTaskExecutor#cancel(String)
     */
    @Override
    public synchronized AsyncExecution cancel(final String id) {
        LOGGER.debug("Cancel execution #{}", id);
        final AsyncExecution asyncExecution = repository.get(id);
        if (asyncExecution != null) {
            if (asyncExecution.getStatus() == DONE) {
                throw new CancellationException();
            }

            try {
                ExecutionContext.get().notifySignal(asyncExecution, Signal.CANCEL);
            } catch (Exception e) {
                LOGGER.error("Unable to call cancel in execution context.", e);
            }
            try {
                final Optional<ListenableFuture<T>> futureToCancel = ofNullable(futures.get(id));
                futureToCancel.ifPresent(tListenableFuture -> tListenableFuture.cancel(true));
            } catch (CancellationException e) {
                LOGGER.debug("Cancel task {} exception.", id, e);
            } finally {
                asyncExecution.updateExecutionState(AsyncExecution.Status.CANCELLED);
                repository.save(asyncExecution);
            }
        }
        return asyncExecution;
    }

    /**
     * @see ManagedTaskExecutor#stop(String)
     */
    @Override
    public AsyncExecution stop(String id) {
        final AsyncExecution asyncTask = find(id);
        if (asyncTask != null) {
            // call the signal handler to deal with the stop signal
            ExecutionContext.get().notifySignal(asyncTask, Signal.STOP);

            // update and save the async execution
            asyncTask.updateExecutionState(DONE);
            repository.save(asyncTask);
        }
        return asyncTask;
    }

    /**
     * ListenableFutureCallback for managed tasks to update the AsyncExecution status based on the tasks progress.
     */
    private class AsyncListenableFutureCallback implements ListenableFutureCallback<T> {

        /** The async execution. */
        private final AsyncExecution asyncExecution;

        /**
         * Default constructor.
         *
         * @param asyncExecution the async execution.
         */
        AsyncListenableFutureCallback(AsyncExecution asyncExecution) {
            this.asyncExecution = asyncExecution;
        }

        /**
         * @see ListenableFutureCallback#onFailure(Throwable)
         */
        @Override
        public void onFailure(Throwable throwable) {
            if (throwable instanceof CancellationException) {
                LOGGER.info("Execution {} is cancelled.", asyncExecution.getId(), throwable);
            } else {
                LOGGER.error("Execution {} finished with error.", asyncExecution.getId(), throwable);
                try {
                    asyncExecution.setException(throwable);
                    asyncExecution.updateExecutionState(AsyncExecution.Status.FAILED);
                    asyncExecution.getTime().setEndDate(System.currentTimeMillis());

                } finally {
                    futures.remove(asyncExecution.getId());
                    repository.save(asyncExecution);
                }
            }
        }

        /**
         * @see ListenableFutureCallback#onSuccess(Object)
         */
        @Override
        public void onSuccess(T t) {
            if (t != null) {
                LOGGER.debug("Execution {} finished with success.", asyncExecution.getId());
                try {
                    asyncExecution.setResult((AsyncExecutionResult) t);
                    asyncExecution.updateExecutionState(AsyncExecution.Status.DONE);
                } finally {
                    futures.remove(asyncExecution.getId());
                    repository.save(asyncExecution);
                }
            }
        }
    }
}
