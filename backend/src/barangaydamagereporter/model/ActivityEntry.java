package barangaydamagereporter.model;

/** One line of the "Recent Activity" list (e.g. "Status updated"). */
public class ActivityEntry {

    // ENCAPSULATION: private fields, read-only getters (an entry never changes)
    private final String kind;    // "ADD", "STATUS", "EDIT" or "DELETE"
    private final String title;
    private final String detail;
    private final String time;
    private final String owner;   // reporter name of the report concerned

    public ActivityEntry(String kind, String title, String detail, String time, String owner) {
        this.kind = kind;
        this.title = title;
        this.detail = detail;
        this.time = time;
        this.owner = owner;
    }

    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getTime() { return time; }
    public String getOwner() { return owner; }
}
