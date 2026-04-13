package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotInfoService {
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;

    public List<SlotVo> getSpecialistSlots(String specialistId, String date, String from, String to) {
        List<Slot> allSlots = slotRepository.findBySpecialistId(specialistId);
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

        List<String> slotIds = allSlots.stream().map(Slot::getId).toList();
        List<Booking> bookings = bookingRepository.findBySlotIdIn(slotIds);

        Map<String, Booking> slotToBookingMap = bookings.stream()
                .collect(Collectors.toMap(Booking::getSlotId, b -> b, (b1, b2) -> b1));

        return allSlots.stream()
                .map(slot -> {
                    SlotVo vo = SlotVo.fromSlot(slot);
                    Booking booking = slotToBookingMap.get(slot.getId());
                    if (booking != null) {
                        vo.setBookingId(booking.getId());
                        vo.setStatus(booking.getStatus().name());
                        try {
                            User customer = userRepository.findById(booking.getCustomerId());
                            if (customer != null) {
                                vo.setCustomerName(customer.getName());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch customer info", e);
                        }
                    }
                    return vo;
                })
                .toList();
    }

}

