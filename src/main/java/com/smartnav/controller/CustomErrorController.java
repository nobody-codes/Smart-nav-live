package com.smartnav.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = 500;
        String errorMessage = "An unexpected error occurred.";

        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorMessage = "The requested navigation route or page was not found.";
                model.addAttribute("statusCode", 404);
                model.addAttribute("errorTitle", "Page Not Found");
                model.addAttribute("errorMessage", errorMessage);
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorMessage = "You do not have administrative privileges to access this resource.";
                model.addAttribute("statusCode", 403);
                model.addAttribute("errorTitle", "Access Forbidden");
                model.addAttribute("errorMessage", errorMessage);
                return "error/403";
            }
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", "System Encountered an Error");
        model.addAttribute("errorMessage", errorMessage);
        return "error/500";
    }
}
