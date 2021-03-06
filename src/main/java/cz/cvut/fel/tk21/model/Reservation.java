package cz.cvut.fel.tk21.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class Reservation extends AbstractEntity {

    @Column(nullable = false, columnDefinition = "DATE")
    private LocalDate date;

    @Embedded
    private FromToTime fromToTime;

    @ManyToOne(optional = false)
    private TennisCourt tennisCourt;

    @ManyToOne
    private User user;

    @ManyToOne
    private Club club;

    @Column
    private int cyclicReservationId; // -1 for non-cyclic reservation

    @OneToOne(mappedBy = "initialReservation")
    private CyclicReservation initialCyclicReservation;

    // for non-registered users
    @Column
    private String email;

    @Column
    private String name;

    @Column
    private String surname;

    @Column
    private String token;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public FromToTime getFromToTime() {
        return fromToTime;
    }

    public void setFromToTime(FromToTime fromToTime) {
        this.fromToTime = fromToTime;
    }

    public TennisCourt getTennisCourt() {
        return tennisCourt;
    }

    public void setTennisCourt(TennisCourt tennisCourt) {
        this.tennisCourt = tennisCourt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getCyclicReservationId() {
        return cyclicReservationId;
    }

    public void setCyclicReservationId(int cyclicReservationId) {
        this.cyclicReservationId = cyclicReservationId;
    }

    public boolean isCyclicReservation(){
        return cyclicReservationId != -1;
    }

    public CyclicReservation getInitialCyclicReservation() {
        return initialCyclicReservation;
    }

    public void setInitialCyclicReservation(CyclicReservation initialCyclicReservation) {
        this.initialCyclicReservation = initialCyclicReservation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Club getClub(){
        return club;
    }

    public boolean isForRegisteredUser(){
        return user != null;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public boolean collides(Reservation r){
        return collides(r.fromToTime);
    }

    public boolean collides(FromToTime time){
        if(time.getFrom().equals(fromToTime.getFrom())) return true;
        if(time.getTo().equals(fromToTime.getTo())) return true;

        if(time.getFrom().isAfter(fromToTime.getFrom()) && time.getFrom().isBefore(fromToTime.getTo())) return true;

        if(time.getTo().isAfter(fromToTime.getFrom()) && time.getTo().isBefore(fromToTime.getTo())) return true;

        if(time.getFrom().isBefore(fromToTime.getFrom()) && time.getTo().isAfter(fromToTime.getTo())) return true;

        return false;
    }

    public int getDuration(){
       int fromMinutes = fromToTime.getFrom().getHour() * 60 + fromToTime.getFrom().getMinute();
       int toMinutes = fromToTime.getTo().getHour() * 60 + fromToTime.getTo().getMinute();
       return toMinutes - fromMinutes;
    }
}
