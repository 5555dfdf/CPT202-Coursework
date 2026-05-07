package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.entity.User;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.vo.SlotVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotInfoService {
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;
    /**
     * Retrieves a list of slots for a specific specialist, filtered by date and time constraints.
     * Each slot is enriched with information about the most recent booking and the customer's name.
     *
     * @param specialistId The unique ID of the specialist.
     * @param date         Optional: Filter by a specific date (ISO_LOCAL_DATE format: yyyy-MM-dd).
     * @param from         Optional: Filter slots starting at or after this timestamp (ISO_OFFSET_DATE_TIME).
     * @param to           Optional: Filter slots starting at or before this timestamp (ISO_OFFSET_DATE_TIME).
     * @return A list of {@link SlotVo} containing detailed slot and booking information.
     */
    public List<SlotVo> getSpecialistSlots(String specialistId, String date, String from, String to) {
        //  Initial retrieval of all slots assigned to the specialist
        List<Slot> allSlots = slotRepository.findBySpecialistId(specialistId);
        // Parse input parameters into temporal objects
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()){
            localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        OffsetDateTime fromTime;
        if (from != null && !from.isEmpty()) {
            fromTime = OffsetDateTime.parse(from);
        } else {
            fromTime = null;
        }
        OffsetDateTime toTime;
        if (to != null && !to.isEmpty()) {
            toTime = OffsetDateTime.parse(to);
        } else {
            toTime = null;
        }
        // If a specific date is provided, filter for slots within that 24-hour window (UTC)
        if (date != null) {
            OffsetDateTime startOfDay = localDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime endOfDay = startOfDay.plusDays(1);
            allSlots = allSlots.stream()
                    .filter(slot -> !slot.getStartTime().isBefore(startOfDay.toLocalDateTime()) && slot.getStartTime().isBefore(endOfDay.toLocalDateTime()))
                    .toList();
        }
        if (from != null) {
            allSlots = allSlots.stream()
                    .filter(slot -> !slot.getStartTime().isBefore(fromTime.toLocalDateTime()))
                    .toList();
        }
        if (to != null) {
            allSlots = allSlots.stream()
                    .filter(slot -> !slot.getStartTime().isAfter(toTime.toLocalDateTime()))
                    .toList();
        }
        return allSlots.stream()
                .map(slot -> {
                    Booking booking = bookingRepository.findTopBySlotIdOrderByCreatedAtDesc(slot.getId()).orElse(null);
                    String customerName = null;
                    if (booking != null) {
                        User customer = userRepository.findById(booking.getCustomerId());
                        customerName = customer != null ? customer.getName() : null;
                    }
                    return SlotVo.fromSlot(slot, booking, customerName);
                })
                .toList();
    }

}
