package com.joowest.noticebot.listener;

import com.joowest.noticebot.domain.AppUser;
import com.joowest.noticebot.domain.Department;
import com.joowest.noticebot.domain.GuildSetting;
import com.joowest.noticebot.domain.Keyword;
import com.joowest.noticebot.domain.Notice;
import com.joowest.noticebot.domain.Subscription;
import com.joowest.noticebot.domain.UserSetting;
import com.joowest.noticebot.repository.AppUserRepository;
import com.joowest.noticebot.repository.DepartmentRepository;
import com.joowest.noticebot.repository.GuildSettingRepository;
import com.joowest.noticebot.repository.KeywordRepository;
import com.joowest.noticebot.repository.NoticeRepository;
import com.joowest.noticebot.repository.SubscriptionRepository;
import com.joowest.noticebot.repository.UserSettingRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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

@Component
public class DiscordListener extends ListenerAdapter {

    private final AppUserRepository appUserRepository;
    private final GuildSettingRepository guildSettingRepository;
    private final UserSettingRepository userSettingRepository;
    private final NoticeRepository noticeRepository;
    private final DepartmentRepository departmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final KeywordRepository keywordRepository;

    public DiscordListener(AppUserRepository appUserRepository,
                           GuildSettingRepository guildSettingRepository,
                           UserSettingRepository userSettingRepository,
                           NoticeRepository noticeRepository,
                           DepartmentRepository departmentRepository,
                           SubscriptionRepository subscriptionRepository,
                           KeywordRepository keywordRepository) {
        this.appUserRepository = appUserRepository;
        this.guildSettingRepository = guildSettingRepository;
        this.userSettingRepository = userSettingRepository;
        this.noticeRepository = noticeRepository;
        this.departmentRepository = departmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.keywordRepository = keywordRepository;
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
        if (!"dept".equals(event.getFocusedOption().getName())) {
            return;
        }
        String subcommand = event.getSubcommandName();
        if (!"학과".equals(subcommand) && !"과".equals(subcommand) && !"취소".equals(subcommand)) {
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
                List<Notice> notices = noticeRepository.findTop10ByOrderByPostedAtDescCreatedAtDesc();
                event.reply(formatNoticeList("📢 최근 공지", notices)).queue();
            }
            case "오늘" -> {
                LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                LocalDateTime start = today.atStartOfDay();
                LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
                List<Notice> notices = noticeRepository.findByPostedAtBetweenOrderByPostedAtDescCreatedAtDesc(start, end);
                event.reply(formatNoticeList("📅 오늘 공지", notices)).queue();
            }
            case "검색" -> {
                OptionMapping keywordOption = event.getOption("keyword");
                if (keywordOption == null) {
                    event.reply("검색어를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String keyword = keywordOption.getAsString();
                List<Notice> notices = noticeRepository.findTop10ByTitleContainingIgnoreCaseOrderByPostedAtDescCreatedAtDesc(keyword);
                event.reply(formatNoticeList("🔎 검색 결과: " + keyword, notices)).queue();
            }
            case "학과" -> {
                OptionMapping departmentOption = event.getOption("dept");
                if (departmentOption == null) {
                    event.reply("학과 코드를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String departmentCode = departmentOption.getAsString();
                List<Notice> notices = noticeRepository.findTop10ByDepartmentDeptCodeIgnoreCaseOrderByPostedAtDescCreatedAtDesc(departmentCode);
                event.reply(formatNoticeList("🏫 학과 공지: " + departmentCode, notices)).queue();
            }
            case "요약" -> {
                LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                LocalDateTime start = today.atStartOfDay();
                LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
                List<Notice> notices = noticeRepository.findByPostedAtBetweenOrderByPostedAtDescCreatedAtDesc(start, end);

                if (notices.isEmpty()) {
                    event.reply("🔹 오늘 공지가 없습니다.").queue();
                    return;
                }

                Map<String, Long> grouped = notices.stream()
                        .collect(Collectors.groupingBy(this::noticeBucket, Collectors.counting()));

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

        AppUser user = ensureUser(event.getUser().getId(), event.getUser().getName());

        switch (sub) {
            case "과" -> {
                Optional<GuildSetting> existingGuildSetting = guildSettingRepository.findByGuildId(event.getGuild().getId());
                if (existingGuildSetting.isEmpty()
                        || existingGuildSetting.get().getChannelId() == null
                        || existingGuildSetting.get().getChannelId().isBlank()) {
                    event.reply("관리자가 먼저 `/설정 채널`로 공지 채널을 설정해주세요.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                GuildSetting guildSetting = existingGuildSetting.get();
                Department department = resolveDepartment(event.getOption("dept")).orElse(null);
                if (department == null || Boolean.FALSE.equals(department.getEnabled())) {
                    event.reply("유효한 학과를 선택해주세요.").setEphemeral(true).queue();
                    return;
                }

                Optional<Subscription> existing =
                        subscriptionRepository.findByUserIdAndGuildSettingIdAndDepartmentId(
                                user.getId(),
                                guildSetting.getId(),
                                department.getId()
                        );

                Subscription subscription = existing.orElseGet(() -> Subscription.builder()
                        .user(user)
                        .guildSetting(guildSetting)
                        .department(department)
                        .build());

                subscription.setEnabled(true);
                subscriptionRepository.save(subscription);

                event.reply("✅ 구독 등록 완료\n\n이 서버에서 `" + department.getDeptName() + "` 학과 공지를 구독합니다.")
                        .setEphemeral(true)
                        .queue();
            }
            case "취소" -> {
                GuildSetting guildSetting = ensureGuildSetting(event.getGuild().getId(), event.getGuild().getName());
                Department department = resolveDepartment(event.getOption("dept")).orElse(null);
                if (department == null) {
                    event.reply("유효한 학과를 선택해주세요.").setEphemeral(true).queue();
                    return;
                }

                Optional<Subscription> existing =
                        subscriptionRepository.findByUserIdAndGuildSettingIdAndDepartmentId(
                                user.getId(),
                                guildSetting.getId(),
                                department.getId()
                        );

                if (existing.isEmpty()) {
                    event.reply("해당 학과 구독 내역이 없습니다.").setEphemeral(true).queue();
                    return;
                }

                subscriptionRepository.delete(existing.get());

                event.reply("🗑 구독 취소 완료\n\n이 서버에서 `" + department.getDeptName() + "` 학과 공지를 해제했습니다.")
                        .setEphemeral(true)
                        .queue();
            }
            case "전체" -> {
                GuildSetting guildSetting = ensureGuildSetting(event.getGuild().getId(), event.getGuild().getName());
                OptionMapping enabledOption = event.getOption("enabled");
                if (enabledOption == null) {
                    event.reply("활성화 여부를 선택해주세요.").setEphemeral(true).queue();
                    return;
                }

                boolean enabled = enabledOption.getAsBoolean();
                UserSetting userSetting = ensureUserSetting(user, guildSetting);
                userSetting.setGlobalNoticeEnabled(enabled);
                userSettingRepository.save(userSetting);

                event.reply("✅ 이 서버의 학교 전체공지 수신이 `" + (enabled ? "ON" : "OFF") + "`으로 설정되었습니다.")
                        .setEphemeral(true)
                        .queue();
            }
            case "목록" -> {
                GuildSetting guildSetting = ensureGuildSetting(event.getGuild().getId(), event.getGuild().getName());
                UserSetting userSetting = ensureUserSetting(user, guildSetting);
                List<Subscription> subscriptions = subscriptionRepository.findByUserIdAndGuildSettingId(user.getId(), guildSetting.getId());

                String list = subscriptions.stream()
                        .filter(subscription -> Boolean.TRUE.equals(subscription.getEnabled()))
                        .map(subscription -> "- " + subscription.getDepartment().getDeptName())
                        .collect(Collectors.joining("\n"));

                StringBuilder sb = new StringBuilder();
                sb.append("📚 내 구독 목록\n\n");
                sb.append("- 학교 전체공지: ")
                        .append(Boolean.TRUE.equals(userSetting.getGlobalNoticeEnabled()) ? "ON" : "OFF");

                if (!list.isBlank()) {
                    sb.append("\n").append(list);
                }

                event.reply(sb.toString()).setEphemeral(true).queue();
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

        AppUser user = ensureUser(event.getUser().getId(), event.getUser().getName());
        GuildSetting guildSetting = ensureGuildSetting(event.getGuild().getId(), event.getGuild().getName());

        switch (sub) {
            case "추가" -> {
                OptionMapping keywordOption = event.getOption("keyword");
                if (keywordOption == null) {
                    event.reply("키워드를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String keyword = keywordOption.getAsString().trim();

                Optional<Keyword> existing =
                        keywordRepository.findByUserIdAndGuildSettingIdAndKeyword(user.getId(), guildSetting.getId(), keyword);

                if (existing.isPresent()) {
                    event.reply("이미 등록된 키워드입니다: `" + keyword + "`").setEphemeral(true).queue();
                    return;
                }

                keywordRepository.save(Keyword.builder()
                        .user(user)
                        .guildSetting(guildSetting)
                        .keyword(keyword)
                        .build());

                event.reply("✅ 키워드 등록 완료: `" + keyword + "`").setEphemeral(true).queue();
            }
            case "삭제" -> {
                OptionMapping keywordOption = event.getOption("keyword");
                if (keywordOption == null) {
                    event.reply("삭제할 키워드를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                String keyword = keywordOption.getAsString().trim();
                Optional<Keyword> existing =
                        keywordRepository.findByUserIdAndGuildSettingIdAndKeyword(user.getId(), guildSetting.getId(), keyword);

                if (existing.isEmpty()) {
                    event.reply("등록되지 않은 키워드입니다: `" + keyword + "`").setEphemeral(true).queue();
                    return;
                }

                keywordRepository.delete(existing.get());
                event.reply("🗑 키워드 삭제 완료: `" + keyword + "`").setEphemeral(true).queue();
            }
            case "목록" -> {
                List<Keyword> keywords = keywordRepository.findByUserIdAndGuildSettingId(user.getId(), guildSetting.getId());
                if (keywords.isEmpty()) {
                    event.reply("현재 등록된 키워드가 없습니다.").setEphemeral(true).queue();
                    return;
                }

                String list = keywords.stream()
                        .map(Keyword::getKeyword)
                        .map(value -> "- " + value)
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

        GuildSetting guildSetting = ensureGuildSetting(event.getGuild().getId(), event.getGuild().getName());

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
                guildSetting.setGuildName(event.getGuild().getName());
                guildSetting.setChannelId(textChannel.getId());
                guildSetting.setEnabled(true);
                guildSettingRepository.save(guildSetting);

                event.reply("✅ 공지 채널이 #" + textChannel.getName() + " 로 설정되었습니다.").queue();
            }
            case "알림" -> {
                OptionMapping enabledOption = event.getOption("enabled");
                if (enabledOption == null) {
                    event.reply("활성화 여부를 선택해주세요.").setEphemeral(true).queue();
                    return;
                }

                boolean enabled = enabledOption.getAsBoolean();
                guildSetting.setGuildName(event.getGuild().getName());
                guildSetting.setEnabled(enabled);
                guildSettingRepository.save(guildSetting);

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

                /구독 전체 enabled   -> 학교 전체공지 수신 ON/OFF
                /구독 과 dept        -> 특정 학과 공지 구독
                /구독 취소 dept      -> 특정 학과 공지 구독 취소
                /구독 목록

                /키워드 추가 keyword
                /키워드 삭제 keyword
                /키워드 목록

                /설정 채널 channel
                /설정 알림 enabled
                """;
        event.reply(help).setEphemeral(true).queue();
    }

    private Optional<Department> resolveDepartment(OptionMapping departmentOption) {
        if (departmentOption == null) {
            return Optional.empty();
        }
        return departmentRepository.findByDeptCode(departmentOption.getAsString());
    }

    private AppUser ensureUser(String discordId, String username) {
        return appUserRepository.findByDiscordId(discordId)
                .map(existing -> {
                    if (!Objects.equals(existing.getUsername(), username)) {
                        existing.setUsername(username);
                        return appUserRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> appUserRepository.save(AppUser.builder()
                        .discordId(discordId)
                        .username(username)
                        .build()));
    }

    private GuildSetting ensureGuildSetting(String guildId, String guildName) {
        return guildSettingRepository.findByGuildId(guildId)
                .map(existing -> {
                    if (!Objects.equals(existing.getGuildName(), guildName)) {
                        existing.setGuildName(guildName);
                        return guildSettingRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> guildSettingRepository.save(GuildSetting.builder()
                        .guildId(guildId)
                        .guildName(guildName)
                        .enabled(true)
                        .build()));
    }

    private UserSetting ensureUserSetting(AppUser user, GuildSetting guildSetting) {
        return userSettingRepository.findByUserIdAndGuildSettingId(user.getId(), guildSetting.getId())
                .orElseGet(() -> userSettingRepository.save(UserSetting.builder()
                        .user(user)
                        .guildSetting(guildSetting)
                        .globalNoticeEnabled(true)
                        .build()));
    }

    private String formatNoticeList(String title, List<Notice> notices) {
        if (notices.isEmpty()) {
            return title + "\n\n조회된 공지가 없습니다.";
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String list = notices.stream()
                .limit(10)
                .map(notice -> {
                    LocalDateTime baseTime = notice.getPostedAt() != null ? notice.getPostedAt() : notice.getCreatedAt();
                    String date = baseTime != null ? baseTime.format(dateFormatter) : "날짜 없음";
                    return "- [" + date + "] " + notice.getTitle() + " (" + noticeBucket(notice) + ")\n  " + notice.getUrl();
                })
                .collect(Collectors.joining("\n"));

        return title + "\n\n" + list;
    }

    private String noticeBucket(Notice notice) {
        if (notice.getDepartment() != null) {
            return notice.getDepartment().getDeptName();
        }
        if (notice.getGlobalNoticeSource() != null) {
            return notice.getGlobalNoticeSource().getSourceName();
        }
        return "미분류";
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
                                                .addOption(OptionType.STRING, "dept", "학과 코드", true, true),
                                        new SubcommandData("취소", "특정 학과 공지 구독 취소")
                                                .addOption(OptionType.STRING, "dept", "학과 코드", true, true),
                                        new SubcommandData("전체", "학교 전체공지 수신 ON/OFF")
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
