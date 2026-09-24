package com.example.urlshortener.service;

import com.example.urlshortener.repository.LinkRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Records clicks off the redirect's hot path.
 * Equivalent of Python's BackgroundTasks: background.add_task(store.record_click, ...).
 */
@Service
public class ClickRecorder {

    private final LinkRepository repository;

    public ClickRecorder(LinkRepository repository) {
        this.repository = repository;
    }

    @Async
    public void record(String code, String referrer) {
        repository.recordClick(code, referrer, System.currentTimeMillis() / 1000.0);
    }
}
