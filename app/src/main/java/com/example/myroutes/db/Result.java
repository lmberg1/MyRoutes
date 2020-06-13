package com.example.myroutes.db;

public abstract class Result<T> {

    private Result() {
    }

    public final static class Success<T> extends Result<T> {
        private T result;

        public Success(T t) {
            this.result = t;
        }

        public T getResult() {
            return result;
        }
    }

    public final static class Error<T> extends Result<T> {
        private SharedViewModel.Status error;

        public Error(SharedViewModel.Status error) {
            this.error = error;
        }

        public SharedViewModel.Status getError() {
            return error;
        }
    }
}