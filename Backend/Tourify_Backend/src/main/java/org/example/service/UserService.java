package org.example.service;

import org.example.bean.dto.UserUpdateDTO;
import org.example.bean.model.AttractionDO;
import org.example.bean.model.AttractionStatusDO;
import org.example.bean.model.BadgeDO;
import org.example.bean.model.UserDO;
import org.example.bean.util.SystemRoleEnum;
import org.example.config.BusinessException;
import org.example.bean.util.ResponseCode;
import org.example.config.Result;
import org.example.repository.AttractionRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

/**
 * Description of the class.
 *
 * @author lvyongjie
 * @created 27/06/2023
 */

@Service
public class UserService {

    @Autowired UserRepository userRepository;
    @Autowired AttractionRepository attractionRepository;


    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String CLIENT_ID;

    // This method will validate the token.
    // success - return a default user object created from the token
    // Failure - return null
    public UserDO validateToken(String idTokenString) throws Exception {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                // Specify the CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(CLIENT_ID))
                // Or, if multiple clients access the backend:
                //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                .build();
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            Payload payload = idToken.getPayload();
            // Print user identifier
            String userId = payload.getSubject();
            // Get profile information from payload
            String email = payload.getEmail();
            boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            // store the information retrieved from idToken to UserDO class and return
            UserDO userDO = new UserDO();
            userDO.setUser_id(userId);
            userDO.setUser_email(email);
            userDO.setUser_icon(pictureUrl);
            userDO.setUser_name(name);
            userDO.setEmailVerified(emailVerified);
            userDO.setNftLink("");
            userDO.setSystemRoleEnum(SystemRoleEnum.USER);
            // set the default data for the new user
            AttractionStatusDO attractionStatusDO = new AttractionStatusDO();
            userDO.setAttractionStatusDO(attractionStatusDO);
            BadgeDO badgeDO = new BadgeDO();
            userDO.setBadgeDO(badgeDO);
            return userDO;
        }
        else {
            return null;
        }


    }


    public boolean insertUser(UserDO userDO) throws Exception {
        try {
            userRepository.saveUser(userDO);
        }catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean saveUser(String idTokenString) throws Exception {
        UserDO userDO = validateToken(idTokenString);
        if (userDO == null){
            throw new BusinessException(ResponseCode.PARAM_USER_IDTOKEN_NOT_VAILD);
        }
        // see if the user already exist in the DB
        UserDO userDO2 = userRepository.findUserById(userDO.getUser_id());
        if (userDO2 != null){
            // user is already in the DB
            throw new BusinessException(ResponseCode.PARAM_USER_ALREADY_EXIST);
        }
        // proceed to store the user in the DB
        try {
            userRepository.saveUser(userDO);
        }catch (Exception e) {
            return false;
        }
        return true;
    }

    public UserDO findUserByName(String userName) throws Exception {
        UserDO userDO = userRepository.findUserByName(userName);
        if (userDO == null){
            throw new BusinessException(ResponseCode.PARAM_USER_NOT_EXIST);
        }
        else {
            return userDO;
        }
    }

    public UserDO findUserById(String userId) throws Exception {
        UserDO userDO = userRepository.findUserById(userId);
        if (userDO == null){
            throw new BusinessException(ResponseCode.PARAM_USER_NOT_EXIST);
        }
        else {
            return userDO;
        }
    }

    // To calculate the distance between two locations using the Haversine formula
    private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM
    public static double calculateDistanceInMeter(double startLat, double startLong, double endLat, double endLong) {

        double dLat  = Math.toRadians((endLat - startLat));
        double dLong = Math.toRadians((endLong - startLong));

        startLat = Math.toRadians(startLat);
        endLat   = Math.toRadians(endLat);

        double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c * 1000; // <-- distance in meters
    }
    public static double haversin(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }


    public Result updateUser(UserUpdateDTO userUpdateDTO) throws Exception {
        // Process the token, authenticate user etc
        UserDO userDO = validateToken(userUpdateDTO.getId_token());
        if (userDO == null){
            throw new BusinessException(ResponseCode.PARAM_USER_IDTOKEN_NOT_VAILD);
        }
        // get the user info in DB
        UserDO userDOdb = findUserById(userDO.getUser_id());
        if (userDOdb == null){
            throw new BusinessException(ResponseCode.PARAM_USER_NOT_EXIST);
        }
        // get the attraction info inDB
        AttractionDO attractionDOdb = attractionRepository.findAttractionById(userUpdateDTO.getAttraction_id());
        if (attractionDOdb == null) {
            throw new BusinessException(ResponseCode.PARAM_ATTRACTION_EMPTY);
        }
        // Check whether the user's current address is close to the attraction
        double distance = calculateDistanceInMeter(userUpdateDTO.getLat(), userUpdateDTO.getLng(), Double.parseDouble(attractionDOdb.getCoordinates_lat()), Double.parseDouble(attractionDOdb.getCoordinates_lng()));
        // Check if distance is within 50 meters
//        System.out.println( distance + " Meters");
        if (distance > 50 ){
            throw new BusinessException(ResponseCode.PARAM_DISTANCE_TOO_LONG);
        }
        else {
            // update the user's attraction record
            Boolean resultBoolean = userRepository.updateUserAttractionStatus(userDO.getUser_id(),attractionDOdb.getName_alias());
            if (resultBoolean == false){
                throw new BusinessException(ResponseCode.PARAM_DISTANCE_TOO_LONG);
            }
            else{
                // If the resultBoolean is true that means The database have already changed the user's attraction's record
                // Needs to check and potentially update the badge record.

                // get the latest user info in DB to check the badge
                UserDO userDONewBadge = findUserById(userDO.getUser_id());
                BadgeDO badgeDO = userDONewBadge.getBadgeDO();
                // set the badge's create time to be the current time in NY
                ZoneId newYorkZoneId = ZoneId.of("America/New_York");   // Set the ZoneId to New York
                LocalDateTime localDateTimeInNewYork = LocalDateTime.now(newYorkZoneId);   // Get the current date and time in New York time zone
                // Check the All_Attraction_Badge
                if(userDONewBadge.getAttractionStatusDO().areAllFieldsTrue()){
                    badgeDO.setAll_Attraction_Badge(true);
                    badgeDO.setAll_Attraction_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getAll_Attraction_Badge_CreateTime()));
                }
                // Check the All_Museum_Badge
                if(userDONewBadge.getAttractionStatusDO().areAllMuseumTrue()){
                    badgeDO.setAll_Museum_Badge(true);
                    badgeDO.setAll_Museum_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getAll_Museum_Badge_CreateTime()));
                }
                // Check the All_Park_Badge
                if(userDONewBadge.getAttractionStatusDO().areAllParkTrue()){
                    badgeDO.setAll_Park_Badge(true);
                    badgeDO.setAll_Park_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getAll_Park_Badge_CreateTime()));
                }
                // Check the All_Dining_Badge
                if(userDONewBadge.getAttractionStatusDO().areAllDiningTrue()){
                    badgeDO.setAll_Dining_Badge(true);
                    badgeDO.setAll_Dining_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getAll_Dining_Badge_CreateTime()));
                }
                // Check every attraction's badge

                if(userDONewBadge.getAttractionStatusDO().getStatue_of_Liberty()){
                    badgeDO.setStatue_of_Liberty_Badge(true);
                    badgeDO.setStatue_of_Liberty_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getStatue_of_Liberty_Badge_CreateTime()));
                }

                if(userDONewBadge.getAttractionStatusDO().getEmpire_State_Building()){
                    badgeDO.setEmpire_State_Badge(true);
                    badgeDO.setEmpire_State_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getEmpire_State_Badge_CreateTime()));
                }

                if(userDONewBadge.getAttractionStatusDO().getBrooklyn_Bridge()){
                    badgeDO.setBrooklyn_Bridge_Badge(true);
                    badgeDO.setBrooklyn_Bridge_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getBrooklyn_Bridge_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getMetropolitan_Museum_of_Art()){
                    badgeDO.setMetropolitan_Museum_of_Art_Badge(true);
                    badgeDO.setMetropolitan_Museum_of_Art_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getMetropolitan_Museum_of_Art_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getMuseum_of_Modern_Art()){
                    badgeDO.setMuseum_of_Modern_Art_Badge(true);
                    badgeDO.setMuseum_of_Modern_Art_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getMuseum_of_Modern_Art_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getGuggenheim_Museum()){
                    badgeDO.setGuggenheim_Museum_Badge(true);
                    badgeDO.setGuggenheim_Museum_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getGuggenheim_Museum_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getCentral_Park()){
                    badgeDO.setCentral_Park_Badge(true);
                    badgeDO.setCentral_Park_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getCentral_Park_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getBryant_Park()){
                    badgeDO.setBryant_Park_Badge(true);
                    badgeDO.setBryant_Park_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getBryant_Park_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getHigh_Line()){
                    badgeDO.setHigh_Line_Badge(true);
                    badgeDO.setHigh_Line_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getHigh_Line_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getBroadway()){
                    badgeDO.setBroadway_Badge(true);
                    badgeDO.setBroadway_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getBroadway_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getMadame_Tussauds_New_York()){
                    badgeDO.setMadame_Tussauds_New_York_Badge(true);
                    badgeDO.setMadame_Tussauds_New_York_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getMadame_Tussauds_New_York_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getLincoln_Center()){
                    badgeDO.setLincoln_Center_Badge(true);
                    badgeDO.setLincoln_Center_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getLincoln_Center_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getGreenwich_Village()){
                    badgeDO.setGreenwich_Village_Badge(true);
                    badgeDO.setGreenwich_Village_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getGreenwich_Village_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getHarlem()){
                    badgeDO.setHarlem_Badge(true);
                    badgeDO.setHarlem_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getHarlem_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getEataly()){
                    badgeDO.setEataly_Badge(true);
                    badgeDO.setEataly_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getEataly_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getGrand_Central_Market()){
                    badgeDO.setGrand_Central_Market_Badge(true);
                    badgeDO.setGrand_Central_Market_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getGrand_Central_Market_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getWhitney_Museum()){
                    badgeDO.setWhitney_Museum_Badge(true);
                    badgeDO.setWhitney_Museum_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getWhitney_Museum_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getMuseum_of_Arts_and_Design()){
                    badgeDO.setMuseum_of_Arts_and_Design_Badge(true);
                    badgeDO.setMuseum_of_Arts_and_Design_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getMuseum_of_Modern_Art_Badge_CreateTime()
                    ));
                }
                if(userDONewBadge.getAttractionStatusDO().getNew_Museum()){
                    badgeDO.setNew_Museum_Badge(true);
                    badgeDO.setNew_Museum_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getNew_Museum_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getMorgan_Library_Museum()){
                    badgeDO.setMorgan_Library_Museum_Badge(true);
                    badgeDO.setMorgan_Library_Museum_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getMorgan_Library_Museum_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getTrinity_Church()){
                    badgeDO.setTrinity_Church_Badge(true);
                    badgeDO.setTrinity_Church_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getTrinity_Church_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getFraunces_Tavern()){
                    badgeDO.setFraunces_Tavern_Badge(true);
                    badgeDO.setFraunces_Tavern_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getFraunces_Tavern_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getOne_World_Observatory()){
                    badgeDO.setOne_World_Observatory_Badge(true);
                    badgeDO.setOne_World_Observatory_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getOne_World_Observatory_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getTop_of_the_Rock()){
                    badgeDO.setTop_of_the_Rock_Badge(true);
                    badgeDO.setTop_of_the_Rock_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getTop_of_the_Rock_Badge_CreateTime()));
                }
                if(userDONewBadge.getAttractionStatusDO().getEdge_Observation_Deck()){
                    badgeDO.setEdge_Observation_Deck_Badge(true);
                    badgeDO.setEdge_Observation_Deck_Badge_CreateTime(checkWhetherUpdateTime(badgeDO.getEdge_Observation_Deck_Badge_CreateTime()));
                }

                // update the user badge in DB
                Boolean resultBooleanBadge = userRepository.updateUserBadge(userDO.getUser_id() , badgeDO);
                if (!resultBooleanBadge){
                    throw new BusinessException(ResponseCode.PARAM_UPDATE_BADGE_ERROR);
                }
                // get the latest user info in DB to return it
                UserDO userDONew = findUserById(userDO.getUser_id());
                return Result.success(userDONew);
            }
        }

    }

    // Check whether the time is 3333-01-01. if it is update, if not don't. just return the previous one.
    LocalDateTime checkWhetherUpdateTime(LocalDateTime localDateTime) {
        LocalDateTime dateTime = LocalDateTime.of(3333, 1, 1, 1, 0);
//        System.out.println("------------------------------");
//        System.out.println("localDateTime: "+localDateTime);
//        System.out.println("dateTime: "+dateTime);
        if (localDateTime.equals(dateTime)){
//            System.out.println("1111111");
            ZoneId newYorkZoneId = ZoneId.of("America/New_York");   // Set the ZoneId to New York
            return LocalDateTime.now(newYorkZoneId);   // Get the current date and time in New York time zone

        }
        else{
//            System.out.println("22222222");
            return localDateTime;
        }

     }

    public Boolean DeleteUser(String idTokenString) throws Exception {
        UserDO userDO = validateToken(idTokenString);
        if (userDO == null){
            throw new BusinessException(ResponseCode.PARAM_USER_IDTOKEN_NOT_VAILD);
        }
        // see whether the user exist in the DB
        UserDO userDO2 = userRepository.findUserById(userDO.getUser_id());
        if (userDO2 == null){
            // user is already in the DB
            throw new BusinessException(ResponseCode.PARAM_USER_NOT_EXIST);
        }

        // proceed to delete the user in the DB
        try {
            userRepository.deleteUser(userDO.getUser_id());
        }catch (Exception e) {
            return false;
        }
        return true;

    }

    public UserDO updateNft(String nftLink, String idTokenString) throws Exception {

        // Process the token, authenticate user etc
        UserDO userDO = validateToken(idTokenString);
        if (userDO == null){
            throw new BusinessException(ResponseCode.PARAM_USER_IDTOKEN_NOT_VAILD);
        }
        // get the user info in DB
        UserDO userDOdb = findUserById(userDO.getUser_id());
        if (userDOdb == null){
            throw new BusinessException(ResponseCode.PARAM_USER_NOT_EXIST);
        }
        if (nftLink == null) {
            throw new BusinessException(ResponseCode.PARAM_NFTLINK_NOT_EXIST);
        }
        // update the user's nft link
        Boolean resultBoolean = userRepository.updateUserNft(userDO.getUser_id(), nftLink);
        if (resultBoolean == false){
            throw new BusinessException(ResponseCode.INTERNAL_ERROR);
        }
        // get the latest user info in db and return
        UserDO userDONew = findUserById(userDO.getUser_id());
        return userDONew;
    }

}
