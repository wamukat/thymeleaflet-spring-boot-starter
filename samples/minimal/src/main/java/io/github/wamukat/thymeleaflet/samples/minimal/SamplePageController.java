package io.github.wamukat.thymeleaflet.samples.minimal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    @GetMapping("/forms")
    public String forms(Model model) {
        List<Map<String, Object>> options = new ArrayList<>();
        options.add(option("starter", "Starter"));
        options.add(option("pro", "Pro"));
        options.add(option("enterprise", "Enterprise"));

        model.addAttribute("options", options);

        model.addAttribute("formInputLabel", "Email");
        model.addAttribute("formInputValue", "");
        model.addAttribute("formInputPlaceholder", "you@example.com");
        model.addAttribute("formInputHelp", "We never share your email.");
        model.addAttribute("formInputError", "");
        model.addAttribute("formInputRequired", true);
        model.addAttribute("formInputDisabled", false);

        model.addAttribute("formSelectLabel", "Plan");
        model.addAttribute("formSelectSelected", "pro");
        model.addAttribute("formSelectHelp", "Choose the plan for this account.");
        model.addAttribute("formSelectError", "");
        model.addAttribute("formSelectRequired", true);
        model.addAttribute("formSelectDisabled", false);

        model.addAttribute("formCheckboxLabel", "Accept terms and conditions");
        model.addAttribute("formCheckboxChecked", false);
        model.addAttribute("formCheckboxHelp", "You must agree before continuing.");
        model.addAttribute("formCheckboxError", "");
        model.addAttribute("formCheckboxDisabled", false);

        return "forms";
    }

    private Map<String, Object> option(String value, String label) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("value", value);
        option.put("label", label);
        return option;
    }
}
