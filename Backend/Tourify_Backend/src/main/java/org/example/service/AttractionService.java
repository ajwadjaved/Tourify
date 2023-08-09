package org.example.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.example.bean.dto.AttractionOnePredictionDTO;
import org.example.bean.dto.AttractionPredictionDTO;
import org.example.bean.model.AttractionDO;
import org.example.bean.model.OpenHour;
import org.example.bean.util.ResponseCode;
import org.example.bean.vo.AttractionOnePredictionVO;
import org.example.bean.vo.AttractionPredictionDetailVO;
import org.example.bean.vo.AttractionPredictionVO;
import org.example.config.BusinessException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.temporal.ChronoField;

import org.example.repository.AttractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.core.io.ResourceLoader;


/**
 * Description of the class.
 *
 * @author lvyongjie
 * @created 12/07/2023
 */

@Service
public class AttractionService {

    @Autowired AttractionRepository attractionRepository;
//    @Autowired ResourceLoader resourceLoader;

    @Value("${spring.custom.docker-flask-ip}")
    private String FLASK_IP;


    // convert the json into a map object
//    private Map<String, Map<String, String>> hashMap;
//    public void init() {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            // load the JSON
//            Resource resource = resourceLoader.getResource("classpath:passengers_average_v2.json");
//            InputStream inputStream = resource.getInputStream();
//            List<Map<String, String>> list = mapper.readValue(inputStream, new TypeReference<List<Map<String, String>>>(){});
//            hashMap = new HashMap<>();
//            for (Map<String, String> item : list) {
//                String dayZoneCombined = (String) item.get("day_zone_combined");
//                hashMap.put(dayZoneCombined, item);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    public Map<String, String> getItem(String key) {
//        return hashMap.get(key);
//    }

    Integer getInteger(String name_alias) throws BusinessException {

        // convert the name_alias into an integer
        HashMap<String, Integer> locations = new HashMap<>();

        locations.put("Empire_State_Building", 0);
        locations.put("Statue_of_Liberty", 1);
        locations.put("Brooklyn_Bridge", 2);
        locations.put("Metropolitan_Museum_of_Art", 3);
        locations.put("Museum_of_Modern_Art", 4);
        locations.put("Guggenheim_Museum", 5);
        locations.put("Central_Park", 6);
        locations.put("Bryant_Park", 7);
        locations.put("High_Line", 8);
        locations.put("Broadway", 9);
        locations.put("Madame_Tussauds_New_York", 10);
        locations.put("Lincoln_Center", 11);
        locations.put("Greenwich_Village", 12);
        locations.put("Harlem", 13);
        locations.put("Eataly", 14);
        locations.put("Grand_Central_Market", 15);
        locations.put("Whitney_Museum", 16);
        locations.put("Museum_of_Arts_and_Design", 17);
        locations.put("New_Museum", 18);
        locations.put("Morgan_Library_Museum", 19);
        locations.put("Trinity_Church", 20);
        locations.put("Fraunces_Tavern", 21);
        locations.put("One_World_Observatory", 22);
        locations.put("Top_of_the_Rock", 23);
        locations.put("Edge_Observation_Deck", 24);

        // Retrieve a value using a key
        String key = name_alias;
        if (locations.containsKey(key)) {
            int value = locations.get(key);
//            System.out.println("The value associated with key \"" + key + "\" is: " + value);
            return value;
        } else {
//            System.out.println("No value is associated with key \"" + key + "\"");
        }
        return null;
    }





    // All 25 attraction's prediction based on current time
    public List<AttractionPredictionVO> getPrediction(AttractionPredictionDTO attractionPredictionDTO) throws BusinessException{
        // get all attractions
        List<AttractionDO> attractionDOList = attractionRepository.getAllAttraction();
        if (attractionDOList.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ATTRACTION_EMPTY_ERROR);
        }

        // Get the current time in NY (PDT timezone)
        ZoneId losAngelesZoneId = ZoneId.of("America/New_York");
        ZonedDateTime nowInLosAngeles = ZonedDateTime.now(losAngelesZoneId);

        // set the NY current time in integer
        int month = nowInLosAngeles.getMonthValue(); // month, from 1 (January) to 12 (December)
        int dayOfWeek = nowInLosAngeles.get(ChronoField.DAY_OF_WEEK); // day of week, from 1 (Monday) to 7 (Sunday)
        int hour = nowInLosAngeles.getHour(); // hour of day, from 0 to 23

        // set the return list
        List<AttractionPredictionVO> attractionPredictionVOList = new LinkedList<>();

        // interate all attractions
        for (AttractionDO attractionDO: attractionDOList) {
            AttractionPredictionVO attractionPredictionVO = new AttractionPredictionVO();
            attractionPredictionVO.setAttraction_id(attractionDO.getId());
            attractionPredictionVO.setName(attractionDO.getName());
            attractionPredictionVO.setName_alias(attractionDO.getName_alias());

            // Discarded
            // get the taxi-zone and passenger parameter
//            PredictionInternalResult predictionInternalResult = getAveragePassengersNum(month, dayOfWeek, hour, attractionDO.getName_alias());

            // invoke the prediction method
            attractionPredictionVO.setBusinessRate(getModelPythonPrediction(attractionPredictionDTO, month, dayOfWeek, hour, attractionDO.getName_alias()));
            attractionPredictionVOList.add(attractionPredictionVO);
//            System.out.println("----------------------------------Finished one attraction's prediction----------------------------------");
        }
        return attractionPredictionVOList;
    }


    // invoke the model and get prediction
    Float getModelPrediction(AttractionPredictionDTO attractionPredictionDTO, int month, int dayOfWeek, int hour, int taxiLocation, int passengersNum) throws BusinessException{
//        System.out.println("3.Starting to make prediction -------------");
        float[][] output = new float[0][];
        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            // load the model
            String currentWorkingDir = System.getProperty("user.dir");
            String modelPath = currentWorkingDir + "/Tourify_Backend/src/main/resources/Tourify_model_v2.onnx";
            try (OrtSession session = env.createSession(modelPath, options)) {
                // create the input
                float[][] longArray = new float[1][7];
                longArray[0][0] = month;
                longArray[0][1] = dayOfWeek;
                longArray[0][2] = hour;
                longArray[0][3] = taxiLocation;
                longArray[0][4] = passengersNum;
                longArray[0][5] = attractionPredictionDTO.getTemperature();
                longArray[0][6] = attractionPredictionDTO.getPrecipitation();
                OnnxTensor tensor = OnnxTensor.createTensor(env, longArray);
                // Create a map to hold the input tensor
                HashMap<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("integer_input", tensor); // Replace "input_name" with the actual name of your model's input
                // run the model
                try (OrtSession.Result results = session.run(inputs)) {
                    // get the output
                    output = (float[][]) ((OnnxTensor) results.get(0)).getValue();
//                    System.out.println("3.Predicted result: " + output[0][0]);  // modify this to suit your output shape
                    return output[0][0];
                }
            }
        } catch (OrtException e) {
            e.printStackTrace();
        }
        throw new BusinessException(ResponseCode.PARAM_PREDICTION_ERROR);
    }



    // The one attraction's next 24 hours prediction
    public AttractionOnePredictionVO getOnePrediction(AttractionOnePredictionDTO attractionOnePredictionDTO) throws BusinessException{
        // check the DTO's input number
        long predictionDays = attractionOnePredictionDTO.getPredictionDays();
        if(predictionDays<=0 || predictionDays>=8){
            throw new BusinessException(ResponseCode.PARAM_ATTRACTION_EMPTY_ERROR);
        }
        // get the attraction from the attraction_id
        AttractionDO attractionDO = attractionRepository.findAttractionById(attractionOnePredictionDTO.getAttraction_id());
        if (attractionDO.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ATTRACTION_EMPTY_ERROR);
        }
        AttractionOnePredictionVO attractionOnePredictionVO = new AttractionOnePredictionVO();
        attractionOnePredictionVO.setAttraction_id(attractionOnePredictionDTO.getAttraction_id());
        attractionOnePredictionVO.setAttraction_name(attractionDO.getName());
        attractionOnePredictionVO.setName_alias(attractionDO.getName_alias());
        attractionOnePredictionVO.setPredictionDays(attractionOnePredictionDTO.getPredictionDays());
        // store the machine prediction into the attractionPredictionDetailVOList
        List<AttractionPredictionDetailVO> attractionPredictionDetailVOList = new LinkedList<>();

        // Set the ZoneId to New York
        ZoneId newYorkZoneId = ZoneId.of("America/New_York");
        // Get the current date and time in New York time zone
        LocalDateTime localDateTimeInNewYork = LocalDateTime.now(newYorkZoneId);
//        System.out.println("localDateTimeInNewYork: " + localDateTimeInNewYork);
        predictionDays--;
        localDateTimeInNewYork = localDateTimeInNewYork.plusDays(predictionDays);
//        System.out.println("--------------------------------------Starting a new prediction----------------------------------");
//        System.out.println("localDateTimeInNewYork: " + localDateTimeInNewYork);



        // Prepare lists to hold the month, day of the week, and hour values
        List<Integer> months = new ArrayList<>();
        List<Integer> daysOfWeek = new ArrayList<>();
        List<Integer> hours = new ArrayList<>();
        List<Boolean> OpenClose = new ArrayList<>();
        // Generate the values for the next 24 hours
        for (int i = 0; i < 24; i++) {
            LocalDateTime futureDateTime = localDateTimeInNewYork.plusHours(i);
            months.add(futureDateTime.getMonthValue());
            daysOfWeek.add(futureDateTime.getDayOfWeek().getValue());
            hours.add(futureDateTime.getHour());
            // Check at the time is the attraction open or close
            OpenHour openHour = attractionDO.getOpenHour();
            LocalTime attractionOpen = null;
            LocalTime attractionClose = null;
            if (futureDateTime.getDayOfWeek().getValue() == 1){attractionOpen = openHour.getMondayOpen();attractionClose = openHour.getMondayClose();}
            if (futureDateTime.getDayOfWeek().getValue() == 2){attractionOpen = openHour.getTuesdayOpen();attractionClose = openHour.getTuesdayClose();}
            if (futureDateTime.getDayOfWeek().getValue() == 3){attractionOpen = openHour.getWednesdayOpen();attractionClose = openHour.getWednesdayClose();}
            if (futureDateTime.getDayOfWeek().getValue() == 4){attractionOpen = openHour.getThursdayOpen();attractionClose = openHour.getThursdayClose();}
            if (futureDateTime.getDayOfWeek().getValue() == 5){attractionOpen = openHour.getFridayOpen();attractionClose = openHour.getFridayClose();}
            if (futureDateTime.getDayOfWeek().getValue() == 6){attractionOpen = openHour.getSaturdayOpen();attractionClose = openHour.getSaturdayClose();}
            if (futureDateTime.getDayOfWeek().getValue() == 7){attractionOpen = openHour.getSundayOpen();attractionClose = openHour.getSundayClose();}
            // compare the attraction's time range with the iterate time
            LocalTime futureLocalTime = futureDateTime.toLocalTime();        // Extract the time part from the current date-time
            int hour = futureLocalTime.getHour();   // Only need the hour part
            LocalTime futureTimeRounded = LocalTime.of(hour,0,1);

            if (attractionOpen!=null && attractionClose!=null) {  // The database is storing empty if the attraction is not open that day
                // Check Whether the time is inside the period
                if (futureTimeRounded.isAfter(attractionOpen) && futureTimeRounded.isBefore(attractionClose)) {
                    OpenClose.add(true);
//                    System.out.println("111111111");
                } else {
                    OpenClose.add(false);
//                    System.out.println("222222222");
                }
            }
            else {  // null means the attraction is not open on that day, skip the conditional statement and directly add false
//                System.out.println("33333333");
//                System.out.println(attractionOpen);
//                System.out.println(attractionClose);
                OpenClose.add(false);
            }
            // Debugging print statement
//            System.out.println("i: " + i + "   futureDateTime:" + futureDateTime + "   hours:" + hours.get(i) +  "   OpenClose:" + OpenClose.get(i));
        }
//        System.out.println("Months: " + months);
//        System.out.println("Days of Week: " + daysOfWeek);
//        System.out.println("Hours: " + hours);
//        System.out.println("OpenClose: " + OpenClose);
        // Use monthsList to loop though to insert the attractionPredictionDetailVO(hour and business) into List (attractionPredictionDetailVOList)
        for (int i = 0; i < 24; i++) {
            // set the response VO
            AttractionPredictionDetailVO attractionPredictionDetailVO = new AttractionPredictionDetailVO();
            attractionPredictionDetailVO.setHour(hours.get(i));
            attractionPredictionDetailVO.setOpenOrClose(OpenClose.get(i));
            // get the 24 weather info from the DTO
            AttractionPredictionDTO attractionPredictionDTO = new AttractionPredictionDTO();
            // exception just in case that front end input does not contain 24 items
            try {
                attractionPredictionDTO.setPrecipitation(attractionOnePredictionDTO.getPrecipitation().get(i));
                attractionPredictionDTO.setPrecipitation(attractionOnePredictionDTO.getTemperatures().get(i));
            }
            catch (Exception e){
                throw new BusinessException(ResponseCode.PARAM_PREDICTION_WEATHER_ERROR);
            }

            // get the taxi-zone and passenger parameter
            // Discarded because new model doesn't require get the average passager form json file
//            PredictionInternalResult predictionInternalResult = getAveragePassengersNum(months.get(i), daysOfWeek.get(i), hours.get(i), attractionOnePredictionVO.getName_alias());
//            System.out.println("2.Preparing to predict. taxiLocation:"+predictionInternalResult.getTaxiLocation()+" passengersNum:"+ predictionInternalResult.getPassengersNum());

            // Get the prediction for one hour
//            System.out.println("2.Preparing to predict. months:"+months.get(i)+" daysOfWeek:"+ daysOfWeek.get(i)+" hour:" + hours.get(i));
            attractionPredictionDetailVO.setBusinessRate(getModelPythonPrediction(attractionPredictionDTO, months.get(i), daysOfWeek.get(i), hours.get(i),attractionDO.getName_alias()));
            attractionPredictionDetailVOList.add(attractionPredictionDetailVO);
//            System.out.println("--------Finished " + i +  " hour of prediction");
        }
        attractionOnePredictionVO.setAttractionPredictionDetailVOList(attractionPredictionDetailVOList);
        return attractionOnePredictionVO;
    }



    // get the taxi zone and the average passengers function
//    PredictionInternalResult getAveragePassengersNum(int month, int dayOfWeek, int hour, String attraction_alias) throws BusinessException{
//        // set the map of list of attractions mapped to the taxi zones
//        HashMap<String, String> map = new HashMap<>();
//        map.put("Empire_State_Building", "164");
//        map.put("Statue_of_Liberty", "103");
//        map.put("Brooklyn_Bridge", "045");
//        map.put("Metropolitan_Museum_of_Art", "043");
//        map.put("Museum_of_Modern_Art", "161");
//        map.put("Guggenheim_Museum", "236");
//        map.put("Central_Park", "043");
//        map.put("Bryant_Park", "164");
//        map.put("High_Line", "246");
//        map.put("Broadway", "230");
//        map.put("Madame_Tussauds_New_York", "100");
//        map.put("Lincoln_Center", "142");
//        map.put("Greenwich_Village", "249");
//        map.put("Harlem", "042");
//        map.put("Eataly", "234");
//        map.put("Grand_Central_Market", "162");
//        map.put("Whitney_Museum", "158");
//        map.put("Museum_of_Arts_and_Design", "163");
//        map.put("New_Museum", "148");
//        map.put("Morgan_Library_Museum", "170");
//        map.put("Trinity_Church", "261");
//        map.put("Fraunces_Tavern", "088");
//        map.put("One_World_Observatory", "261");
//        map.put("Top_of_the_Rock", "161");
//        map.put("Edge_Observation_Deck", "246");
//
//        // get the taxiLocation
//        String taxiLocation = map.get(attraction_alias);          // set the taxi location
//        int taxiLocationInt = Integer.parseInt(taxiLocation);        // convert string to integer
//        PredictionInternalResult predictionInternalResult = new PredictionInternalResult();
//        predictionInternalResult.setTaxiLocation(taxiLocationInt);
//        System.out.println("1.GetAveragePassengers function - returned taxiLocation:" + taxiLocationInt);
//
//        // get the average passengers from reading the passages average json file (construct the search id: month - day of week - hour - taxi location id)
//        String passengersId = "";
//        if ( month>=10 && month<=12){
//            passengersId = String.valueOf(month);
//        }
//        else {
//            passengersId = passengersId + "0"+ String.valueOf(month);
//        }
//        passengersId = passengersId + "-" + dayOfWeek + "-";
//        if ( hour>=0 && hour <= 9){
//            passengersId = passengersId + "0" + String.valueOf(hour);
//        }
//        else {
//            passengersId = passengersId + String.valueOf(hour);
//        }
//        passengersId = passengersId + "-" + String.valueOf(taxiLocation);
//        System.out.println("1.Constructing passengersId: " +passengersId);
//
//        // search the primary id(passengersId) in the json file
//        init(); // invoke the method to load the json into map object
//        Map<String, String> mapJson= getItem(passengersId);
//        System.out.println("1.Returned map1:"+mapJson);
//        // Trying to get the passenger
//        int passengersNum;
//        try {
//            passengersNum = Integer.parseInt(mapJson.get("passengers_average"));
//        } catch (NullPointerException e) {
//            System.out.println("1.1.Hit the empty passenger");
//            passengersNum = 0;
//        }
//        System.out.println("1.Returned passengersNum:"+passengersNum);
//        predictionInternalResult.setPassengersNum(passengersNum);
//        return predictionInternalResult;
//    }



    // invoke the model api from python
    Integer getModelPythonPrediction(AttractionPredictionDTO attractionPredictionDTO, int month, int dayOfWeek, int hour, String name_alias) throws BusinessException{
        System.out.println("3.Starting to invoke prediction on python service -------------------------------");
        System.out.println("month: "+month+"dayOfWeek: "+dayOfWeek+"hour: "+hour);

        WebClient webClient = WebClient.create(FLASK_IP);
        int value = getInteger(name_alias);

        Map<String, Object> map = new HashMap<>();
        map.put("month", month);
        map.put("day_of_week", dayOfWeek);
        map.put("hour", hour);
        map.put("name_alias", value);
        map.put("temp_avg", attractionPredictionDTO.getTemperature());
        map.put("precipitation", attractionPredictionDTO.getPrecipitation());

        Mono<Map> responseMono = webClient.post()
                .uri("/predict")
                .bodyValue(map)
                .retrieve()
                .bodyToMono(Map.class);

        // process the response
        Map<String, Integer> response = responseMono.block();
        if(response.containsKey("prediction")) {
            Integer predictionValue = response.get("prediction");
            System.out.println("predictionValue: " + predictionValue);

//            System.out.println("Prediction value: " + predictionValue);
            return predictionValue;
        } else {
//            System.out.println("The key 'prediction' does not exist in the map");
            return 0;
        }
    }

    }


