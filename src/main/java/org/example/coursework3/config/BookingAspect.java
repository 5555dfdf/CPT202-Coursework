package org.example.coursework3.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.service.SpecialistBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Aspect
@Slf4j
@Configuration
public class BookingAspect {

    @Autowired
    private SpecialistBookingService bookingService;

    // 获取刚刚保存成功的 Booking 对象 / list<Booking> auto-completed, rejected
    @AfterReturning(
            pointcut = "execution(* org.example.coursework3.repository.BookingRepository.save*(..))",
            returning = "savedBooking"  // 拿到保存后的对象
    )
    public void afterBookingSave(Object savedBooking) {
        try {
            if (savedBooking instanceof Booking booking) {
                bookingService.createBookingHistory(booking);
            }
            else if (savedBooking instanceof Iterable<?>) {
                for (Object obj : (Iterable<?>) savedBooking) {
                    if (obj instanceof Booking booking) {
                        bookingService.createBookingHistory(booking);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("状态历史记录失败: {}", e.getMessage());
        }
    }
}