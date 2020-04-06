package cz.cvut.fel.tk21.service;

import cz.cvut.fel.tk21.config.properties.AppProperties;
import cz.cvut.fel.tk21.dao.ConfirmationTokenDao;
import cz.cvut.fel.tk21.dao.UserDao;
import cz.cvut.fel.tk21.exception.ValidationException;
import cz.cvut.fel.tk21.model.User;
import cz.cvut.fel.tk21.model.mail.ConfirmationToken;
import cz.cvut.fel.tk21.model.mail.Mail;
import cz.cvut.fel.tk21.rest.dto.user.UserDto;
import cz.cvut.fel.tk21.service.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService extends BaseService<UserDao, User> {


    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final ConfirmationTokenDao confirmationTokenDao;
    private final AuthenticationManager authenticationManager;
    private final AppProperties appProperties;

    @Autowired
    protected UserService(UserDao dao, PasswordEncoder passwordEncoder, MailService mailService, ConfirmationTokenDao confirmationTokenDao, AuthenticationManager authenticationManager, AppProperties appProperties) {
        super(dao);
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.confirmationTokenDao = confirmationTokenDao;
        this.authenticationManager = authenticationManager;
        this.appProperties = appProperties;
    }

    @Transactional
    public int createUser(UserDto userDto) {
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

        if (!dao.isEmailUnique(userDto.getEmail())) {
            throw new ValidationException("Účet s tímto emailem již existuje");
        }

        User user = new User();
        user.setName(userDto.getName());
        user.setSurname(userDto.getSurname());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword());
        user.setVerifiedAccount(false);

        ConfirmationToken token = new ConfirmationToken(user);
        user.setConfirmationToken(token);

        super.dao.persist(user);

        //Email with confirmation token
        sendEmailConfirmationEmail(user.getEmail(), token.getConfirmationToken());

        return user.getId();
    }

    @Transactional
    public User createNonVerifiedUser(String name, String surname, String email){
        User user = new User();
        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);
        user.setPassword(null);
        user.setVerifiedAccount(false);

        dao.persist(user);
        return user;
    }

    @Transactional
    public boolean isEmailTokenValid(String token){
        Optional<ConfirmationToken> confirmationToken = confirmationTokenDao.findByConfirmationToken(token);
        if(confirmationToken.isPresent()){
            if (!confirmationToken.get().isValid()) throw new ValidationException("Platnost tokenu již vypršela");
            Optional<User> user = dao.getUserByEmail(confirmationToken.get().getUser().getEmail());
            if(user.isPresent()){
                user.get().setVerifiedAccount(true);
                dao.update(user.get());
            }else{
                return false;
            }
            return true;
        }

        return false;
    }

    @Transactional
    public Optional<User> findUserByEmail(String email){
        return dao.getUserByEmail(email);
    }

    @Transactional
    public Optional<User> findUserByWebId(long webId){
        return dao.getUserByWebId(webId);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(){
        if (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken) return null;
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = dao.getUserByEmail(email);
        if(user.isEmpty()) return null;
        User entity = user.get();
        return entity;
    }

    @Transactional
    public void updateName(String name){
        User user = getCurrentUser();
        if(user.isScraped()) throw new ValidationException("Při provázání s cztenis nelze jméno měnit");
        user.setName(name);
        this.update(user);
    }

    @Transactional
    public void updateSurname(String surname){
        User user = getCurrentUser();
        user.setSurname(surname);
        this.update(user);
    }

    @Transactional
    public void updateEmail(String email){
        if (!dao.isEmailUnique(email)) {
            throw new ValidationException("Účet s tímto emailem již existuje");
        }

        User user = getCurrentUser();
        user.setEmail(email);
        user.setVerifiedAccount(false);

        ConfirmationToken token = new ConfirmationToken(user);
        user.setConfirmationToken(token);

        super.dao.update(user);

        //Email with confirmation token
        sendEmailConfirmationEmail(user.getEmail(), token.getConfirmationToken());
    }

    @Transactional
    public void updatePassword(String oldPass, String newPass){
        User user = getCurrentUser();

        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), oldPass));
        } catch (BadCredentialsException ex) {
            throw new ValidationException("Hesla se neshodují");
        }

        user.setPassword(passwordEncoder.encode(newPass));
        this.update(user);
    }

    private void sendEmailConfirmationEmail(String email, String token){
        Mail mail = new Mail();
        mail.setFrom("noreply@tk21.cz");
        mail.setTo(email);
        mail.setSubject("Potvrzení emailové adresy");

        Map<String, Object> model = new HashMap<>();
        model.put("token", token);
        mail.setModel(model);

        mailService.sendEmailConfirmation(mail);
    }
}
