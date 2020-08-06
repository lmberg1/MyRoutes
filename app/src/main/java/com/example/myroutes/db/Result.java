package com.example.myroutes.db;

import com.example.myroutes.SharedViewModel.Status;

public class Result<T> {
    public T data;
    public Status status;

    public Result(T data, Status status) {
        this.data = data;
        this.status = status;
    }
}