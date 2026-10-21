package com.bloodlink.controller;

import com.bloodlink.model.*;
import com.bloodlink.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class PageAdvice {
    private final BloodLinkService service;
    @Value("${app.map.tile-url}") private String tileUrl;
    @Value("${app.map.attribution}") private String attribution;
    @Value("${app.map.attribution-url}") private String attributionUrl;
    public PageAdvice(BloodLinkService service) { this.service = service; }

    @ModelAttribute
    public void common(Model model, Principal principal, HttpServletRequest request) {
        model.addAttribute("groups", BloodGroup.values()); model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("urgencies", Urgency.values()); model.addAttribute("path", request.getRequestURI());
        model.addAttribute("tileUrl", tileUrl); model.addAttribute("mapAttribution", attribution); model.addAttribute("mapAttributionUrl", attributionUrl);
        if (principal == null) return;
        AppUser user = service.user(principal.getName()); model.addAttribute("currentUser", user);
        boolean isHospital = user.getRole() == Role.HOSPITAL; model.addAttribute("isHospital", isHospital);
        if (isHospital) {
            HospitalProfile hospital = service.hospital(principal.getName());
            model.addAttribute("profile", hospital); model.addAttribute("displayName", hospital.getName());
        } else {
            DonorProfile donor = service.donor(principal.getName()); model.addAttribute("profile", donor);
            model.addAttribute("displayName", donor.getFullName()); model.addAttribute("unreadCount", service.unread(principal.getName()));
            Map<Long, Double> distances = new HashMap<>();
            if (donor.getLatitude() != null) for (BloodRequest item : service.allRequests()) distances.put(item.getId(), service.distanceKm(donor, item.getHospital()));
            model.addAttribute("distances", distances);
        }
    }
    @ExceptionHandler(BusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(BusinessException ex, Model model) { model.addAttribute("errorTitle", "That action could not be completed"); model.addAttribute("errorMessage", ex.getMessage()); return "error"; }
    @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(Model model) { model.addAttribute("errorTitle", "Access restricted"); model.addAttribute("errorMessage", "You do not have permission to access or change this information."); return "error"; }
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public String concurrentUpdate(Model model) { model.addAttribute("errorTitle", "These details just changed"); model.addAttribute("errorMessage", "Another action updated this record. Reload the page and try again. Your action was not partially saved."); return "error"; }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public String duplicate(Model model) { model.addAttribute("errorTitle", "This record already exists"); model.addAttribute("errorMessage", "This action conflicts with an existing record. Reload the page and check the current status or choose another username."); return "error"; }
}
