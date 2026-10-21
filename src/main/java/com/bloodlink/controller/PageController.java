package com.bloodlink.controller;

import com.bloodlink.controller.form.*;
import com.bloodlink.model.*;
import com.bloodlink.model.ResponseStatus;
import com.bloodlink.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {
    private final BloodLinkService service;
    public PageController(BloodLinkService service) { this.service = service; }

    @GetMapping({"/", "/login"})
    public String login(Principal principal) { return principal == null ? "login" : "redirect:/dashboard"; }

    @GetMapping("/register")
    public String registration(Model model) { model.addAttribute("form", new RegistrationForm()); return "register"; }
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult errors) {
        if (errors.hasErrors()) return "register";
        try { service.register(form); }
        catch (BusinessException ex) { errors.reject("registration", ex.getMessage()); return "register"; }
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        String username = principal.getName();
        if (service.user(username).getRole() == Role.HOSPITAL) {
            List<BloodRequest> requests = service.hospitalRequests(username);
            model.addAttribute("requests", requests.stream().limit(6).toList());
            model.addAttribute("activeCount", requests.stream().filter(BloodRequest::isAccepting).count());
            model.addAttribute("interestedCount", requests.stream().mapToLong(r -> service.requestResponses(username, r.getId()).stream().filter(d -> d.getStatus() == ResponseStatus.INTERESTED).count()).sum());
            model.addAttribute("donationCount", service.hospitalDonations(username).size());
            model.addAttribute("fulfilledCount", requests.stream().filter(r -> r.getStatus() == RequestStatus.FULFILLED).count());
            model.addAttribute("fulfilledRequests", requests.stream().filter(r -> r.getStatus() == RequestStatus.FULFILLED).limit(3).toList());
        } else {
            List<BloodRequest> relevant = service.relevantRequests(service.donor(username));
            model.addAttribute("requests", relevant.stream().limit(6).toList());
            model.addAttribute("relevantCount", relevant.size());
            model.addAttribute("donationCount", service.donorDonations(username).size());
            model.addAttribute("points", service.points(username));
        }
        return "dashboard";
    }
    @GetMapping("/requests")
    public String requests(Principal principal, @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(defaultValue = "") String city, @RequestParam(required = false) Urgency urgency,
            @RequestParam(defaultValue = "ACTIVE") String status, Model model) {
        boolean hospital = service.user(principal.getName()).getRole() == Role.HOSPITAL;
        List<BloodRequest> items = hospital ? service.hospitalRequests(principal.getName()) : service.allRequests();
        items = items.stream().filter(r -> bloodGroup == null || r.getBloodGroup() == bloodGroup)
            .filter(r -> city.isBlank() || r.getHospital().getCity().toLowerCase(Locale.ROOT).contains(city.trim().toLowerCase(Locale.ROOT)))
            .filter(r -> urgency == null || r.getUrgency() == urgency)
            .filter(r -> status.equals("ALL") || (status.equals("ACTIVE") ? r.isAccepting() : r.getDisplayStatus().name().equals(status))).toList();
        model.addAttribute("requests", items); model.addAttribute("selectedGroup", bloodGroup);
        model.addAttribute("cityFilter", city); model.addAttribute("selectedUrgency", urgency); model.addAttribute("selectedStatus", status);
        return "requests";
    }
    @GetMapping("/requests/{id}")
    public String details(@PathVariable Long id, Principal principal, Model model) {
        BloodRequest request = service.request(id); model.addAttribute("bloodRequest", request);
        if (service.user(principal.getName()).getRole() == Role.HOSPITAL) {
            service.requireOwner(request, principal.getName());
            model.addAttribute("responses", service.requestResponses(principal.getName(), id));
        } else {
            model.addAttribute("myResponse", service.myResponse(principal.getName(), id).orElse(null));
            model.addAttribute("distance", service.distanceKm(service.donor(principal.getName()), request.getHospital()));
        }
        return "request-detail";
    }
    @GetMapping("/hospital/requests/new")
    public String newRequest(Model model) {
        RequestForm form = new RequestForm(); form.setUnitsRequested(1); form.setUrgency(Urgency.NORMAL);
        form.setRequiredBy(BloodLinkService.now().plusDays(1).withSecond(0).withNano(0));
        model.addAttribute("form", form); return "request-form";
    }
    @PostMapping("/hospital/requests")
    public String publish(@Valid @ModelAttribute("form") RequestForm form, BindingResult errors, Principal principal, RedirectAttributes flash) {
        if (errors.hasErrors()) return "request-form";
        try {
            BloodRequest request = service.publish(principal.getName(), form);
            flash.addFlashAttribute("success", "Request published. Matching donors now have an in-app alert.");
            return "redirect:/requests/" + request.getId();
        } catch (BusinessException ex) { errors.reject("request", ex.getMessage()); return "request-form"; }
    }
    @PostMapping("/hospital/requests/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        service.cancel(principal.getName(), id); flash.addFlashAttribute("success", "Request cancelled."); return "redirect:/requests/" + id;
    }
    @PostMapping("/hospital/requests/{id}/confirm/{responseId}")
    public String confirm(@PathVariable Long id, @PathVariable Long responseId, @RequestParam int units, Principal principal, RedirectAttributes flash) {
        service.confirm(principal.getName(), id, responseId, units);
        flash.addFlashAttribute("success", "Donation confirmed. Progress updated and 100 points awarded to the donor."); return "redirect:/requests/" + id;
    }
    @PostMapping("/donor/requests/{id}/respond")
    public String respond(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        service.respond(principal.getName(), id); flash.addFlashAttribute("success", "The hospital can now see your availability and contact details. Screening takes place at the hospital.");
        return "redirect:/requests/" + id;
    }
    @PostMapping("/donor/requests/{id}/withdraw")
    public String withdraw(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        service.withdraw(principal.getName(), id); flash.addFlashAttribute("success", "Your interest has been withdrawn."); return "redirect:/requests/" + id;
    }
    @GetMapping("/donor/responses")
    public String responses(Principal principal, Model model) { model.addAttribute("responses", service.myResponses(principal.getName())); return "responses"; }
    @GetMapping("/donor/rewards")
    public String rewards(Principal principal, Model model) {
        model.addAttribute("donations", service.donorDonations(principal.getName())); model.addAttribute("points", service.points(principal.getName())); return "rewards";
    }
    @GetMapping("/donor/notifications")
    public String alerts(Principal principal, Model model) { model.addAttribute("alerts", service.alerts(principal.getName())); return "notifications"; }
    @PostMapping("/donor/notifications/{id}/read")
    public String read(@PathVariable Long id, Principal principal) { return "redirect:/requests/" + service.readAlert(principal.getName(), id); }
    @PostMapping("/donor/notifications/read-all")
    public String readAll(Principal principal) { service.readAll(principal.getName()); return "redirect:/donor/notifications"; }
    @GetMapping("/api/notifications/count") @ResponseBody
    public Map<String, Long> unread(Principal principal) { return Map.of("unread", service.unread(principal.getName())); }

    @GetMapping("/donor/profile")
    public String donorProfile(Principal principal, Model model) {
        DonorForm form = new DonorForm(); BeanUtils.copyProperties(service.donor(principal.getName()), form);
        model.addAttribute("form", form); return "donor-profile";
    }
    @PostMapping("/donor/profile")
    public String donorProfile(@Valid @ModelAttribute("form") DonorForm form, BindingResult errors, Principal principal, RedirectAttributes flash) {
        if (errors.hasErrors()) return "donor-profile";
        service.updateDonor(principal.getName(), form); flash.addFlashAttribute("success", "Your profile and alert preferences have been saved."); return "redirect:/donor/profile";
    }
    @GetMapping("/hospital/profile")
    public String hospitalProfile(Principal principal, Model model) {
        HospitalForm form = new HospitalForm(); BeanUtils.copyProperties(service.hospital(principal.getName()), form);
        model.addAttribute("form", form); return "hospital-profile";
    }
    @PostMapping("/hospital/profile")
    public String hospitalProfile(@Valid @ModelAttribute("form") HospitalForm form, BindingResult errors, Principal principal, RedirectAttributes flash) {
        if (errors.hasErrors()) return "hospital-profile";
        service.updateHospital(principal.getName(), form); flash.addFlashAttribute("success", "Hospital details updated."); return "redirect:/hospital/profile";
    }
}
