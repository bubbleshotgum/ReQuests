package ru.winxboyz.requests.utils;

public class Tracker {
    private final QuestData quest;
    private long startTime;
    private Long cooldownStartTime;
    private boolean dirty = true;
    private final int[] progress;

    public QuestData getQuest() {
        return quest;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clear() {
        dirty = false;
    }

    public long getStartTime() {
        return startTime;
    }

    public Long getCooldownStartTime() {
        return cooldownStartTime;
    }

    public Long getResetAt() {
        return cooldownStartTime == null ? null : cooldownStartTime+quest.getCooldown();
    }

    public int[] getProgress() {
        return progress.clone();
    }

    public int getProgress(int i) {
        if(i < 0 || i >= progress.length)
            return -1;
        return progress[i];
    }

    public int getProgressLength() {
        return progress.length;
    }

    public void setProgress(int index, int amount) {
        if(index < 0 || index >= progress.length)
            return;
        progress[index] = amount;
        dirty = true;
    }

    public void increaseProgress(int index, int amount) {
        if(index < 0 || index >= progress.length)
            return;
        progress[index] += amount;
        dirty = true;
    }

    public boolean isOnCooldown() {
        return cooldownStartTime != null && System.currentTimeMillis() <= getResetAt();
    }

    public void sendOnCooldown() {
        cooldownStartTime = System.currentTimeMillis();
        dirty = true;
    }

    public Tracker(QuestData quest, long startTime, long cooldownStartTime) {
        this.quest = quest;
        progress = new int[quest.getObjectives().size()];
        // for(int i = 0; i < progress.length; i++)
        //     progress[i] = 0;
        this.startTime = startTime;
        this.cooldownStartTime = cooldownStartTime;
    }
    public Tracker(QuestData quest, long startTime) {
        this.quest = quest;
        progress = new int[quest.getObjectives().size()];
        // for(int i = 0; i < progress.length; i++)
        //     progress[i] = 0;
        this.startTime = startTime;
        this.cooldownStartTime = null;
    }
}
