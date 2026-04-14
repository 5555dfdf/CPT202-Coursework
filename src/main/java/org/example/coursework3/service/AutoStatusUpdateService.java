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

@Slf4j
@Service
public class AutoStatusUpdateService {

    private static final long PENDING_EXPIRY_HOURS = 24;
    private static final long CANCEL_BEFORE_START_MINUTES = 30;
    private static final long COMPLETE_AFTER_END_HOURS = 2;

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai"); // 或 Asia/Shanghai

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCancelPendingBookings() {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        LocalDateTime threshold = now.minusHours(PENDING_EXPIRY_HOURS);

        List<Booking> allPending = bookingRepository
                .findByStatusAndCreatedAtBefore(BookingStatus.Pending, threshold);

        List<Booking> toCancel = new ArrayList<>();

        for (Booking booking : allPending) {
            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot == null) {
                log.warn("Slot not found for booking id={}", booking.getId());
                continue;
            }

            if (slot.getStartTime().isBefore(now.plusMinutes(CANCEL_BEFORE_START_MINUTES))) {
                booking.setStatus(BookingStatus.Cancelled);
                toCancel.add(booking);
                log.info("Auto-cancelled booking id={}", booking.getId());
            }
        }

        if (!toCancel.isEmpty()) {
            bookingRepository.saveAll(toCancel);
            log.info("Total auto-cancelled bookings: {}", toCancel.size());
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCompleteConfirmedBookings() {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        List<Booking> allConfirmed = bookingRepository.findByStatus(BookingStatus.Confirmed);
        List<Booking> toComplete = new ArrayList<>();

        for (Booking booking : allConfirmed) {
            Slot slot = slotRepository.findById(booking.getSlotId()).orElse(null);
            if (slot == null) {
                log.warn("Slot not found for booking id={}", booking.getId());
                continue;
            }

            if (now.isAfter(slot.getEndTime().plusHours(COMPLETE_AFTER_END_HOURS))) {
                booking.setStatus(BookingStatus.Completed);
                toComplete.add(booking);
                log.info("Auto-completed booking id={}", booking.getId());
            }
        }

        if (!toComplete.isEmpty()) {
            bookingRepository.saveAll(toComplete);
            log.info("Total auto-completed bookings: {}", toComplete.size());
        }
    }
}