package at.ac.fhcampuswien.asd.rest.service;

import at.ac.fhcampuswien.asd.entity.models.User;
import at.ac.fhcampuswien.asd.entity.services.UserEntityService;
import at.ac.fhcampuswien.asd.exceptions.*;
import at.ac.fhcampuswien.asd.helper.Hashing;
import at.ac.fhcampuswien.asd.rest.model.InboundUserChangePasswordDto;
import at.ac.fhcampuswien.asd.rest.model.InboundUserRegistrationDto;
import at.ac.fhcampuswien.asd.rest.model.OutboundUserRegistrationDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserRestService {

    final static String sessionIdName = "X-SESSION-ID";
    UserEntityService userEntityService;

    /**
     * Compares the password specified by the user with the encrypted version in the database.
     *
     * @param user            The user for which the password is compared.
     * @param enteredPassword The password entered.
     * @return Returns true if the password matches, false otherwise.
     */
    public boolean comparePassword(User user, String enteredPassword) {
        byte[] hash = Hashing.generateHash(enteredPassword, user.getSalt());
        return Arrays.equals(hash, user.getPassword());

    }

    /**
     * Compares the time on user action with the time the session is supposed to be valid
     *
     * @param user The user for which the time is compared
     * @return Returns true if the currentTime is bellow the time of the session validity
     */
    public boolean sessionStillValid(User user) throws InvalidSessionException {
        if (user.getSessionValidUntil() == null) {
            throw new InvalidSessionException("The session for the user is invalid.");
        }
        return new Date().getTime() < user.getSessionValidUntil();
    }


    /**
     * Removes the session id of the user if his session is no longer valid
     *
     * @param user The user that is to be logged out
     * @throws InvalidSessionException Is throws if the session is no longer valid
     */
    public void logoutUserOnInvalidSession(User user) throws InvalidSessionException {
        if (!sessionStillValid(user)) {
            userEntityService.removeSessionId(user);
            throw new InvalidSessionException("The session for the user is invalid.");
        }
    }

    /**
     * Logs in the user.
     *
     * @param username  The username used ot identify the user.
     * @param password  The password for the specified user.
     * @param session The HttpSession.
     * @throws UserLockedException     In case the user is locked.
     * @throws AuthenticationException In case the specified password or username is incorrect.
     */
    public void login(String username, String password, HttpSession session) throws AuthenticationException, UserLockedException {
        User user = null;
        try {
            user = retrieveUser(username);
            checkPassword(password, user);
        } catch (UserNotFoundException | InvalidPasswordException e) {
            throw new AuthenticationException("Username or password not correct");
        }
        ensureUserNotLocked(user);
        resetLock(user);
        resetFailedLoginCounter(user);
        userEntityService.setSessionId(user, (UUID) session.getAttribute(sessionIdName));
        userEntityService.setSessionValidUntil(user);
    }

    /**
     * Resets the failed login counter.
     *
     * @param user The user for which to reset the counter.
     */
    private void resetFailedLoginCounter(User user) {
        if (user.getFailedLoginCounter() != 0) {
            userEntityService.resetFailedCounter(user);
        }
    }

    /**
     * Resets the lockout timer.
     *
     * @param user The user for which to reset the lockout.
     */
    private void resetLock(User user) {
        if (user.getLockedUntil() != null) {
            userEntityService.resetLock(user);
        }
    }

    /**
     * Checks the users' existence.
     *
     * @param username The username of the user to check.
     * @return Returns the User.
     * @throws UserNotFoundException In case the user is not found in the database.
     */
    private User retrieveUser(String username) throws UserNotFoundException {
        User user = userEntityService.getUserByUsername(username);
        if (user == null) {
            throw new UserNotFoundException("The user name does not exist!");
        }
        return user;
    }

    /**
     * Validates the password specified by the user.
     *
     * @param password The password specified by the user.
     * @param user     The user for which to validate the password for.
     * @throws InvalidPasswordException In case the password is invalid.
     */
    private void checkPassword(String password, User user) throws InvalidPasswordException {
        if (!comparePassword(user, password)) {
            user = userEntityService.incrementFailedLoginCount(user);
            if (user.getFailedLoginCounter() >= 4) {
                userEntityService.setLockTime(user);
            }
            throw new InvalidPasswordException("The password is not correct!");
        }
    }

    /**
     * Checks the locked status.
     *
     * @param user The user for which to check the locked status.
     * @throws UserLockedException In case the user is locked.
     */
    private void ensureUserNotLocked(User user) throws UserLockedException {
        if (user.getLockedUntil() != null && user.getLockedUntil() > new Date().getTime()) {
            throw new UserLockedException("The user is locked, login will be possible at" + new Date(user.getLockedUntil()));
        }
    }

    /**
     * Ends the users' session.
     *
     * @param username The username of the users for which to end the session.
     * @param session  The HttpSession.
     * @throws InvalidSessionException In case the session does not match the users session.
     * @throws UserNotFoundException   In case the user does not exist.
     */
    public boolean logout(String username, HttpSession session) throws InvalidSessionException, UserNotFoundException {
        UUID sessionId = extractSessionId(session);
        User user = retrieveUser(username);
        logoutUserOnInvalidSession(user);
        if (!user.getSession()
                .equals(sessionId)) {
            throw new InvalidSessionException("The session for the user is invalid.");
        } else {
            userEntityService.removeSessionId(user);
        }
        return true;
    }

    /**
     * Creates the user in the database.
     *
     * @param inboundUserRegistrationDto Specifies information required for the user creation.
     * @return Returns an outbound representation of the user.
     * @throws UserAlreadyExistsException In case the user already exists.
     */

    public OutboundUserRegistrationDto createUser(InboundUserRegistrationDto inboundUserRegistrationDto) throws UserAlreadyExistsException {
        return userEntityService.addUser(inboundUserRegistrationDto);
    }

    /**
     * Changes the password of the user in the database.
     *
     * @param username                     The username of the users for which to end the session.
     * @param inboundUserChangePasswordDto Specifies information required for the password change.
     * @param session                      The HttpSession.
     * @throws InvalidSessionException  In case there is no active session.
     * @throws InvalidSessionException  In case the session does not match the users' session.
     * @throws UserNotFoundException    In case the user does not exist.
     * @throws InvalidPasswordException In case the specified password is incorrect.
     * @throws InvalidPasswordException In case the new password does not match the control new password
     */
    public boolean changePassword(String username, InboundUserChangePasswordDto inboundUserChangePasswordDto, HttpSession session) throws UserNotFoundException, InvalidSessionException, InvalidPasswordException {

        UUID sessionId = extractSessionId(session);
        if (sessionId == null) {
            throw new InvalidSessionException("There is no valid session active");
        }
        User user = retrieveUser(username);
        logoutUserOnInvalidSession(user);
        userEntityService.setSessionValidUntil(user);
        checkPassword(inboundUserChangePasswordDto.getOldPassword(), user);
        if (!user.getSession()
                .equals(sessionId)) {
            throw new InvalidSessionException("The session for the user is invalid.");
        } else if (!inboundUserChangePasswordDto.getNewPassword()
                .equals(inboundUserChangePasswordDto.getControlNewPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        } else {
            userEntityService.setPassword(user, inboundUserChangePasswordDto.getNewPassword());
            return true;
        }
    }


    public void removeUserByUsername(String username, String password, HttpSession session) throws UserNotFoundException, InvalidSessionException, InvalidPasswordException {

        UUID sessionId = extractSessionId(session);
        if (sessionId == null) {
            throw new InvalidSessionException("There is no valid session active");
        }
        User user = retrieveUser(username);
        logoutUserOnInvalidSession(user);
        if (ObjectUtils.isEmpty(user.getSession()) || !sessionId.equals(user.getSession()))
            throw new InvalidSessionException("You are not authorized to delete the account.");
        if (ObjectUtils.isEmpty(password) || !comparePassword(user, password))
            throw new InvalidPasswordException("Passwords do not match");
        userEntityService.removeUser(user);
    }

    private UUID extractSessionId(HttpSession session) throws InvalidSessionException {
        UUID sessionId;
        try {
            sessionId = (UUID) session.getAttribute(sessionIdName);
        } catch (Exception e) {
            throw new InvalidSessionException("There is no valid session active");
        }
        return sessionId;
    }


}
