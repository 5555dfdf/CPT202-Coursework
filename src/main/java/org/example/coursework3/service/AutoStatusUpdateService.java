package org.example.coursework3.service;

import lombok.extern.slf4j.Slf4j;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**Service for automatically updating booking statuses based on time rules including auto-cancel
and auto-complete
 **/
@Slf4j
@Service
public class AutoStatusUpdateService {

    private static final long CANCEL_BEFORE_START_MINUTES = 10;
    private static final long COMPLETE_AFTER_END_HOURS = 2;
    //Set standard timezone
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SlotRepository slotRepository;

    // Check every 5 minutes, auto-cancel 10 minutes before the event
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelUnconfirmedBeforeStart() {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        // Get all bookings currently in 'Pending' status
        List<Booking> pending = bookingRepository
                .findByStatus(BookingStatus.Pending);

        for (Booking booking : pending) {
            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot == null) continue;
            //Cancel booking that current time is less than 10 minutes before start time
            if (slot.getStartTime().isBefore(now.plusMinutes(CANCEL_BEFORE_START_MINUTES))) {
                booking.setStatus(BookingStatus.Cancelled);
                bookingRepository.save(booking);
                log.info("Auto-cancelled booking id={}", booking.getId());
            }
        }
    }
    // Check every 5 minutes, auto-complete event after 2 hours
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCompleteConfirmedBookings() {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        // Get all bookings currently in 'Confirmed' status
        List<Booking> allConfirmed = bookingRepository.findByStatus(BookingStatus.Confirmed);
        // A list to store the data to be changed
        List<Booking> toComplete = new ArrayList<>();

        for (Booking booking : allConfirmed) {
            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot == null) {
                log.warn("Slot not found for booking id={}", booking.getId());
                continue;
            }
            // If the current time is more than 2 hours past the slot's end time, change to completed
            if (now.isAfter(slot.getEndTime().plusHours(COMPLETE_AFTER_END_HOURS))) {
                booking.setStatus(BookingStatus.Completed);
                toComplete.add(booking);
                log.info("Auto-completed booking id={}", booking.getId());
            }
        }
        // save all changes
        if (!toComplete.isEmpty()) {
            bookingRepository.saveAll(toComplete);
            log.info("Total auto-completed bookings: {}", toComplete.size());
        }
    }
}