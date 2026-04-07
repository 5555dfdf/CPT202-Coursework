package org.example.coursework3.service;

import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AutoStatusUpdateService {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoCancelPendingBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Booking> allPending = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.Pending, threshold);
        List<Booking> toCancel = new ArrayList<>();
        for (Booking booking : allPending) {
            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot == null) continue;
            if (slot.getStartTime().isBefore(OffsetDateTime.now().plusMinutes(30))) {
                toCancel.add(booking);
            }
        }

        for (Booking booking : toCancel) {
            booking.setStatus(BookingStatus.Cancelled);
            bookingRepository.save(booking);
            System.out.println("Auto-cancelled booking: " + booking.getId());
        }

        if (!toCancel.isEmpty()) {
            System.out.println("Total auto-cancelled bookings: " + toCancel.size());
        }
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoCompleteConfirmedBookings() {
        LocalDateTime threshold = LocalDateTime.now();
        List<Booking> allConfirmed = bookingRepository.findByStatusAndUpdatedAtBefore(BookingStatus.Confirmed,threshold);
        List<Booking> toComplete = new ArrayList<>();

        OffsetDateTime now = OffsetDateTime.now();

        for (Booking booking : allConfirmed) {

            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot == null) continue;

            if (now.isAfter(slot.getEndTime().plusHours(2))) {
                toComplete.add(booking);
            }
        }


        for (Booking booking : toComplete) {
            booking.setStatus(BookingStatus.Completed);
            bookingRepository.save(booking);
            System.out.println("Auto-completed booking: " + booking.getId());
        }

        if (!toComplete.isEmpty()) {
            System.out.println("Total auto-completed bookings: " + toComplete.size());
        }
    }
}



