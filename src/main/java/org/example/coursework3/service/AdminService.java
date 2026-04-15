package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.CreateSpecialistRequest;
import org.example.coursework3.dto.request.EditSpecialistRequest;
import org.example.coursework3.dto.request.SlotRequest;
import org.example.coursework3.dto.response.BookingPageResult;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.*;
import org.example.coursework3.vo.AdminSlotVo;
import org.example.coursework3.vo.MyBookingVo;
import org.hibernate.grammars.hql.HqlParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SpecialistsRepository specialistsRepository;
    private final ExpertiseRepository expertiseRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public Specialist createSpecialist(CreateSpecialistRequest request) {
        if (userRepository.findByEmail(request.getUserEmail()).isPresent()) {
            throw new MsgException("该邮箱已被注册");
        }

        // 1. 保存用户
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getUserEmail());
        user.setRole(Role.Specialist);
        user.setPasswordHash(request.getPassword());
        userRepository.save(user);

        // 2. 创建专家（userId = user.getId()）
        Specialist specialist = new Specialist(user.getId(), user.getName(), request.getPrice(), request.getBio());
        List<Expertise> expertiseList = new ArrayList<>();
        for (String expertiseId : request.getExpertiseIds()) {
            Expertise expertise = expertiseRepository.findById(expertiseId)
                    .orElseThrow(() -> new MsgException("专长不存在"));
            expertiseList.add(expertise);
        }
        specialist.setExpertises(expertiseList);
        specialistsRepository.save(specialist); // JPA 自动维护中间表specialist_expertise
        return specialist;
    }

    public Specialist updateSpecialist(String id, EditSpecialistRequest request) {
        Specialist specialist;
        try {
            specialist = specialistsRepository.getByUserId(id);
        } catch (Exception e) {
            throw new MsgException("该专家不存在");
        }
        if (request.getName() != null) {
            User user = userRepository.findById(id);
            user.setName(request.getName());
            userRepository.save(user);
            specialist.setName(request.getName());
        }
        if (request.getBio() != null) {
            specialist.setBio(request.getBio());
        }
        if (request.getPrice() != null) {
            specialist.setPrice(request.getPrice());
        }
        if (request.getStatus() != null) {
            specialist.setStatus(request.getStatus());
        }
        if (request.getExpertiseIds() != null) {
            List<Expertise> expertiseList = new ArrayList<>();
            for (String expertiseId : request.getExpertiseIds()) {
                Expertise expertise = expertiseRepository.findById(expertiseId)
                        .orElseThrow(() -> new MsgException("专长不存在"));
                expertiseList.add(expertise);
            }
            specialist.setExpertises(expertiseList);
        }
        specialistsRepository.save(specialist);
        return specialist;
    }

    @Transactional
    public Specialist updateSpecialistStatus(String id, SpecialistStatus status) {
        Specialist specialist;
        try {
            specialist = specialistsRepository.getByUserId(id);
        } catch (Exception e) {
            throw new MsgException("该专家不存在");
        }
        specialist.setStatus(status);
        specialistsRepository.save(specialist);
        return specialist;
    }

    @Transactional
    public void deleteSpecialist(String id) {
        User user = userRepository.getUserById(id);
        Specialist specialist = specialistsRepository.getByUserId(id);
        specialistsRepository.delete(specialist);
        userRepository.delete(user);
    }

    public Expertise createExpertise(String name, String description) {
        if (expertiseRepository.existsByName(name)) {
            throw new MsgException("该专家长已存在");
        }
        Expertise expertise = new Expertise();
        expertise.setName(name);
        expertise.setDescription(description);
        expertiseRepository.save(expertise);
        return expertise;
    }

    @Transactional
    public Expertise updateExpertise(String id, String name, String description) {
        Expertise expertise = null;
        try {
            expertise = expertiseRepository.getExpertiseById(id);
        } catch (Exception e) {
            throw new MsgException("该专长不存在");
        }
        expertise.setName(name);
        expertise.setDescription(description);
        expertiseRepository.save(expertise);
        return expertise;
    }

    public void deleteExpertise(String id) {
        if (!expertiseRepository.existsById(id)) {
            throw new MsgException("请输入有效专长ID");
        }
        expertiseRepository.deleteById(id);
    }

    public List<AdminSlotVo> listSlots(String specialistId, String date, String from, String to, Boolean available) {
        List<Slot> slots;
        if (specialistId != null && !specialistId.isBlank()) {
            slots = slotRepository.findBySpecialistId(specialistId.trim());
        } else {
            slots = slotRepository.findAll();
        }

        LocalDate filterDate = parseLocalDateOrNull(date);
        LocalTime fromTime = parseLocalTimeOrNull(from);
        LocalTime toTime = parseLocalTimeOrNull(to);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<AdminSlotVo> result = new ArrayList<>();

        for (Slot s : slots) {
            if (s == null) continue;
            if (available != null && !Objects.equals(available, s.getAvailable())) continue;

            LocalDateTime start = s.getStartTime();
            LocalDateTime end = s.getEndTime();
            if (start == null || end == null) continue;

            if (filterDate != null && !filterDate.equals(start.toLocalDate())) continue;

            if (fromTime != null && start.toLocalTime().isBefore(fromTime)) continue;
            if (toTime != null && start.toLocalTime().isAfter(toTime)) continue;

            AdminSlotVo vo = new AdminSlotVo();
            vo.setId(s.getId());
            vo.setSpecialistId(s.getSpecialistId());
            vo.setDate(start.toLocalDate().toString());
            vo.setStart(start.toLocalTime().format(timeFmt));
            vo.setEnd(end.toLocalTime().format(timeFmt));
            vo.setAvailable(s.getAvailable());
            result.add(vo);
        }

        return result;
    }

    public AdminSlotVo createSlot(String specialistId, String date, String from, String to, Boolean available) {
        if (specialistId == null || specialistId.isBlank()) {
            throw new MsgException("specialistId不能为空");
        }
        if (date == null || date.isBlank() || from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new MsgException("date/start/end不能为空");
        }

        List<Slot> slots = slotRepository.findBySpecialistId(specialistId.trim());
        LocalDateTime startNew = parseDateAndTime(date.trim(), from.trim());
        LocalDateTime endNew = parseDateAndTime(date.trim(), to.trim());
        if (!startNew.isBefore(endNew)) {
            throw new MsgException("开始时间必须早于结束时间");
        }

        for (Slot old : slots) {
            if (old == null) continue;
            if (isTimeOverlap(startNew, endNew, old.getStartTime(), old.getEndTime())) {
                throw new MsgException("时段冲突！");
            }
        }

        Slot slot = new Slot();
        slot.setSpecialistId(specialistId.trim());
        slot.setStartTime(startNew);
        slot.setEndTime(endNew);
        slot.setAvailable(available);
        slotRepository.save(slot);

        AdminSlotVo vo = new AdminSlotVo();
        vo.setId(slot.getId());
        vo.setSpecialistId(slot.getSpecialistId());
        vo.setDate(startNew.toLocalDate().toString());
        vo.setStart(from.trim());
        vo.setEnd(to.trim());
        vo.setAvailable(slot.getAvailable());
        return vo;
    }

    public AdminSlotVo updateSlot(String id, SlotRequest request) {
        Slot slot = slotRepository.getSlotById(id);
        if (slot == null) {
            throw new MsgException("时段无效，无法编辑");
        }
        if (request == null) {
            throw new MsgException("请求体不能为空");
        }

        String date = request.getDate();
        String startStr = request.getStart();
        String endStr = request.getEnd();
        if (date == null || date.isBlank() || startStr == null || startStr.isBlank() || endStr == null || endStr.isBlank()) {
            throw new MsgException("date/start/end不能为空");
        }

        LocalDateTime startNew = parseDateAndTime(date.trim(), startStr.trim());
        LocalDateTime endNew = parseDateAndTime(date.trim(), endStr.trim());
        if (!startNew.isBefore(endNew)) {
            throw new MsgException("开始时间必须早于结束时间");
        }


        List<Slot> slots = slotRepository.findBySpecialistId(slot.getSpecialistId());
        for (Slot old : slots) {
            if (old == null) continue;
            if (old.getId() != null && old.getId().equals(slot.getId())) continue;
            if (isTimeOverlap(startNew, endNew, old.getStartTime(), old.getEndTime())) {
                throw new MsgException("时段冲突！");
            }
        }

        slot.setStartTime(startNew);
        slot.setEndTime(endNew);
        if (request.getAvailable() != null) {
            slot.setAvailable(request.getAvailable());
        }
        slotRepository.save(slot);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        AdminSlotVo vo = new AdminSlotVo();
        vo.setId(slot.getId());
        vo.setSpecialistId(slot.getSpecialistId());
        vo.setDate(slot.getStartTime().toLocalDate().toString());
        vo.setStart(slot.getStartTime().toLocalTime().format(timeFmt));
        vo.setEnd(slot.getEndTime().toLocalTime().format(timeFmt));
        vo.setAvailable(slot.getAvailable());
        return vo;
    }

    public void deleteSlot(String id) {
        if (id == null || id.isBlank()) {
            throw new MsgException("slotId不能为空");
        }
        if (!slotRepository.existsById(id)) {
            throw new MsgException("时段不存在");
        }
        slotRepository.deleteById(id);
    }

    private LocalDate parseLocalDateOrNull(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return LocalDate.parse(v.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new MsgException("日期格式错误：" + v);
        }
    }

    private LocalTime parseLocalTimeOrNull(String v) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim();
        try {
            return LocalTime.parse(t);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(t).toLocalTime();
        } catch (DateTimeParseException ignored) {
        }
        throw new MsgException("时间格式错误：" + v);
    }

    private LocalDateTime parseDateAndTime(String date, String time) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // 2. 解析日期和时间字符串为 LocalDate/LocalTime
        LocalDate localDate = LocalDate.parse(date, dateFormatter);
        LocalTime localTime = LocalTime.parse(time, timeFormatter);

        // 3. 拼接为 LocalDateTime 并返回
        return LocalDateTime.of(localDate, localTime);
    }

    private boolean isTimeOverlap(LocalDateTime newStart, LocalDateTime newEnd,
                                  LocalDateTime oldStart, LocalDateTime oldEnd) {
        return newStart.isBefore(oldEnd) && oldStart.isBefore(newEnd);
    }

    public BookingPageResult listBookings(Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        List<Booking> bookingList = bookingRepository.findAll();
        int total = bookingList.size();
        int start = Math.min((safePage - 1) * safePageSize, total);
        int end = Math.min(start + safePageSize, total);
        List<Booking> pageItems = bookingList.subList(start, end);
        return BookingPageResult.of(pageItems, total, safePage, safePageSize);
    }


}
