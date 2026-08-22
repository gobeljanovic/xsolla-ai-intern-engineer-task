package com.example.XsollaTask.review.job;

public interface ReviewJobEventListener {

    void onEvent(ReviewJobEvent event);

    void onComplete();
}