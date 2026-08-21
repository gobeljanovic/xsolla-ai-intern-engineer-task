package com.example.XsollaTask.review.provider;

import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Finding;

import java.util.List;

//made interface for two providers, llm and mock
public interface ReviewProvider {

    List<Finding> review(ParsedDiff diff);
}