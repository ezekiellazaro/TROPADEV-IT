package barangaydamagereporter.model;

/**
 * A public facility (hall, road, bridge...). A DamageReport HAS-A Facility
 * (composition: one object stored inside another object).
 */
public class Facility {

    // OOP CONCEPT: ENCAPSULATION
    private String name;
    private String type;
    private String barangay;

    public Facility(String name, String type, String barangay) {
        this.name = name;
        this.type = type;
        this.barangay = barangay;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }

    @Override
    public String toString() {
        return name + " (" + type + ", " + barangay + ")";
    }
}
