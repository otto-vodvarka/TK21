package cz.cvut.fel.tk21.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.cvut.fel.tk21.exception.NotFoundException;
import cz.cvut.fel.tk21.model.Club;
import cz.cvut.fel.tk21.model.Reservation;
import cz.cvut.fel.tk21.model.User;
import cz.cvut.fel.tk21.model.security.UserDetails;
import cz.cvut.fel.tk21.service.ClubService;
import cz.cvut.fel.tk21.service.ReservationService;
import cz.cvut.fel.tk21.ws.dto.ReservationMessage;
import cz.cvut.fel.tk21.ws.dto.UpdateReservationMessage;
import cz.cvut.fel.tk21.ws.dto.UpdateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class WebsocketService {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private WebsocketHandler handler;

    public ReservationMessage createInitialMessage(User user, int clubId, LocalDate date) {
        Optional<Club> club = clubService.find(clubId);
        club.orElseThrow(() -> new NotFoundException("Klub nebyl nalezen"));

        if(date == null){
            date = reservationService.findNearestAvailableReservationDate(club.get());
        }

        return reservationService.initialReservationMessage(club.get(), date, user);
    }

    public void sendUpdateMessageToSubscribers(int clubId, LocalDate date, Reservation reservation, UpdateType updateType){
        List<WebSocketSession> subscribers = handler.findAllSubscriptions(clubId, date);
        for(WebSocketSession session : subscribers) {
            boolean editable = false;
            boolean mine = false;
            if(updateType != UpdateType.DELETE){
                User user = extractUserFromSession(session);
                editable = this.isReservationEditable(reservation, user);
                mine = this.isReservationMine(reservation, user);
            }
            handler.sendMessageToSession(session, new UpdateReservationMessage(updateType, reservation, editable, mine));
        }
    }

    private boolean isReservationEditable(Reservation reservation, User user){
        return reservationService.isUserAllowedToEditReservation(user, reservation);
    }

    private boolean isReservationMine(Reservation reservation, User user){
        return reservationService.isOwner(reservation, user);
    }

    private User extractUserFromSession(WebSocketSession session){
        if(session.getPrincipal() == null) return null;
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) session.getPrincipal();
        UserDetails userDetails = (UserDetails) token.getPrincipal();
        return userDetails.getUser();
    }

}
