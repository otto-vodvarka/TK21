package cz.cvut.fel.tk21.rest.dto.reservation;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.fel.tk21.model.FromToTime;
import cz.cvut.fel.tk21.model.Reservation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateReservationDto {

    private String email;

    private String name;

    private String surname;

    private FromToTime time;

    private int courtId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy")
    private LocalDate date;

    public CreateReservationDto() {
    }

    public CreateReservationDto(Reservation reservation) {
        this.email = reservation.getEmail();
        this.name = reservation.getName();
        this.surname = reservation.getSurname();
        this.time = reservation.getFromToTime();
        this.courtId = reservation.getTennisCourt().getId();
        this.date = reservation.getDate();
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

    public FromToTime getTime() {
        return time;
    }

    public void setTime(FromToTime time) {
        this.time = time;
    }

    public int getCourtId() {
        return courtId;
    }

    public void setCourtId(int courtId) {
        this.courtId = courtId;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy")
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @JsonIgnore
    public Reservation getEntity(){
        Reservation reservation = new Reservation();

        reservation.setEmail(email);
        reservation.setName(name);
        reservation.setSurname(surname);
        reservation.setFromToTime(time);
        reservation.setDate(date);

        return reservation;
    }
}
