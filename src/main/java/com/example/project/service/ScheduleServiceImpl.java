package com.example.project.service;

import com.example.project.dto.PageRequestDTO;
import com.example.project.dto.PageResultDTO;
import com.example.project.dto.RegisterScheduleDTO;
import com.example.project.dto.ResponseScheduleDTO;
import com.example.project.dto.ScheduleBlockDTO;
import com.example.project.dto.ScheduleParticipantDTO;
import com.example.project.entity.*;
import com.example.project.repository.ActivityBlockRepository;
import com.example.project.repository.NotificationRepository;
import com.example.project.repository.PlannersRepository;
import com.example.project.repository.PlannersUserRepository;
import com.example.project.repository.ScheduleBlockRepository;
import com.example.project.repository.ScheduleParticipantRepository;
import com.example.project.repository.ScheduleRepository;
import com.example.project.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.data.autoconfigure.web.DataWebProperties.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Service
@Log4j2
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {
  private final NotificationRepository notificationRepository;
  private final PlannersServiceImpl plannersServiceImpl;
  private final PlannersRepository plannersRepository;
  private final PlannersUserRepository plannersUserRepository;
  private final ScheduleParticipantRepository scheduleParticipantRepository;
  private final ScheduleRepository scheduleRepository;
  private final UserRepository userRepository;
  private final ActivityBlockRepository activityBlockRepository;
  private final ScheduleBlockRepository scheduleBlockRepository;

  @Override
  public ResponseScheduleDTO get(Long sid, Long uid) {
    Optional<Schedule> result = scheduleRepository.findById(sid);
    if (result.isPresent()) {
      ResponseScheduleDTO dto = entityToDto(result.get());

      if (uid != null) {
        if (isCreator(sid, uid)) {
          dto.setUserStatus(ScheduleRole.CREATOR);
        } else if (isJoiner(sid, uid)) {
          dto.setUserStatus(ScheduleRole.FOLLOWER);
        } else {
          dto.setUserStatus(ScheduleRole.NONE);
        }
      } else {
        // uid가 null이면 로그인하지 않은 상태 → 기본값 유지
        dto.setUserStatus(ScheduleRole.NONE);
      }

      return dto;
    }
    return null;
  }

  @Override
  public PageResultDTO<ResponseScheduleDTO, Schedule> getScheduleList(Long tid, Long uid, PageRequestDTO pageRequestDTO,
                                                                      String filterStatus) {

    Page<Schedule> result;

    if (filterStatus == null || filterStatus.isBlank()) {
      result = scheduleRepository.findByPlanners_TidAndScheduleStatusNot(
          tid,
          ScheduleStatus.ENDED,
          pageRequestDTO.getPageable(Sort.by("sid").descending()));
    } else {
      result = scheduleRepository.findByPlanners_TidAndScheduleStatus(
          tid,
          ScheduleStatus.valueOf(filterStatus),
          pageRequestDTO.getPageable(Sort.by("sid").descending()));
    }

    Function<Schedule, ResponseScheduleDTO> fn = schedule -> {
      ResponseScheduleDTO dto = entityToDto(schedule);

      if (uid != null) {
        if (isCreator(schedule.getSid(), uid)) {
          dto.setUserStatus(ScheduleRole.CREATOR);
        } else if (isJoiner(schedule.getSid(), uid)) {
          dto.setUserStatus(ScheduleRole.FOLLOWER);
        } else {
          dto.setUserStatus(ScheduleRole.NONE);
        }
      } else {
        dto.setUserStatus(ScheduleRole.NONE);
      }

      return dto;
    };

    return new PageResultDTO<>(result, fn);
  }

  @Override
  public boolean existsMySchedule(Long tid, Long uid) {
    return scheduleRepository.existsMySchedule(tid, uid);
  }

  @Override
  public PageResultDTO<ResponseScheduleDTO, Schedule> getUserScheduleList(Long tid, Long uid, int scPage,
                                                                          String filterStatus) {
    PageRequest pageable = PageRequest.of(scPage - 1, 10);
    Page<Schedule> result = null;
    if (filterStatus == null || filterStatus.isBlank()) {
      result = scheduleRepository.findByPlanners_TidAndUser_UidWithNoFilter(tid, uid, pageable);
    } else {
      ScheduleStatus status = ScheduleStatus.valueOf(filterStatus);
      result = scheduleRepository.findByPlanners_TidAndUser_UidWithFilter(tid, uid, status, pageable);
    }

    Function<Schedule, ResponseScheduleDTO> fn = schedule -> {
      ResponseScheduleDTO dto = entityToDto(schedule);

      if (uid != null) {
        if (isCreator(schedule.getSid(), uid)) {
          dto.setUserStatus(ScheduleRole.CREATOR);
        } else if (isJoiner(schedule.getSid(), uid)) {
          dto.setUserStatus(ScheduleRole.FOLLOWER);
        } else {
          dto.setUserStatus(ScheduleRole.NONE);
        }
      } else {
        dto.setUserStatus(ScheduleRole.NONE);
      }

      return dto;
    };

    return new PageResultDTO<>(result, fn);
  }

  @Override
  public Long createSchedule(@RequestParam Long tid, Long uid, RegisterScheduleDTO registerScheduleDTO,
                             MultipartFile scheduleThumbnail, String uploadPath) throws IOException {

    validateImage(scheduleThumbnail);

    registerScheduleDTO.setScheduleThumbnail("/img/letsplan.png");

    if (scheduleThumbnail != null && !scheduleThumbnail.isEmpty()) {
      registerScheduleDTO.setScheduleThumbnail(saveScheduleImage(
          scheduleThumbnail, uploadPath, "thumbnail", "/scheduleThumbnail/"));
    }

    Schedule schedule = dtoToEntity(registerScheduleDTO);
    Optional<User> user = userRepository.findById(uid);
    Optional<Planners> planners = plannersRepository.findById(tid);
    schedule.setPlanners(planners.get());
    schedule.setCreator(user.get());
    schedule.setScheduleStatus(ScheduleStatus.PENDING);

    scheduleRepository.save(schedule);
    if (schedule.getSid() != null) {
      ScheduleParticipant scheduleParticipant = ScheduleParticipant.builder()
          .user(user.get())
          .schedule(schedule)
          .build();
      scheduleParticipantRepository.save(scheduleParticipant);

      List<PlannersUser> members = plannersUserRepository.getMemberListExceptMe(tid, uid);
      for (PlannersUser member : members) {
        Notification notification = Notification.builder()
            .user(member.getUser())
            .title(planners.get().getName() + " 플래너즈에서 새 일정이 등록되었습니다!")
            .type(NotificationType.SCHEDULE)
            .isRead(false)
            .url("/planners/schedule?tid=" + tid)
            .build();
        notificationRepository.save(notification);
      }
    }

    return schedule.getSid();
  }

  private void validateImage(MultipartFile image) {
    if (image != null && !image.isEmpty() && !image.getContentType().startsWith("image")) {
      throw new IllegalArgumentException("이미지 형식이 올바르지 않습니다.");
    }
  }

  private String saveScheduleImage(MultipartFile image, String uploadPath, String imageType, String webPrefix)
      throws IOException {
    String originalName = image.getOriginalFilename();
    String fileName = originalName == null ? "image" : originalName.substring(originalName.lastIndexOf("\\") + 1);
    String folderPath = makeFolder(uploadPath, imageType);
    String uuid = UUID.randomUUID().toString();
    String saveFileName = uuid + "_" + fileName;

    String saveName = uploadPath + File.separator + "schedule" + File.separator + imageType
        + File.separator + folderPath + File.separator + saveFileName;
    Path savePath = Paths.get(saveName);
    image.transferTo(savePath);

    return webPrefix + folderPath.replace(File.separator, "/") + "/" + saveFileName;
  }

  private String makeFolder(String uploadPath, String imageType) {
    String str = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    String folderPath = str.replace("/", File.separator);
    File uploadPathFolder = new File(uploadPath + File.separator + "schedule" + File.separator + imageType, folderPath);

    if (!uploadPathFolder.exists()) {
      uploadPathFolder.mkdirs();
    }

    return folderPath;
  }

  @Override
  public boolean isJoiner(Long sid, Long uid) {
    return scheduleParticipantRepository.existsBySchedule_SidAndUser_Uid(sid, uid);
  }

  // 일정을 만든 사람 or 플래너즈 관리자인지 여부 조회 (True or False)
  @Override
  public boolean isCreator(Long sid, Long uid) {
    Optional<Schedule> schedule = scheduleRepository.findById(sid);
    if (schedule.isPresent()) {
      if (uid == plannersServiceImpl.getOwner(schedule.get().getPlanners().getTid()).getUid() ||
          scheduleRepository.existsBySidAndCreator_Uid(sid, uid)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Long submitSchedule(Long sid, List<ScheduleBlockDTO> scheduleBlockDTO) {
    if (scheduleBlockDTO.isEmpty()) {
      return -1L;
    }
    for (ScheduleBlockDTO dto : scheduleBlockDTO) {
      Optional<ActivityBlock> activityBlock = activityBlockRepository.findById(dto.getBid());
      Optional<Schedule> schedule = scheduleRepository.findById(sid);

      if (activityBlock.isPresent() && schedule.isPresent()) {
        ScheduleBlock scheduleBlock = ScheduleBlock.builder()
            .block(activityBlockRepository.findById(dto.getBid()).get())
            .schedule(scheduleRepository.findById(sid).get())
            .startTime(LocalDateTime.of(dto.getDate(), dto.getStartTime()))
            .endTime(LocalDateTime.of(
                dto.getEndTime().isBefore(dto.getStartTime()) ? dto.getDate().plusDays(1) : dto.getDate(),
                dto.getEndTime()))
            .build();
        scheduleBlockRepository.save(scheduleBlock);
        schedule.get().setScheduleStatus(ScheduleStatus.SCHEDULED);
        scheduleRepository.save(schedule.get());
      } else {
        return 0L;
      }
    }
    return 1L;
  }

  @Override
  public Long removeSchedule(Long sid, Long uid) {
    Optional<Schedule> schedule = scheduleRepository.findById(sid);

    if (schedule.isPresent()) {
      if (isCreator(sid, uid)) { // 일정 생성자 혹은 플래너즈 관리자일시
        if (schedule.get().getScheduleStatus() != ScheduleStatus.PENDING) {
          noteSchedule_canceled(sid);
        }
        scheduleRepository.delete(schedule.get());
        return 1L;
      }
      return 0L;
    }
    return -1L;
  }

  // 스케쥴 블록 땡겨오기
  @Override
  public List<ScheduleBlockDTO> getScheduleBlockList(Long sid) {
    List<ScheduleBlock> blocks = scheduleBlockRepository.findBySchedule_Sid(sid);
    List<ScheduleBlockDTO> dto = blocks.stream().map(block -> ScheduleBlockDTO.builder()
        .bid(block.getBid())
        .name(block.getBlock().getName())
        .dateStr(block.getStartTime().toLocalDate().toString())
        .startTimeStr(block.getStartTime().toLocalTime().toString())
        .endTimeStr(block.getEndTime().toLocalTime().toString())
        .build()).toList();

    return dto;
  }

  // 스케쥴 참가자 땡겨오기
  @Override
  public List<ScheduleParticipantDTO> getScheduleParticipantsList(Long sid) {
    List<ScheduleParticipant> people = scheduleParticipantRepository.findBySchedule_Sid(sid);
    List<ScheduleParticipantDTO> dto = people.stream().map(person -> ScheduleParticipantDTO.builder()
        .pid(person.getPid())
        .uid(person.getUser().getUid())
        .sid(person.getSchedule().getSid())
        .nickname(person.getUser().getName())
        .profileImage(person.getUser().getProfileImg())
        .build()).toList();

    return dto;
  }

  // 일정 참가
  @Override
  public Long participate(Long uid, Long sid) {
    if (scheduleParticipantRepository.existsBySchedule_SidAndUser_Uid(sid, uid)) {
      return -1L; // 이미 참가신청한 일정일 시
    }

    Optional<User> userTmp = userRepository.findById(uid);
    Optional<Schedule> scheduleTmp = scheduleRepository.findById(sid);

    if (userTmp.isPresent() && scheduleTmp.isPresent()) {
      Schedule schedule = scheduleTmp.get();
      if (schedule.getPopulation() == schedule.getMaxPopulation()) {
        return -2L;
      }
      ScheduleParticipant scheduleParticipant = ScheduleParticipant.builder()
          .user(userTmp.get())
          .schedule(schedule)
          .build();
      scheduleParticipantRepository.save(scheduleParticipant);
      schedule.increasePopulation();
      scheduleRepository.save(schedule);
      return 1L;
    }
    return 0L;
  }

  @Override
  public Long cancelParticipate(Long uid, Long sid) {
    Optional<ScheduleParticipant> participantTmp = scheduleParticipantRepository.findBySchedule_SidAndUser_Uid(sid,
        uid);
    Optional<Schedule> scheduleTmp = scheduleRepository.findById(sid);

    if (participantTmp.isPresent()) {
      ScheduleParticipant participant = participantTmp.get();
      if (scheduleRepository.findById(sid).get().getCreator().getUid().equals(uid)) {
        return -1L;
      }
      Schedule schedule = scheduleTmp.get();
      schedule.decreasePopulation();
      scheduleRepository.save(schedule);
      scheduleParticipantRepository.delete(participant);
      return 1L;
    }
    return 0L;
  }

  @Override
  public boolean isParticipant(Long uid, Long sid) {
    return scheduleParticipantRepository.existsBySchedule_SidAndUser_Uid(sid, uid);
  }

  @Override
  public Long closeSchedule(Long sid, Long uid) {
    Optional<Schedule> scheduleTmp = scheduleRepository.findById(sid);

    if (scheduleTmp.isPresent()) {
      if (isCreator(sid, uid)) { // 일정 생성자가 맞거나 플래너즈 관리자일시
        Schedule schedule = scheduleTmp.get();
        schedule.setScheduleStatus(ScheduleStatus.CLOSED);
        scheduleRepository.save(schedule);
        return 1L;
      }
      return 0L;
    }
    return -1L;
  }

  @Override
  public boolean isClosed(Long sid) {
    return scheduleRepository.isClosed(sid);
  }

  @Scheduled(cron = "0 * * * * *") // 1분마다 일정 체크 갱신
  @Transactional
  public void updateEndedSchedules() {
    scheduleRepository.updateEndedSchedules(LocalDateTime.now());
  }

  // 하루 남은 일정에 대한 알림 전송
  @Scheduled(cron = "0 0 0 * * *")
  public void checkDday() {
    List<Schedule> list = scheduleRepository.findSchedulesByDate(LocalDate.now().plusDays(1));

    for (Schedule schedule : list) {
      List<ScheduleParticipant> participants = scheduleParticipantRepository.findBySchedule_Sid(schedule.getSid());
      for (ScheduleParticipant participant : participants) {
        Notification notification = Notification.builder()
            .user(participant.getUser())
            .title(schedule.getTitle() + " 일정이 하루 남았습니다!")
            .isRead(false)
            .url("/planners/schedule/detail?tid=" + schedule.getPlanners().getTid() + "&sid=" + schedule.getSid()) // 내
            // 일정
            // 목록은
            // 여기서
            // 추가
            .type(NotificationType.SCHEDULE)
            .build();
        notificationRepository.save(notification);
      }
    }
  }

  public void noteSchedule_canceled(Long sid) {

    Optional<Schedule> scheduleTmp = scheduleRepository.findById(sid);
    if (scheduleTmp.isPresent()) {
      Schedule schedule = scheduleTmp.get();
      // 취소 일정들 알림 가기
      List<ScheduleParticipant> participants = scheduleParticipantRepository.findBySchedule_Sid(schedule.getSid());
      for (ScheduleParticipant participant : participants) {
        Notification notification = Notification.builder()
            .user(participant.getUser())
            .title(schedule.getTitle() + " 일정이 취소되었습니다!")
            .isRead(false)
            .url("#")
            .type(NotificationType.SCHEDULE)
            .build();
        notificationRepository.save(notification);
      }
    }
  }

  public Long modifyScheduleTitle(Long sid, String title) {
    Optional<Schedule> scheduleTmp = scheduleRepository.findById(sid);

    if (scheduleTmp.isPresent()) {
      Schedule schedule = scheduleTmp.get();
      schedule.setTitle(title);
      scheduleRepository.save(schedule);
      return 1L;
    }
    return 0L;
  }
}