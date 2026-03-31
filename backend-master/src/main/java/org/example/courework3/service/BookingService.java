package org.example.courework3.service;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Booking;
import org.example.courework3.entity.BookingHistory;
import org.example.courework3.entity.Slot;
import org.example.courework3.entity.Specialist;
import org.example.courework3.entity.User;
import org.example.courework3.repository.BookingHistoryRepository;
import org.example.courework3.repository.BookingRepository;
import org.example.courework3.repository.SlotRepository;
import org.example.courework3.repository.SpecialistRepository;
import org.example.courework3.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SlotRepository slotRepository;
    private final SpecialistRepository specialistRepository;
    private final UserRepository userRepository;

    public List<Booking> listBookings(Map<String, Object> params) {
        List<Booking> bookings = bookingRepository.findAll();
        return bookings;
    }

    public Booking getBooking(String id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
    }

    public Booking getBookingOrNull(String id) {
        return bookingRepository.findById(id).orElse(null);
    }

    @Transactional
    public Booking createBooking(Map<String, Object> payload, String customerId) {
        String specialistId = (String) payload.get("specialistId");
        String slotId = (String) payload.get("slotId");
        String note = payload.containsKey("note") ? (String) payload.get("note") : null;

        if (specialistId == null || specialistId.isBlank()) {
            throw new RuntimeException("Specialist ID cannot be empty");
        }
        if (slotId == null || slotId.isBlank()) {
            throw new RuntimeException("Slot ID cannot be empty");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new RuntimeException("Customer ID cannot be empty");
        }
        if (!specialistRepository.existsById(specialistId)) {
            throw new RuntimeException("Specialist not found: " + specialistId);
        }
        if (!slotRepository.existsById(slotId)) {
            throw new RuntimeException("Slot not found: " + slotId);
        }
        if (!userRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found: " + customerId);
        }

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        if (!Boolean.TRUE.equals(slot.getAvailable())) {
            throw new RuntimeException("Slot is not available");
        }
        if (!slot.getSpecialistId().equals(specialistId)) {
            throw new RuntimeException("Slot does not belong to the specified specialist");
        }

        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setSpecialistId(specialistId);
        booking.setSlotId(slotId);
        booking.setNote(note);
        booking.setStatus("Pending");

        Booking saved = bookingRepository.save(booking);

        slot.setAvailable(false);
        slotRepository.save(slot);

        BookingHistory history = new BookingHistory();
        history.setBookingId(saved.getId());
        history.setStatus("Pending");
        bookingHistoryRepository.save(history);

        return saved;
    }

    @Transactional
    public Booking updateBookingStatus(String id, String status, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));

        String oldStatus = booking.getStatus();
        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);

        BookingHistory history = new BookingHistory();
        history.setBookingId(id);
        history.setStatus(status);
        history.setReason(reason);
        bookingHistoryRepository.save(history);

        if ("Cancelled".equals(status) || "Rejected".equals(status)) {
            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot != null) {
                slot.setAvailable(true);
                slotRepository.save(slot);
            }
        }

        return saved;
    }

    public List<BookingHistory> getBookingHistory(String bookingId) {
        return bookingHistoryRepository.findByBookingIdOrderByChangedAtDesc(bookingId);
    }
}
