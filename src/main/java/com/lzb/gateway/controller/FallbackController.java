package com.lzb.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by lijin on 2019/11/4.
 */
@RestController
@RequestMapping("/v1/test")
@Slf4j
public class FallbackController {

    @GetMapping("/fallback")
    public String fallback() {
        log.info("fallback");
        return "fallback";
    }
}
