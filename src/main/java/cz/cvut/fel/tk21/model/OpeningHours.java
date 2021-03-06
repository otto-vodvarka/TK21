package cz.cvut.fel.tk21.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
public class OpeningHours extends AbstractEntity {

    @OneToOne(mappedBy = "openingHours")
    private Club club;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "monday_from", columnDefinition = "TIME")),
            @AttributeOverride(name = "to", column = @Column(name = "monday_to", columnDefinition = "TIME"))
    })
    private FromToTime monday;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "tuesday_from", columnDefinition = "TIME")),
            @AttributeOverride(name = "to", column = @Column(name = "tuesday_to", columnDefinition = "TIME"))
    })
    private FromToTime tuesday;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "wednesday_from", columnDefinition = "TIME")),
            @AttributeOverride(name = "to", column = @Column(name = "wednesday_to", columnDefinition = "TIME"))
    })
    private FromToTime wednesday;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "thursday_from", columnDefinition = "TIME")),
            @AttributeOverride(name = "to", column = @Column(name = "thursday_to", columnDefinition = "TIME"))
    })
    private FromToTime thursday;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "friday_from", columnDefinition = "TIME")),
            @AttributeOverride(name = "to", column = @Column(name = "friday_to", columnDefinition = "TIME"))
    })
    private FromToTime friday;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "saturday_from", columnDefinition = "TIME")),
            @AttributeOverride(name = "to", column = @Column(name = "saturday_to", columnDefinition = "TIME"))
    })
    private FromToTime saturday;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "sunday_from", columnDefinition = "TIME")),
            @AttributeOverride(name = "to", column = @Column(name = "sunday_to", columnDefinition = "TIME"))
    })
    private FromToTime sunday;

    @ElementCollection
    @CollectionTable(name = "SPECIAL_DAYS")
    @MapKeyColumn(columnDefinition = "DATE")
    private Map<LocalDate, FromToTime> specialDays = new HashMap<>();

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public FromToTime getMonday() {
        return monday;
    }

    public void setMonday(FromToTime monday) {
        this.monday = monday;
    }

    public FromToTime getTuesday() {
        return tuesday;
    }

    public void setTuesday(FromToTime tuesday) {
        this.tuesday = tuesday;
    }

    public FromToTime getWednesday() {
        return wednesday;
    }

    public void setWednesday(FromToTime wednesday) {
        this.wednesday = wednesday;
    }

    public FromToTime getThursday() {
        return thursday;
    }

    public void setThursday(FromToTime thursday) {
        this.thursday = thursday;
    }

    public FromToTime getFriday() {
        return friday;
    }

    public void setFriday(FromToTime friday) {
        this.friday = friday;
    }

    public FromToTime getSaturday() {
        return saturday;
    }

    public void setSaturday(FromToTime saturday) {
        this.saturday = saturday;
    }

    public FromToTime getSunday() {
        return sunday;
    }

    public void setSunday(FromToTime sunday) {
        this.sunday = sunday;
    }

    public Map<Day, FromToTime> getRegularHours() {
        Map<Day, FromToTime> regularHours = new HashMap<>();
        regularHours.put(Day.MONDAY, getRegularHoursAtDay(Day.MONDAY));
        regularHours.put(Day.TUESDAY, getRegularHoursAtDay(Day.TUESDAY));
        regularHours.put(Day.WEDNESDAY, getRegularHoursAtDay(Day.WEDNESDAY));
        regularHours.put(Day.THURSDAY, getRegularHoursAtDay(Day.THURSDAY));
        regularHours.put(Day.FRIDAY, getRegularHoursAtDay(Day.FRIDAY));
        regularHours.put(Day.SATURDAY, getRegularHoursAtDay(Day.SATURDAY));
        regularHours.put(Day.SUNDAY, getRegularHoursAtDay(Day.SUNDAY));
        return regularHours;
    }

    public void setRegularHours(Map<Day, FromToTime> openingHours) {
        for (Map.Entry<Day, FromToTime> entry : openingHours.entrySet()) {
            Day day = entry.getKey();
            FromToTime time = entry.getValue();
            updateRegularHours(day, time);
        }
    }

    public Map<LocalDate, FromToTime> getSpecialDays() {
        return specialDays;
    }

    public void setSpecialDays(Map<LocalDate, FromToTime> specialDays) {
        this.specialDays = specialDays;
    }

    public void addSpecialDate(LocalDate date, FromToTime time) {
        specialDays.put(date, time);
    }

    public void removeSpecialDate(LocalDate date) {
        specialDays.remove(date);
    }

    public boolean containsSpecialDate(LocalDate date) {
        return specialDays.containsKey(date);
    }

    public void updateSpecialDate(LocalDate date, FromToTime fromToTime) {
        specialDays.put(date, fromToTime);
    }

    public void updateRegularHours(Day day, FromToTime time) {
        if (time == null) time = new FromToTime();
        switch (day) {
            case MONDAY:
                monday = time;
                break;
            case TUESDAY:
                tuesday = time;
                break;
            case WEDNESDAY:
                wednesday = time;
                break;
            case THURSDAY:
                thursday = time;
                break;
            case FRIDAY:
                friday = time;
                break;
            case SATURDAY:
                saturday = time;
                break;
            case SUNDAY:
                sunday = time;
                break;
        }
    }

    public FromToTime getRegularHoursAtDay(Day day) {
        FromToTime hours = null;
        switch (day) {
            case MONDAY:
                hours = monday;
                break;
            case TUESDAY:
                hours = tuesday;
                break;
            case WEDNESDAY:
                hours = wednesday;
                break;
            case THURSDAY:
                hours = thursday;
                break;
            case FRIDAY:
                hours = friday;
                break;
            case SATURDAY:
                hours = saturday;
                break;
            case SUNDAY:
                hours = sunday;
                break;
        }
        if(hours == null){
            hours = new FromToTime();
        }
        return hours;
    }

    public Map<LocalDate, FromToTime> getSpecialDaysInYear(int year) {
        return specialDays.entrySet().stream()
                .filter(x -> x.getKey().getYear() == year)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<LocalDate, FromToTime> getSpecialDaysInNextDays(int days) {
        LocalDate target = LocalDate.now().plusDays(days + 1);
        LocalDate now = LocalDate.now().minusDays(1);
        return specialDays.entrySet().stream()
                .filter(x -> x.getKey().isAfter(now) && x.getKey().isBefore(target))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public boolean isOpenedAtDate(LocalDate date) {
        if (containsSpecialDate(date)) {
            return specialDays.get(date).getFrom() != null;
        }

        Day day = Day.getDayFromCode(date.getDayOfWeek().getValue());
        FromToTime hours = getRegularHoursAtDay(day);

        return hours.getFrom() != null;
    }

    public boolean isOpenedAtDateAndTime(LocalDate date, FromToTime time) {
        FromToTime hours = null;
        if (!isOpenedAtDate(date)) return false;
        if (containsSpecialDate(date)) {
            hours = specialDays.get(date);
        } else {
            Day day = Day.getDayFromCode(date.getDayOfWeek().getValue());
            hours = getRegularHoursAtDay(day);
        }

        //Start is before opening
        if (time.getFrom().isBefore(hours.getFrom())) return false;

        //Start after closing
        if (time.getFrom().isAfter(hours.getTo())) return false;

        //End is before opening
        if (time.getTo().isBefore(hours.getFrom())) return false;

        //End after closing
        if (time.getTo().isAfter(hours.getTo())) return false;

        return true;
    }

    public boolean isAfterOpeningAtThisTimeAndDate(LocalDate date, LocalTime time) {
        FromToTime hours = null;
        if (!isOpenedAtDate(date)) return true;
        if (containsSpecialDate(date)) {
            hours = specialDays.get(date);
        } else {
            Day day = Day.getDayFromCode(date.getDayOfWeek().getValue());
            hours = getRegularHoursAtDay(day);
        }
        return time.isAfter(hours.getTo());
    }

    public FromToTime getOpeningTimesAtDate(LocalDate date) {
        if (!isOpenedAtDate(date)) return null;
        return getRegularHoursAtDay(Day.getDayFromCode(date.getDayOfWeek().getValue()));
    }
}
