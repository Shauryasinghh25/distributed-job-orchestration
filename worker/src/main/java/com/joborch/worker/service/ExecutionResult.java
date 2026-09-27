package com.joborch.worker.service;

public sealed interface ExecutionResult
        permits ExecutionResult.Success, ExecutionResult.Retry, ExecutionResult.DeadLetter {

    record Success()                    implements ExecutionResult {}
    record Retry(long delayMs)          implements ExecutionResult {}
    record DeadLetter(String reason)    implements ExecutionResult {}

    static ExecutionResult success()                   { return new Success(); }
    static ExecutionResult retry(long delayMs)         { return new Retry(delayMs); }
    static ExecutionResult deadLetter(String reason)   { return new DeadLetter(reason); }
}
