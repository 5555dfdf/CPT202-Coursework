package org.example.coursework3.service;

import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class AutoSlotUpdateService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long CANCEL_BEFORE_START_MINUTES = 10;

    @Autowired
    private SlotRepository slotRepository;


    /**
     * Scheduled task that runs every 5 minutes (300,000 milliseconds).
     * It scans the database for slots that are currently marked as 'available'
     * but are starting within the next 10 minutes.
     *
     * Logic:
     * 1. Calculate the 'cutoff' time (current time + 10 minutes).
     * 2. Query all available slots where the start time is before this cutoff.
     * 3. Batch update these slots to 'available = false'.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoInactiveSlot(){
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        List<Slot> slots = slotRepository.getSlotByStartTimeBeforeAndAvailableTrue(now.plusMinutes(CANCEL_BEFORE_START_MINUTES));
        if (slots.isEmpty()){
            log.info("No expired slots");
        }else{
            for (Slot slot : slots){
                slot.setAvailable(false);
                slotRepository.save(slot);
                log.info("Auto-cancelled slot id={}", slot.getId());
            }
        }
    }


}
