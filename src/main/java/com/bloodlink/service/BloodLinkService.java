package com.bloodlink.service;

import com.bloodlink.controller.form.*;
import com.bloodlink.model.*;
import com.bloodlink.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** One transaction per business action keeps progress, donations and rewards consistent. */
@Service
@Transactional
public class BloodLinkService {
    private final AppUserRepository users;
    private final DonorProfileRepository donors;
    private final HospitalProfileRepository hospitals;
    private final BloodRequestRepository requests;
    private final DonorResponseRepository responses;
    private final NotificationRepository notifications;
    private final DonationRepository donations;
    private final RewardTransactionRepository rewards;
    private final PasswordEncoder passwords;

    public BloodLinkService(AppUserRepository users, DonorProfileRepository donors,
            HospitalProfileRepository hospitals, BloodRequestRepository requests,
            DonorResponseRepository responses, NotificationRepository notifications,
            DonationRepository donations, RewardTransactionRepository rewards, PasswordEncoder passwords) {
        this.users = users; this.donors = donors; this.hospitals = hospitals;
        this.requests = requests; this.responses = responses; this.notifications = notifications;
        this.donations = donations; this.rewards = rewards; this.passwords = passwords;
    }

    public static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }

    public AppUser user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new AccessDeniedException("Account not found."));
    }
    public DonorProfile donor(String username) {
        return donors.findByUserUsername(username).orElseThrow(() -> new AccessDeniedException("Donor access required."));
    }
    public HospitalProfile hospital(String username) {
        return hospitals.findByUserUsername(username).orElseThrow(() -> new AccessDeniedException("Hospital access required."));
    }
    public BloodRequest request(Long id) {
        return requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private BloodRequest lockedRequest(Long id) {
        return requests.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public void requireOwner(BloodRequest request, String username) {
        if (!request.getHospital().getUser().getUsername().equals(username))
            throw new AccessDeniedException("This request belongs to another hospital.");
    }
    private void requireOpen(BloodRequest request) {
        if (!request.isAccepting()) throw new BusinessException("This request is closed or its deadline has passed.");
    }

    public DonorProfile register(RegistrationForm form) {
        String username = form.getUsername().trim().toLowerCase(Locale.ROOT);
        if (users.findByUsername(username).isPresent()) throw new BusinessException("That username is already taken.");
        AppUser user = new AppUser();
        user.setUsername(username); user.setPasswordHash(passwords.encode(form.getPassword())); user.setRole(Role.DONOR);
        users.save(user);
        DonorProfile donor = new DonorProfile(); donor.setUser(user);
        applyDonorForm(donor, form);
        return donors.save(donor);
    }

    public void updateDonor(String username, DonorForm form) { applyDonorForm(donor(username), form); }
    private void applyDonorForm(DonorProfile donor, DonorForm form) {
        donor.setFullName(form.getFullName().trim()); donor.setDateOfBirth(form.getDateOfBirth());
        donor.setBloodGroup(form.getBloodGroup()); donor.setPhone(form.getPhone().trim());
        donor.setEmail(form.getEmail() == null ? "" : form.getEmail().trim()); donor.setCity(form.getCity().trim());
        donor.setLatitude(form.getLatitude()); donor.setLongitude(form.getLongitude());
        // A profile edit cannot erase the latest hospital-confirmed donation date.
        LocalDate latestConfirmed = donations.findByResponseDonorIdOrderByConfirmedAtDesc(donor.getId() == null ? -1L : donor.getId())
            .stream().map(d -> d.getConfirmedAt().toLocalDate()).max(LocalDate::compareTo).orElse(null);
        LocalDate supplied = form.getLastDonationDate();
        donor.setLastDonationDate(latestConfirmed != null && (supplied == null || supplied.isBefore(latestConfirmed)) ? latestConfirmed : supplied);
        donor.setNotificationsEnabled(form.isNotificationsEnabled());
    }
    public void updateHospital(String username, HospitalForm form) {
        HospitalProfile hospital = hospital(username);
        hospital.setName(form.getName().trim()); hospital.setAddress(form.getAddress().trim());
        hospital.setCity(form.getCity().trim()); hospital.setPhone(form.getPhone().trim());
        hospital.setLatitude(form.getLatitude()); hospital.setLongitude(form.getLongitude());
    }

    public BloodRequest publish(String username, RequestForm form) {
        if (form.getUnitsRequested() == null || form.getUnitsRequested() < 1 || form.getUnitsRequested() > 100)
            throw new BusinessException("Request between 1 and 100 donation units.");
        if (form.getRequiredBy() == null || !form.getRequiredBy().isAfter(now()))
            throw new BusinessException("Choose a future deadline in Indian Standard Time.");
        BloodRequest request = new BloodRequest(); request.setHospital(hospital(username));
        request.setBloodGroup(form.getBloodGroup()); request.setUnitsRequested(form.getUnitsRequested());
        request.setUrgency(form.getUrgency()); request.setRequiredBy(form.getRequiredBy());
        request.setDescription(form.getDescription()); request.setCreatedAt(now());
        requests.save(request);
        for (DonorProfile donor : donors.findAll()) {
            if (donor.isNotificationsEnabled() && donor.getBloodGroup() == request.getBloodGroup()
                    && donor.getCity().equalsIgnoreCase(request.getHospital().getCity())) {
                Notification alert = new Notification(); alert.setDonor(donor); alert.setRequest(request);
                alert.setCreatedAt(now()); notifications.save(alert);
            }
        }
        return request;
    }

    public DonorResponse respond(String username, Long requestId) {
        BloodRequest request = lockedRequest(requestId); requireOpen(request);
        DonorProfile donor = donor(username);
        DonorResponse response = responses.findByRequestIdAndDonorId(requestId, donor.getId()).orElseGet(DonorResponse::new);
        if (response.getStatus() == ResponseStatus.CONFIRMED) throw new BusinessException("Your donation is already confirmed.");
        if (response.getStatus() == ResponseStatus.INTERESTED) throw new BusinessException("You have already expressed interest.");
        response.setRequest(request); response.setDonor(donor); response.setStatus(ResponseStatus.INTERESTED); response.setRespondedAt(now());
        request.setStatus(RequestStatus.IN_PROGRESS);
        return responses.save(response);
    }
    public void withdraw(String username, Long requestId) {
        BloodRequest request = lockedRequest(requestId);
        DonorResponse response = responses.findByRequestIdAndDonorId(requestId, donor(username).getId())
            .orElseThrow(() -> new BusinessException("No response found."));
        if (response.getStatus() != ResponseStatus.INTERESTED) throw new BusinessException("Only an unconfirmed response can be withdrawn.");
        response.setStatus(ResponseStatus.WITHDRAWN);
        // Preserve closed states; otherwise return a request with no active responses to Open.
        if (request.isAccepting() && request.getConfirmedUnits() == 0
                && responses.findByRequestIdOrderByRespondedAtAsc(requestId).stream().noneMatch(r -> r.getStatus() == ResponseStatus.INTERESTED))
            request.setStatus(RequestStatus.OPEN);
    }
    public Donation confirm(String username, Long requestId, Long responseId, int units) {
        BloodRequest request = lockedRequest(requestId); requireOwner(request, username); requireOpen(request);
        DonorResponse response = responses.findById(responseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!response.getRequest().getId().equals(requestId)) throw new AccessDeniedException("Response does not belong to this request.");
        if (response.getStatus() != ResponseStatus.INTERESTED) throw new BusinessException("This response is withdrawn or already confirmed.");
        if (units < 1 || units > request.getRemainingUnits()) throw new BusinessException("Confirmed units must fit within the remaining target.");
        Donation donation = new Donation(); donation.setResponse(response); donation.setUnits(units); donation.setConfirmedAt(now());
        donations.save(donation);
        response.setStatus(ResponseStatus.CONFIRMED);
        request.setConfirmedUnits(request.getConfirmedUnits() + units);
        request.setStatus(request.getRemainingUnits() == 0 ? RequestStatus.FULFILLED : RequestStatus.IN_PROGRESS);
        response.getDonor().setLastDonationDate(now().toLocalDate());
        RewardTransaction reward = new RewardTransaction(); reward.setDonation(donation); reward.setPoints(100); rewards.save(reward);
        return donation;
    }
    public void cancel(String username, Long requestId) {
        BloodRequest request = lockedRequest(requestId); requireOwner(request, username); requireOpen(request);
        request.setStatus(RequestStatus.CANCELLED);
    }

    @Scheduled(fixedDelay = 60000)
    public void expireRequests() {
        // Use the same request lock as respond/confirm/cancel to avoid overwriting a concurrent fulfillment.
        for (Long id : requests.expiredCandidateIds(now())) {
            BloodRequest request = lockedRequest(id);
            if (request.getDisplayStatus() == RequestStatus.EXPIRED) request.setStatus(RequestStatus.EXPIRED);
        }
    }
    public List<BloodRequest> allRequests() { return requests.findAllByOrderByCreatedAtDesc(); }
    public List<BloodRequest> hospitalRequests(String username) {
        Long hospitalId = hospital(username).getId();
        return allRequests().stream().filter(r -> r.getHospital().getId().equals(hospitalId)).toList();
    }
    public List<BloodRequest> relevantRequests(DonorProfile donor) {
        return allRequests().stream().filter(BloodRequest::isAccepting)
            .filter(r -> r.getBloodGroup() == donor.getBloodGroup() && r.getHospital().getCity().equalsIgnoreCase(donor.getCity())).toList();
    }
    public List<DonorResponse> requestResponses(String username, Long requestId) {
        requireOwner(request(requestId), username);
        return responses.findByRequestIdOrderByRespondedAtAsc(requestId);
    }
    public Optional<DonorResponse> myResponse(String username, Long requestId) {
        return responses.findByRequestIdAndDonorId(requestId, donor(username).getId());
    }
    public List<DonorResponse> myResponses(String username) { return responses.findByDonorIdOrderByRespondedAtDesc(donor(username).getId()); }
    public List<Donation> donorDonations(String username) { return donations.findByResponseDonorIdOrderByConfirmedAtDesc(donor(username).getId()); }
    public List<Donation> hospitalDonations(String username) { return donations.findByResponseRequestHospitalIdOrderByConfirmedAtDesc(hospital(username).getId()); }
    public long points(String username) { return rewards.pointsFor(donor(username).getId()); }
    public List<Notification> alerts(String username) { return notifications.findByDonorIdOrderByCreatedAtDesc(donor(username).getId()); }
    public long unread(String username) { return notifications.countByDonorIdAndSeenFalse(donor(username).getId()); }
    public Long readAlert(String username, Long id) {
        Notification alert = notifications.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!alert.getDonor().getUser().getUsername().equals(username)) throw new AccessDeniedException("This alert belongs to another donor.");
        alert.setSeen(true); return alert.getRequest().getId();
    }
    public void readAll(String username) { alerts(username).forEach(alert -> alert.setSeen(true)); }

    public Double distanceKm(DonorProfile donor, HospitalProfile hospital) {
        if (donor.getLatitude() == null || donor.getLongitude() == null) return null;
        double lat1 = Math.toRadians(donor.getLatitude()), lat2 = Math.toRadians(hospital.getLatitude());
        double dLat = lat2 - lat1, dLon = Math.toRadians(hospital.getLongitude() - donor.getLongitude());
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        return Math.round(6371 * 2 * Math.asin(Math.sqrt(Math.min(1, a))) * 10.0) / 10.0;
    }
}
