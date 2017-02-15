package il.co.anyway.app.models;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Accident {

    // accident severity
    public static final int SEVERITY_FATAL = 1;
    public static final int SEVERITY_SEVERE = 2;
    public static final int SEVERITY_LIGHT = 3;
    public static final int SEVERITY_VARIOUS = 4;
    // accident subtype
    public static final int ACCIDENT_MULTIPLE = -10;
    // set finals for accident subtype
    public static final int ACCIDENT_TYPE_CAR_TO_CAR = -1; // Synthetic type
    public static final int ACCIDENT_TYPE_CAR_TO_OBJECT = -2; // Synthetic type
    public static final int ACCIDENT_TYPE_CAR_TO_PEDESTRIAN = 1;
    public static final int ACCIDENT_TYPE_FRONT_TO_SIDE = 2;
    public static final int ACCIDENT_TYPE_FRONT_TO_REAR = 3;
    public static final int ACCIDENT_TYPE_SIDE_TO_SIDE = 4;
    public static final int ACCIDENT_TYPE_FRONT_TO_FRONT = 5;
    public static final int ACCIDENT_TYPE_WITH_STOPPED_CAR_NO_PARKING = 6;
    public static final int ACCIDENT_TYPE_WITH_STOPPED_CAR_PARKING = 7;
    public static final int ACCIDENT_TYPE_WITH_STILL_OBJECT = 8;
    public static final int ACCIDENT_TYPE_OFF_ROAD_OR_SIDEWALK = 9;
    public static final int ACCIDENT_TYPE_ROLLOVER = 10;
    public static final int ACCIDENT_TYPE_SKID = 11;
    public static final int ACCIDENT_TYPE_HIT_PASSSENGER_IN_CAR = 12;
    public static final int ACCIDENT_TYPE_FALLING_OFF_MOVING_VEHICLE = 13;
    public static final int ACCIDENT_TYPE_FIRE = 14;
    public static final int ACCIDENT_TYPE_OTHER = 15;
    public static final int ACCIDENT_TYPE_BACK_TO_FRONT = 17;
    public static final int ACCIDENT_TYPE_BACK_TO_SIDE = 18;
    public static final int ACCIDENT_TYPE_WITH_ANIMAL = 19;
    public static final int ACCIDENT_TYPE_WITH_VEHICLE_LOAD = 20;
    @SuppressWarnings("unused")
    private final String LOG_TAG = Accident.class.getSimpleName();
    // accident fields
    private long id;
    private String title;
    private String description;
    private int type;
    private int subType;
    private int severity;
    private Date created;
    private LatLng location;
    private String address;
    private int locationAccuracy;
    private boolean markerAddedToMap;

    public Accident(long id, String title, String description, int type, int subType, int severity,
                    Date created, LatLng location, String address, int locationAccuracy) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.subType = subType;
        this.severity = severity;
        this.created = created;
        this.location = location;
        this.address = address;
        this.locationAccuracy = locationAccuracy;
        this.markerAddedToMap = false;
    }

    public Accident() {
        this.markerAddedToMap = false;
    }

    public long getId() {
        return id;
    }

    public Accident setId(long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "Accident{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", subType=" + subType +
                ", severity=" + severity +
                ", created=" + created +
                ", location=" + location +
                ", address='" + address + '\'' +
                ", locationAccuracy=" + locationAccuracy +
                ", markerAddedToMap=" + markerAddedToMap +
                '}';
    }

    public Accident setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Accident setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getType() {
        return type;
    }

    public Accident setType(int type) {
        this.type = type;
        return this;
    }

    public int getSubType() {
        return subType;
    }

    public Accident setSubType(int subType) {
        this.subType = subType;
        return this;
    }

    public int getSeverity() {
        return severity;
    }

    public Accident setSeverity(int severity) {
        this.severity = severity;
        return this;
    }

    public Date getCreatedDate() {
        return created;
    }

    public Accident setCreatedDate(Date created) {
        this.created = created;
        return this;
    }

    public String getCreatedDateAsString() {

        String format = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(created);

    }

    public LatLng getLocation() {
        return location;
    }

    public Accident setLocation(LatLng location) {
        this.location = location;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public Accident setAddress(String address) {
        this.address = address;
        return this;
    }

    public int getLocationAccuracy() {
        return locationAccuracy;
    }

    public Accident setLocationAccuracy(int locationAccuracy) {
        this.locationAccuracy = locationAccuracy;
        return this;
    }

    public boolean isMarkerAddedToMap() {
        return markerAddedToMap;
    }

    public Accident setMarkerAddedToMap(boolean markerAddedToMap) {
        this.markerAddedToMap = markerAddedToMap;
        return this;
    }
}