package com.stockking.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Sends the site root to the read-only paper dashboard. */
@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "redirect:/paper";
    }
}
