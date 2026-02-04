package io.github.wamukat.thymeleaflet.samples.minimal;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SamplePageController {

    @GetMapping("/")
    public String index(Model model) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", "Alex Morgan");
        profile.put("role", "Product Designer");
        profile.put("plan", "Pro");
        profile.put("region", "APAC");
        profile.put("points", 1280);
        profile.put("projects", 8);

        model.addAttribute("profile", profile);
        model.addAttribute("primaryCardTitle", "Welcome to Thymeleaflet");
        model.addAttribute("primaryCardDescription", "This page assembles fragments into a real UI.");
        model.addAttribute("infoCardTitle", "Preview in Thymeleaflet");
        model.addAttribute("infoCardDescription", "Open /thymeleaflet to see each fragment in isolation.");
        model.addAttribute("ctaLabel", "Get started");

        return "index";
    }
}
