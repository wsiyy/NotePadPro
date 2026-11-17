package com.example.notepadpro;

/** 笔记实体类。 */
public class Note {

    private long id;
    private String title = "";
    private String content = "";
    private long createdAt;
    private long updatedAt;
    private String category = "其他";
    private boolean pinned;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? "其他" : category;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    /** 列表中显示的标题，空标题统一显示“无标题”。 */
    public String getDisplayTitle() {
        return title.trim().isEmpty() ? "无标题" : title;
    }

    /** 列表中显示的内容摘要（把换行替换为空格）。 */
    public String getSnippet() {
        return content.replace('\n', ' ').trim();
    }
}
