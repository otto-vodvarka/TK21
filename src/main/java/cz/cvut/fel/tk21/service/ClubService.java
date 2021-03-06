package cz.cvut.fel.tk21.service;

import cz.cvut.fel.tk21.annotation.ClubManagementOnly;
import cz.cvut.fel.tk21.dao.ClubDao;
import cz.cvut.fel.tk21.exception.BadRequestException;
import cz.cvut.fel.tk21.exception.NotFoundException;
import cz.cvut.fel.tk21.exception.UnauthorizedException;
import cz.cvut.fel.tk21.exception.ValidationException;
import cz.cvut.fel.tk21.model.*;
import cz.cvut.fel.tk21.model.mail.Mail;
import cz.cvut.fel.tk21.rest.dto.club.*;
import cz.cvut.fel.tk21.service.mail.MailService;
import cz.cvut.fel.tk21.util.DateUtils;
import cz.cvut.fel.tk21.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ClubService extends BaseService<ClubDao, Club> {

    @Autowired
    private ClubRelationService clubRelationService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private CourtService courtService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private VerificationRequestService verificationRequestService;

    @Autowired
    private PostService postService;

    @Autowired
    private MailService mailService;

    protected ClubService(ClubDao dao) {
        super(dao);
    }

    @Transactional
    public Integer registerClub(ClubRegistrationDto clubDto){
        if (!dao.isNameUnique(clubDto.getName())) {
            throw new ValidationException("Klub s tímto jménem již existuje");
        }
        if (!dao.isAddressUnique(clubDto.getAddress().getEntity())) {
            throw new ValidationException("Klub s touto adresou již existuje");
        }

        Club club = clubDto.getEntity();

        //Initial opening hours
        club.setOpeningHours(getInitialOpeningHours());

        //Initial season
        club.setSeasons(getInitialSeason(DateUtils.getCurrentYear()));

        club.setReservationPermission(ReservationPermission.SIGNED);
        club.setMinReservationTime(15);
        club.setMaxReservationTime(180);
        club.setRegistered(true);
        club.setReservationsEnabled(true);

        //links signed user with this club as admin
        User user = userService.getCurrentUser();
        String email = user.getEmail();
        club.setEmails(new ArrayList<>(List.of(email)));

        dao.persist(club);
        clubRelationService.addUserToClub(club, user, UserRole.ADMIN);

        this.sendClubRegisteredInfoEmail(user.getEmail(), club);

        return club.getId();
    }

    @Transactional
    public void registerScrapedClub(Club club){
        if(userService.getCurrentUser() == null) throw new UnauthorizedException("Přístup odepřen");
        if(!club.isContactEmail(userService.getCurrentUser().getEmail())) throw new UnauthorizedException("Přístup odepřen");
        if(!club.isWebScraped()) throw new BadRequestException("Špatný dotaz");
        if(club.isRegistered()) throw new ValidationException("Tento klub je již registrován");

        //Initial opening hours
        club.setOpeningHours(getInitialOpeningHours());

        //Initial season
        club.setSeasons(getInitialSeason(DateUtils.getCurrentYear()));

        club.setReservationPermission(ReservationPermission.SIGNED);
        club.setMinReservationTime(15);
        club.setMaxReservationTime(180);
        club.setRegistered(true);

        User user = userService.getCurrentUser();

        dao.update(club);
        clubRelationService.addUserToClub(club, user, UserRole.ADMIN);

        this.sendClubRegisteredInfoEmail(user.getEmail(), club);
    }

    @Transactional
    public void deleteClub(Club club){
        if(!clubRelationService.hasRole(club, userService.getCurrentUser(), UserRole.ADMIN)) throw new UnauthorizedException("Přístup odepřen");

        clubRelationService.deleteAllRelationsByClub(club);
        verificationRequestService.deleteAllVerificationRequestsByClub(club);
        postService.deleteAllPostsByClub(club);

        if(club.isWebScraped()){
            club.setRegistered(false);
            this.update(club);
        } else {
            this.remove(club);
        }
    }

    @Transactional
    public boolean isCurrentUserAllowedToManageThisClub(Club club){
        User user = userService.getCurrentUser();
        if(user == null) return false;
        return clubRelationService.hasRole(club, user, UserRole.ADMIN);
    }

    @Transactional
    public boolean isUserAllowedToManageThisClub(User user, Club club){
        if(user == null) return false;
        return clubRelationService.hasRole(club, user, UserRole.ADMIN);
    }

    @Transactional(readOnly = true)
    public ClubSearchDto findAllPaginated(int page, int size) {
        return searchForClubsByNameOrCity("", page, size);
    }

    @Transactional
    public ClubSearchDto searchForClubsByNameOrCity(String name, int page, int size){
        List<BasicClubInfoDto> clubs = new ArrayList<>();
        for(Club club : dao.findClubsByNameOrCity(name, page, size)){
            clubs.add(new BasicClubInfoDto(club));
        }
        int lastPage = (int) Math.ceil(dao.countClubsByNameOrCity(name) / (double)size);
        return new ClubSearchDto(clubs, page, lastPage);
    }

    @Transactional(readOnly = true)
    public Optional<Club> findClubByWebId(int webId){
        return dao.findClubByWebId(webId);
    }

    @Transactional(readOnly = true)
    public Optional<Club> findClubByName(String name){
        return dao.findClubByName(name);
    }

    @Transactional(readOnly = true)
    public Optional<Club> findClubByNameCaseInsensitive(String name){
        return dao.findClubByNameCaseInsensitive(name);
    }

    @Transactional(readOnly = true)
    public List<Club> findAllClubsByContactEmail(String email){
        return dao.findAllByContactEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Club> findAllScrapedClubs(){
        return dao.findAllScrapedClubs();
    }

    @Transactional
    @ClubManagementOnly
    public void addCourt(Club club, TennisCourt tennisCourt){
        if(!courtService.isNameUniqueInClub(club, tennisCourt.getName())) throw new ValidationException("Kurt s tímto jménem již existuje");
        club.addCourt(tennisCourt);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void removeCourt(Club club, TennisCourt tennisCourt){
        club.removeCourt(tennisCourt);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void addSeason(Club club, Season season, int year){
        if(season.getSummer().getFrom().getYear() != year || season.getWinter().getFrom().getYear() != year)
            throw new BadRequestException("Datumy nesedí");
        club.addSeasonInYear(year, season);
        this.update(club);
    }

    @Transactional
    public void addDefaultSeason(Club club, int year){
        club.addSeasonInYear(year, getDefaultSeason(year));
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateSeason(Club club, Season season, int year){
        if(season.getSummer().getFrom().getYear() != year || season.getWinter().getFrom().getYear() != year)
            throw new BadRequestException("Datumy nesedí");
        club.addSeasonInYear(year, season);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateReservationSeasonSettings(Club club, ReservationSeasonSettingsDto dto){
        Season season = club.getSeasonInYear(dto.getYear());
        if(season == null){
            throw new NotFoundException("Sezóna nebyla nalezena");
        }

        if(dto.isWinter()){
            season.setWinterResEnabled(dto.isEnable());
        } else {
            season.setSummerResEnabled(dto.isEnable());
        }

        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateReservationPermission(Club club, ReservationPermission reservationPermission){
        club.setReservationPermission(reservationPermission);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateMinReservationTime(Club club, int time){
        if(time < 0) time = 0;
        club.setMinReservationTime(time);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateMaxReservationTime(Club club, int time){
        if(time < 0) time = 0;
        club.setMaxReservationTime(time);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateName(Club club, String name){
        if(club.isWebScraped()) throw new ValidationException("U klubu převzatého z Cztenis nelze tato položka upravovat");
        if(clubService.findClubByNameCaseInsensitive(name).isPresent()) throw new ValidationException("Klub s tímto názvem již existuje");
        club.setName(name);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateDescription(Club club, String description){
        club.setDescription(description);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateAddress(Club club, Address address){
        if(club.isWebScraped()) throw new ValidationException("U klubu převzatého z Cztenis nelze tato položka upravovat");
        club.setAddress(address);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void updateContact(Club club, ContactDto contactDto){
        if(!club.isWebScraped()){
            if(contactDto.getEmails() == null || contactDto.getEmails().isEmpty()) throw new ValidationException("Musíte mít alespoň jeden email");
            for (String email : contactDto.getEmails()){
                if(!StringUtils.isValidEmail(email)) throw new ValidationException("Nevalidní email");
            }
            club.setEmails(contactDto.getEmails());
        }
        club.setWeb(contactDto.getWeb());
        club.setTelephone(contactDto.getTelephone());
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void enableReservations(Club club, boolean enable){
        club.setReservationsEnabled(enable);
        this.update(club);
    }

    @Transactional
    @ClubManagementOnly
    public void blockUser(Club club, User user){
        if(club.isUserBlocked(user)) return;
        if(!verificationRequestService.hasUserUnresolvedRequest(club, user)) throw new ValidationException("Tohoto uživatele nelze zablokovat");

        club.addToBlocked(user);
        this.update(club);

        verificationRequestService.processVerification(club, user, "DENIED");
    }

    @Transactional
    @ClubManagementOnly
    public void unblockUser(Club club, User user){
        if(!club.isUserBlocked(user)) return;

        club.removeFromBlocked(user);
        this.update(club);
    }

    private OpeningHours getInitialOpeningHours(){
        OpeningHours openingHours = new OpeningHours();

        Map<Day, FromToTime> hoursMap = new HashMap<>();
        hoursMap.put(Day.MONDAY, new FromToTime("09:00", "21:00"));
        hoursMap.put(Day.TUESDAY, new FromToTime("09:00", "21:00"));
        hoursMap.put(Day.WEDNESDAY, new FromToTime("09:00", "21:00"));
        hoursMap.put(Day.THURSDAY, new FromToTime("09:00", "21:00"));
        hoursMap.put(Day.FRIDAY, new FromToTime("09:00", "21:00"));
        hoursMap.put(Day.SATURDAY, new FromToTime("09:00", "21:00"));
        hoursMap.put(Day.SUNDAY, new FromToTime("09:00", "21:00"));
        openingHours.setRegularHours(hoursMap);

        return openingHours;
    }

    private Map<Integer, Season> getInitialSeason(int year){
        Map<Integer, Season> seasons = new HashMap<>();

        seasons.put(year - 1, getDefaultSeason(year - 1));
        seasons.put(year, getDefaultSeason(year));
        seasons.put(year + 1, getDefaultSeason(year + 1));

        return seasons;
    }

    private Season getDefaultSeason(int year){
        Season season = new Season(new FromToDate("04-01-" + year, "09-30-" + year), new FromToDate("10-01-" + year, "03-30-" + (year+1)));
        season.setSummerResEnabled(true);
        season.setWinterResEnabled(true);
        return season;
    }

    @Autowired
    public void setClubRelationService(ClubRelationService clubRelationService) {
        this.clubRelationService = clubRelationService;
    }

    private void sendClubRegisteredInfoEmail(String email, Club club){
        Mail mail = new Mail();
        mail.setFrom("noreply@tk21.cz");
        mail.setTo(email);
        mail.setSubject("Registrace klubu");

        Map<String, Object> model = new HashMap<>();
        model.put("clubID", club.getId());
        model.put("name", club.getName());
        mail.setModel(model);

        mailService.sendClubRegisteredInfo(mail);
    }
}
