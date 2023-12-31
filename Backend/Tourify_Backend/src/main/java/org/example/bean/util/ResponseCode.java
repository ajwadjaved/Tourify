package org.example.bean.util;

import lombok.Getter;

/**
 * Description of the class.
 *
 * @author lvyongjie
 * @created 15/06/2023
 */


@Getter
public enum ResponseCode {
    SUCCESS(200, "response.message.success"),
    INTERNAL_ERROR(500, "response.message.internal.error"),
    PARAM_ATTRACTION_EMPTY(10001, "param.attraction.not.exist"),
    PARAM_USER_IDTOKEN_EMPTY(10002, "param.user.idtoken.not.exist"),
    PARAM_USER_IDTOKEN_NOT_VAILD(10003, "param.user.idtoken.not.valid"),
    PARAM_USER_NOT_EXIST(10004, "param.user.not.exist"),
    PARAM_USER_ID_EMPTY(10005, "param.user.id.empty"),
    PARAM_USER_ALREADY_EXIST(10006, "param.user.already.exist"),
    PARAM_NFTLINK_NOT_EXIST(10007, "param.ntflink.not.exist"),
    PARAM_DISTANCE_TOO_LONG(10050, "param.distance.too.long"),
    PARAM_UPDATE_BADGE_ERROR(10051, "param.update.badge.error"),
    PARAM_ATTRACTION_EMPTY_ERROR(10007, "param.attraction.empty.error"),
    PARAM_PREDICTION_ERROR(10008, "param.prediction.error"),
    PARAM_PREDICTION_WEATHER_ERROR(10008, "param.prediction.weather.error(needs all 24 info)");










    /**
     * status code
     */
    private Integer code;

    /**
     * status message
     */
    private String msg;

    ResponseCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ResponseCode getByName(String name) {
        for (ResponseCode code : values()) {
            if (code.name().equals(name)) {
                return code;
            }
        }
        return null;
    }

}
