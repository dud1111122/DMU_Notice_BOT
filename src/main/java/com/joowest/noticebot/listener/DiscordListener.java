package com.joowest.noticebot.listener;

import com.joowest.noticebot.domain.Department;
import com.joowest.noticebot.domain.GuildSetting;
import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.domain.UserKeyword;
import com.joowest.noticebot.domain.UserSubscription;
import com.joowest.noticebot.repository.DepartmentRepository;
import com.joowest.noticebot.repository.GuildSettingRepository;
import com.joowest.noticebot.repository.NoticeRepository;
import com.joowest.noticebot.repository.UserKeywordRepository;
import com.joowest.noticebot.repository.UserSubscriptionRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DiscordListener extends ListenerAdapter {

    private static final String ALL_DEPT = "__ALL__";

    private final GuildSettingRepository guildSettingRepository;
    private final NoticeRepository noticeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserKeywordRepository userKeywordRepository;

    public DiscordListener(GuildSettingRepository guildSettingRepository,
                           NoticeRepository noticeRepository,
                           DepartmentRepository departmentRepository,
                           UserSubscriptionRepository userSubscriptionRepository,
                           UserKeywordRepository userKeywordRepository) {
        this.guildSettingRepository = guildSettingRepository;
        this.noticeRepository = noticeRepository;
        this.departmentRepository = departmentRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.userKeywordRepository = userKeywordRepository;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();

        switch (command) {
            case "공지" -> handleNoticeCommand(event);
            case "구독" -> handleSubscriptionCommand(event);
            case "키워드" -> handleKeywordCommand(event);
            case "설정" -> handleSettingCommand(event);
            case "도움말" -> handleHelpCommand(event);
            default -> event.reply("지원하지 않는 명령어입니다.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!"구독".equals(event.getName())) {
            return;
        }
        if (!"과".equals(event.getSubcommandName()) && !"취소".equals(event.getSubcommandName())) {
            return;
        }
        if (!"dept".equals(event.getFocusedOption().getName())) {
            return;
        }

        String query = event.getFocusedOption().getValue().toLowerCase();
        List<Department> departments = departmentRepository.findByEnabledTrueOrderBySortOrderAscDeptNameAsc();

        event.replyChoices(
                        departments.stream()
                                .filter(d -> d.getDeptName() != null && d.getDeptCode() != null)
                                .filter(d -> d.getDeptName().toLowerCase().contains(query)
                                        || d.getDeptCode().toLowerCase().contains(query))
                                .limit(25)
                                .map(d -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(
                                        d.getDeptName(),
                                        d.getDeptCode()
                                ))
                                .toList()
                )
                .queue();
    }

    private void handleNoticeCommand(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) {
            event.reply("`/공지` 하위 명령어를 선택해주세요.").setEphemeral(true).queue();
            return;
        }

        switch (sub) {
            case "최근" -> {
                List<Notice> notices = noticeRepository.findTop10ByOrderByCreatedAtDesc();
                event.reply(formatNoticeList("📢 최근 공지", notices)).queue();
            }
            case "오늘" -> {
                LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                LocalDateTime start = today.atStartOfDay();
                LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
                List<Notice> notices = noticeRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
                event.reply(formatNoticeList("📅 오늘 공지", notices)).queue();
            }
            case "검색" -> {
                OptionMapping keywordOption = event.getOption("keyword");
                if (keywordOption == null) {
                    event.reply("검색어를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String keyword = keywordOption.getAsString();
                List<Notice> notices = noticeRepository.findTop10ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
                event.reply(formatNoticeList("🔎 검색 결과: " + keyword, notices)).queue();
            }
            case "학과" -> {
                OptionMapping departmentOption = event.getOption("dept");
                if (departmentOption == null) {
                    event.reply("학과 코드를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String departmentCode = departmentOption.getAsString();
                List<Notice> notices = noticeRepository.findTop10ByDepartmentCodeIgnoreCaseOrderByCreatedAtDesc(departmentCode);
                event.reply(formatNoticeList("🏫 학과 공지: " + departmentCode, notices)).queue();
            }
            case "요약" -> {
                LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                LocalDateTime start = today.atStartOfDay();
                LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
                List<Notice> notices = noticeRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

                if (notices.isEmpty()) {
                    event.reply("🔹 오늘 공지가 없습니다.").queue();
                    return;
                }

                Map<String, Long> grouped = notices.stream()
                        .collect(Collectors.groupingBy(Notice::getDepartmentName, Collectors.counting()));

                StringBuilder sb = new StringBuilder();
                sb.append("🔹 오늘 공지 요약\n\n");
                sb.append("총 ").append(notices.size()).append("개\n\n");
                grouped.forEach((k, v) -> sb.append(k).append(" ").append(v).append("\n"));

                event.reply(sb.toString()).queue();
            }
            default -> event.reply("지원하지 않는 `/공지` 하위 명령어입니다.").setEphemeral(true).queue();
        }
    }

    private void handleSubscriptionCommand(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null || event.getGuild() == null || event.getUser() == null) {
            event.reply("서버에서 실행 가능한 명령어입니다.").setEphemeral(true).queue();
            return;
        }

        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();

        switch (sub) {
            case "과" -> {
                OptionMapping deptOption = event.getOption("dept");
                if (deptOption == null) {
                    event.reply("학과명을 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String deptCode = deptOption.getAsString();
                Optional<Department> department = departmentRepository.findByDeptCode(deptCode);

                if (department.isEmpty() || Boolean.FALSE.equals(department.get().getEnabled())) {
                    event.reply("유효한 학과를 선택해주세요.").setEphemeral(true).queue();
                    return;
                }

                Optional<UserSubscription> existing =
                        userSubscriptionRepository.findByUserIdAndGuildIdAndDepartmentCode(userId, guildId, deptCode);

                UserSubscription subscription = existing.orElseGet(() -> UserSubscription.builder()
                        .userId(userId)
                        .guildId(guildId)
                        .departmentCode(deptCode)
                        .createdAt(LocalDateTime.now())
                        .build());

                subscription.setEnabled(true);
                userSubscriptionRepository.save(subscription);

                event.reply("✅ 구독 등록 완료\n\n`" + department.get().getDeptName() + "` 공지를 구독합니다.")
                        .setEphemeral(true)
                        .queue();
            }
            case "취소" -> {
                OptionMapping deptOption = event.getOption("dept");
                if (deptOption == null) {
                    event.reply("취소할 학과를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }

                String deptCode = deptOption.getAsString();
                Optional<UserSubscription> existing =
                        userSubscriptionRepository.findByUserIdAndGuildIdAndDepartmentCode(userId, guildId, deptCode);

                if (existing.isEmpty()) {
                    event.reply("해당 학과 구독 내역이 없습니다.").setEphemeral(true).queue();
                    return;
                }

                UserSubscription subscription = existing.get();
                subscription.setEnabled(false);
                userSubscriptionRepository.save(subscription);

                String deptName = departmentRepository.findByDeptCode(deptCode)
                        .map(Department::getDeptName)
                        .orElse(deptCode);

                event.reply("🗑 구독 취소 완료\n\n`" + deptName + "` 공지 구독을 해제했습니다.")
                        .setEphemeral(true)
                        .queue();
            }
            case "전체" -> {
                OptionMapping enabledOption = event.getOption("enabled");
                if (enabledOption == null) {
                    event.reply("활성화 여부를 선택해주세요.").setEphemeral(true).queue();
                    return;
                }

                boolean enabled = enabledOption.getAsBoolean();
                Optional<UserSubscription> existing =
                        userSubscriptionRepository.findByUserIdAndGuildIdAndDepartmentCode(userId, guildId, ALL_DEPT);

                UserSubscription subscription = existing.orElseGet(() -> UserSubscription.builder()
                        .userId(userId)
                        .guildId(guildId)
                        .departmentCode(ALL_DEPT)
                        .createdAt(LocalDateTime.now())
                        .build());

                subscription.setEnabled(enabled);
                userSubscriptionRepository.save(subscription);

                event.reply("✅ 전체 공지 알림이 `" + (enabled ? "ON" : "OFF") + "`으로 설정되었습니다.")
                        .setEphemeral(true)
                        .queue();
            }
            case "목록" -> {
                List<UserSubscription> subs = userSubscriptionRepository.findByUserIdAndGuildId(userId, guildId);
                if (subs.isEmpty()) {
                    event.reply("현재 등록된 구독이 없습니다.").setEphemeral(true).queue();
                    return;
                }

                String list = subs.stream()
                        .map(s -> {
                            String label = ALL_DEPT.equals(s.getDepartmentCode()) ? "전체" : s.getDepartmentCode();
                            if (!ALL_DEPT.equals(s.getDepartmentCode())) {
                                label = departmentRepository.findByDeptCode(s.getDepartmentCode())
                                        .map(Department::getDeptName)
                                        .orElse(s.getDepartmentCode());
                            }
                            return "- " + label + " : " + (Boolean.TRUE.equals(s.getEnabled()) ? "ON" : "OFF");
                        })
                        .collect(Collectors.joining("\n"));

                event.reply("📚 내 구독 목록\n\n" + list).setEphemeral(true).queue();
            }
            default -> event.reply("지원하지 않는 `/구독` 하위 명령어입니다.").setEphemeral(true).queue();
        }
    }

    private void handleKeywordCommand(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null || event.getGuild() == null || event.getUser() == null) {
            event.reply("서버에서 실행 가능한 명령어입니다.").setEphemeral(true).queue();
            return;
        }

        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();

        switch (sub) {
            case "추가" -> {
                OptionMapping keywordOption = event.getOption("keyword");
                if (keywordOption == null) {
                    event.reply("키워드를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String keyword = keywordOption.getAsString();

                Optional<UserKeyword> existing =
                        userKeywordRepository.findByUserIdAndGuildIdAndKeyword(userId, guildId, keyword);

                if (existing.isPresent()) {
                    event.reply("이미 등록된 키워드입니다: `" + keyword + "`").setEphemeral(true).queue();
                    return;
                }

                userKeywordRepository.save(UserKeyword.builder()
                        .userId(userId)
                        .guildId(guildId)
                        .keyword(keyword)
                        .createdAt(LocalDateTime.now())
                        .build());

                event.reply("✅ 키워드 등록 완료: `" + keyword + "`").setEphemeral(true).queue();
            }
            case "삭제" -> {
                OptionMapping keywordOption = event.getOption("keyword");
                if (keywordOption == null) {
                    event.reply("삭제할 키워드를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String keyword = keywordOption.getAsString();
                Optional<UserKeyword> existing =
                        userKeywordRepository.findByUserIdAndGuildIdAndKeyword(userId, guildId, keyword);

                if (existing.isEmpty()) {
                    event.reply("등록되지 않은 키워드입니다: `" + keyword + "`").setEphemeral(true).queue();
                    return;
                }

                userKeywordRepository.delete(existing.get());
                event.reply("🗑 키워드 삭제 완료: `" + keyword + "`").setEphemeral(true).queue();
            }
            case "목록" -> {
                List<UserKeyword> keywords = userKeywordRepository.findByUserIdAndGuildId(userId, guildId);
                if (keywords.isEmpty()) {
                    event.reply("현재 등록된 키워드가 없습니다.").setEphemeral(true).queue();
                    return;
                }

                String list = keywords.stream()
                        .map(UserKeyword::getKeyword)
                        .map(k -> "- " + k)
                        .collect(Collectors.joining("\n"));

                event.reply("🏷 내 키워드 목록\n\n" + list).setEphemeral(true).queue();
            }
            default -> event.reply("지원하지 않는 `/키워드` 하위 명령어입니다.").setEphemeral(true).queue();
        }
    }

    private void handleSettingCommand(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("서버에서 실행 가능한 명령어입니다.").setEphemeral(true).queue();
            return;
        }

        if (event.getMember() == null ||
                !(event.getMember().hasPermission(Permission.ADMINISTRATOR) ||
                        event.getMember().hasPermission(Permission.MANAGE_SERVER))) {
            event.reply("이 명령어는 서버 관리자만 사용할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        String sub = event.getSubcommandName();
        if (sub == null) {
            event.reply("`/설정` 하위 명령어를 선택해주세요.").setEphemeral(true).queue();
            return;
        }

        String guildId = event.getGuild().getId();
        GuildSetting setting = guildSettingRepository.findByGuildId(guildId);
        if (setting == null) {
            setting = GuildSetting.builder()
                    .guildId(guildId)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        switch (sub) {
            case "채널" -> {
                OptionMapping channelOption = event.getOption("channel");
                if (channelOption == null) {
                    event.reply("채널을 선택해주세요.").setEphemeral(true).queue();
                    return;
                }
                if (channelOption.getAsChannel().getType() != ChannelType.TEXT) {
                    event.reply("텍스트 채널만 설정할 수 있습니다.").setEphemeral(true).queue();
                    return;
                }

                TextChannel textChannel = channelOption.getAsChannel().asTextChannel();
                setting.setChannelId(textChannel.getId());
                if (setting.getEnabled() == null) {
                    setting.setEnabled(true);
                }
                guildSettingRepository.save(setting);

                event.reply("✅ 공지 채널이 #" + textChannel.getName() + " 로 설정되었습니다.").queue();
            }
            case "알림" -> {
                OptionMapping enabledOption = event.getOption("enabled");
                if (enabledOption == null) {
                    event.reply("활성화 여부를 선택해주세요.").setEphemeral(true).queue();
                    return;
                }

                boolean enabled = enabledOption.getAsBoolean();
                setting.setEnabled(enabled);
                guildSettingRepository.save(setting);

                event.reply("✅ 서버 공지 알림이 `" + (enabled ? "ON" : "OFF") + "`으로 설정되었습니다.").queue();
            }
            default -> event.reply("지원하지 않는 `/설정` 하위 명령어입니다.").setEphemeral(true).queue();
        }
    }

    private void handleHelpCommand(SlashCommandInteractionEvent event) {
        String help = """
                📘 동미봇 명령어

                /공지 최근
                /공지 오늘
                /공지 검색 keyword
                /공지 학과 dept
                /공지 요약

                /구독 과 dept
                /구독 취소 dept
                /구독 전체 enabled
                /구독 목록

                /키워드 추가 keyword
                /키워드 삭제 keyword
                /키워드 목록

                /설정 채널 channel
                /설정 알림 enabled
                """;
        event.reply(help).setEphemeral(true).queue();
    }

    private String formatNoticeList(String title, List<Notice> notices) {
        if (notices.isEmpty()) {
            return title + "\n\n조회된 공지가 없습니다.";
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String list = notices.stream()
                .limit(10)
                .map(n -> {
                    String created = n.getCreatedAt() != null ? n.getCreatedAt().format(dateFormatter) : n.getDate();
                    return "- [" + created + "] " + n.getTitle() + "\n  " + n.getUrl();
                })
                .collect(Collectors.joining("\n"));

        return title + "\n\n" + list;
    }

    public void registerSlashCommands(JDA jda) {
        jda.updateCommands()
                .addCommands(
                        Commands.slash("공지", "공지 조회")
                                .addSubcommands(
                                        new SubcommandData("최근", "최근 공지 목록 조회"),
                                        new SubcommandData("오늘", "오늘 올라온 공지 조회"),
                                        new SubcommandData("검색", "키워드로 공지 검색")
                                                .addOption(OptionType.STRING, "keyword", "검색어", true),
                                        new SubcommandData("학과", "특정 학과 공지 조회")
                                                .addOption(OptionType.STRING, "dept", "학과 코드", true, true),
                                        new SubcommandData("요약", "오늘 공지 요약")
                                ),
                        Commands.slash("구독", "공지 알림 구독 관리")
                                .addSubcommands(
                                        new SubcommandData("과", "특정 학과 공지 구독")
                                                .addOption(OptionType.STRING, "dept", "학과명", true, true),
                                        new SubcommandData("취소", "특정 학과 공지 구독 취소")
                                                .addOption(OptionType.STRING, "dept", "학과명", true, true),
                                        new SubcommandData("전체", "전체 공지 알림 ON/OFF")
                                                .addOption(OptionType.BOOLEAN, "enabled", "활성화 여부", true),
                                        new SubcommandData("목록", "내 구독 목록 조회")
                                ),
                        Commands.slash("키워드", "키워드 알림 관리")
                                .addSubcommands(
                                        new SubcommandData("추가", "키워드 알림 추가")
                                                .addOption(OptionType.STRING, "keyword", "키워드", true),
                                        new SubcommandData("삭제", "키워드 알림 제거")
                                                .addOption(OptionType.STRING, "keyword", "키워드", true),
                                        new SubcommandData("목록", "등록 키워드 조회")
                                ),
                        Commands.slash("설정", "서버 관리자 설정")
                                .addSubcommands(
                                        new SubcommandData("채널", "공지 전송 채널 지정")
                                                .addOption(OptionType.CHANNEL, "channel", "공지 채널", true),
                                        new SubcommandData("알림", "서버 공지 알림 ON/OFF")
                                                .addOption(OptionType.BOOLEAN, "enabled", "활성화 여부", true)
                                ),
                        Commands.slash("도움말", "봇 사용법 안내")
                )
                .queue();
    }
}
