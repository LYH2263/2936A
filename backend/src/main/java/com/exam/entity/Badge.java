package com.exam.entity;

public enum Badge {
    FIRST_SUBMIT("初次交卷", "🏆", "首次提交答卷"),
    PERFECT_SCORE("满分王者", "👑", "获得满分成绩"),
    PASS_STREAK_3("连续及格3次", "🔥", "连续3次考试及格"),
    WEEKLY_ACTIVE("周活跃答题", "📅", "一周内参加3场及以上考试"),
    APPEAL_WIN("申诉胜诉", "⚖️", "申诉被批准"),
    CORRECTION_EXPERT("纠错达人", "🔍", "纠错反馈被确认");

    private final String label;
    private final String icon;
    private final String description;

    Badge(String label, String icon, String description) {
        this.label = label;
        this.icon = icon;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}
