package me.xemor.herodrafter.commands;

import me.xemor.herodrafter.DataManager;
import me.xemor.herodrafter.Player;
import me.xemor.herodrafter.HeroDrafter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.*;

public class QueueCommand implements Command {

    @Override
    public void executeCommand(SlashCommandEvent e) {
        switch (e.getSubcommandName()) {
            case "join" -> joinQueue(e);
            case "leave" -> leaveQueue(e);
            case "add" -> executeIfPrivileged(e, this::addQueue);
            case "kick" -> executeIfPrivileged(e, this::kickQueue);
            case "view" -> viewQueue(e);
            case "vc" -> executeIfPrivileged(e, this::addVC);
        }
    }

    public void addVC(SlashCommandEvent e) {
        try {
            e.getMember().getVoiceState().getChannel().getMembers().forEach((member) -> addQueue(member.getUser(), e.getHook()));
        } catch (NullPointerException ex) {
            e.getHook().sendMessage(HeroDrafter.getDataManager().getConfig().getQueueVCErrorMessage()).queue();
        }
    }

    public void viewQueue(SlashCommandEvent e) {
        Deque<Player> queue = HeroDrafter.getMatchHandler().getQueue();
        List<RestAction<User>> retrieved = new ArrayList<>();
        queue.forEach((player -> retrieved.add(e.getJDA().retrieveUserById(player.getId()))));
        if (queue.size() == 0) { e.getHook().sendMessage(HeroDrafter.getDataManager().getConfig().getEmptyQueueMessage()).queue(); return; }
        RestAction.allOf(retrieved).queue((List<User> users) -> {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Queue");
            embedBuilder.setColor(HeroDrafter.getDataManager().getConfig().getColor());
            String queueStr = users.stream()
                    .filter(Objects::nonNull)
                    .map(User::getName)
                    .reduce((string1, string2) -> string1 + "\n" + string2)
                    .orElse("");
            embedBuilder.addField("Queue", queueStr, true);
            e.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
        });
    }

    public void joinQueue(SlashCommandEvent e) {
        User user = e.getUser();
        InteractionHook hook = e.getHook();
        addQueue(user, hook);
    }

    public void leaveQueue(SlashCommandEvent e) {
        User user = e.getUser();
        InteractionHook hook = e.getHook();
        kickQueue(user, hook);
    }

    public void addQueue(SlashCommandEvent e) {
        addQueue(e.getOption("user").getAsUser(), e.getHook());
    }

    public void addQueue(User user, InteractionHook hook) {
        DataManager dataManager = HeroDrafter.getDataManager();
        Optional<Player> player = dataManager.getPlayer(user.getIdLong());
        if (player.isPresent()) {
            HeroDrafter.getMatchHandler().addToQueue(player.get());
            hook.sendMessage(dataManager.getConfig().getJoinQueueMessage().replace("%user_name%", user.getName())).queue();
        } else {
            hook.sendMessage(dataManager.getConfig().getNeedProfileMessage().replace("%user_name%", user.getName())).queue();
        }
    }

    public void kickQueue(SlashCommandEvent e) {
        kickQueue(e.getOption("user").getAsUser(), e.getHook());
    }

    public void kickQueue(User user, InteractionHook hook) {
        DataManager dataManager = HeroDrafter.getDataManager();
        Optional<Player> optionalPlayer = dataManager.getPlayer(user.getIdLong());
        if (optionalPlayer.isPresent()) {
            HeroDrafter.getMatchHandler().removeFromQueue(optionalPlayer.get());
            hook.sendMessage(dataManager.getConfig().getLeaveQueueMessage().replace("%user_name%", user.getName())).queue();
        } else {
            hook.sendMessage(dataManager.getConfig().getNeedProfileMessage().replace("%user_name%", user.getName())).queue();
        }
    }

}
