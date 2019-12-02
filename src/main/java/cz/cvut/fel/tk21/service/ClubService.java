package cz.cvut.fel.tk21.service;

import cz.cvut.fel.tk21.dao.ClubDao;
import cz.cvut.fel.tk21.dao.ClubRelationDao;
import cz.cvut.fel.tk21.dao.UserDao;
import cz.cvut.fel.tk21.exception.ValidationException;
import cz.cvut.fel.tk21.model.Club;
import cz.cvut.fel.tk21.model.ClubRelation;
import cz.cvut.fel.tk21.model.User;
import cz.cvut.fel.tk21.model.UserRole;
import cz.cvut.fel.tk21.rest.dto.ClubRegistrationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClubService extends BaseService<ClubDao, Club> {

    @Autowired
    private UserDao userDao;

    @Autowired
    private ClubRelationDao clubRelationDao;

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
        dao.persist(club);

        //links signed in user with this club as admin
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if(email == null) throw new ValidationException("Uživatel neexistuje");
        Optional<User> user = userDao.getUserByEmail(email);
        if(user.isEmpty()) throw new ValidationException("Uživatel neexistuje");

        ClubRelation relation = new ClubRelation();
        relation.setClub(club);
        relation.setUser(user.get());
        relation.addRole(UserRole.ADMIN);
        clubRelationDao.persist(relation);

        return club.getId();
    }

}